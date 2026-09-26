package KondoKit.views

import KondoKit.party.LocalPartyState
import KondoKit.party.PartyClient
import KondoKit.plugin.Companion.TITLE_BAR_COLOR
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.partyServerUrl
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import KondoKit.plugin.StateManager.focusedView
import KondoKit.ui.BaseView
import KondoKit.ui.OnPostClientTickCallback
import KondoKit.ui.View
import KondoKit.ui.ViewConstants
import KondoKit.ui.components.ButtonPanel
import KondoKit.ui.components.SearchField
import KondoKit.util.Helpers
import KondoKit.util.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.util.setFixedSize
import com.google.gson.JsonObject
import plugin.api.API
import rt4.ObjTypeList
import java.awt.*
import java.awt.image.BufferedImage
import javax.swing.*

/**
 * View-only party panel: join a party by password and watch every other
 * member's inventory, gear, stats and prayers. Nothing received here can act
 * on the local client; it is only drawn.
 *
 * Each member gets one [MemberCard], built once and then updated in place:
 * only slots and labels whose value actually changed are repainted, and all
 * drawing goes through lightweight (double-buffered) Swing components, so
 * polling never makes the panel flicker.
 */
object PartyView : View, OnPostClientTickCallback {
    const val VIEW_NAME = "PARTY_VIEW"
    const val ICON_SPRITE = 203 // Hitpoints skill icon

    // Not "WIDTH": inside any Swing component that name resolves to the
    // inherited ImageObserver.WIDTH (= 1) instead, silently.
    private const val CARD_W = 234
    private const val SLOT_W = 36
    private const val SLOT_H = 32
    private const val HEADER_ROW_H = 22
    private const val SPEC_ROW_H = 22
    private const val HEADER_H = HEADER_ROW_H + SPEC_ROW_H
    private const val TABS_H = 38
    private const val TAB_W = 40
    private const val TAB_HIGHLIGHT_SPRITE = 1030 // red backing behind the selected side tab
    private const val STAT_ROW_H = 20
    private const val TEXT_ROW_H = 16
    private const val SPEC_BAR_H = 16
    private const val PRAYER_COLS = 5
    private const val PRAYER_CELL = 37 // icon spacing on the in-game prayer tab
    private const val PRAYER_GLOW_SPRITE = 155 // highlight drawn behind an active prayer

    // Sprites are the game's own side-tab icons (interface 548's tab row).
    private enum class Tab(val label: String, val spriteId: Int) {
        INVENTORY("Inventory", 900), GEAR("Worn Equipment", 901), STATS("Stats", 898), PRAYER("Prayer", 902)
    }

    /** One member's state, with item images already rendered (game thread). */
    private class MemberModel(
        val name: String,
        val inv: List<BufferedImage?>,
        val gear: List<BufferedImage?>,
        val skills: List<IntArray>,      // [base, boosted, xp]
        val prayerPoints: IntArray,      // [current, max]
        val prayers: Int,
        val spec: Int?                   // special attack %, null if never sent
    )

    private var partyView: BaseView? = null
    private lateinit var membersPanel: JPanel
    private lateinit var statusLabel: JLabel
    private lateinit var waitingLabel: JLabel
    private var lastStatusKey = ""

    private val cards = LinkedHashMap<String, MemberCard>() // EDT only
    private val skillIcons = HashMap<Int, BufferedImage>()
    // Same (id, qty) always yields the same image instance, so slots can skip
    // repainting with a plain identity check.
    private val itemImageCache = object : LinkedHashMap<Long, BufferedImage>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, BufferedImage>) = size > 512
    }

    // Prayer tab order; bit i of a member's "prayers" bitmask is PRAYER_NAMES[i]
    // (built from LocalPartyState.PRAYER_VARPS, in the same order).
    private val PRAYER_NAMES = arrayOf(
        "Thick Skin", "Burst of Strength", "Clarity of Thought", "Sharp Eye", "Mystic Will",
        "Rock Skin", "Superhuman Strength", "Improved Reflexes", "Rapid Restore", "Rapid Heal",
        "Protect Item", "Hawk Eye", "Mystic Lore", "Steel Skin", "Ultimate Strength",
        "Incredible Reflexes", "Protect from Summoning", "Protect from Magic",
        "Protect from Missiles", "Protect from Melee", "Eagle Eye", "Mystic Might",
        "Retribution", "Redemption", "Smite", "Chivalry", "Piety"
    )

    // Bright icon sprite per prayer, same order as PRAYER_NAMES; the tab lays
    // them out in this order, PRAYER_COLS per row. Interface 271's components
    // reference the dark "level too low" variants (135-154, 506-509, 948-950);
    // these are their bright twins from the same sprite archive.
    private val PRAYER_SPRITES = intArrayOf(
        115, 116, 117, 133, 134, 118, 119, 120, 121, 122, 123, 502, 503, 124,
        125, 126, 944, 127, 128, 129, 504, 505, 131, 130, 132, 945, 946
    )

    // Loaded lazily: sprites are not ready until the cache has them.
    private var iconsLoaded = false                       // game thread
    private var prayerIcons: List<BufferedImage>? = null  // EDT
    private var prayerGlow: BufferedImage? = null         // EDT
    private var tabIcons: Map<Tab, BufferedImage>? = null // EDT
    private var tabHighlight: BufferedImage? = null       // EDT

    // Equipment tab layout, 3 columns x 5 rows; values are equipment slots.
    private val GEAR_LAYOUT = arrayOf(
        intArrayOf(-1, 0, -1),   // head
        intArrayOf(1, 2, 13),    // cape, amulet, ammo
        intArrayOf(3, 4, 5),     // weapon, body, shield
        intArrayOf(-1, 7, -1),   // legs
        intArrayOf(9, 10, 12)    // hands, feet, ring
    )

    override val name: String = VIEW_NAME
    override val iconSpriteId: Int = ICON_SPRITE
    override val panel: JPanel
        get() = partyView ?: JPanel()

    override fun createView() {
        for (skill in 0 until 24) {
            skillIcons[skill] = opaque(getBufferedImageFromSprite(API.GetSprite(Helpers.getSpriteId(skill))))
        }

        partyView = BaseView(VIEW_NAME).apply {
            val controls = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = VIEW_BACKGROUND_COLOR
                alignmentX = Component.CENTER_ALIGNMENT
            }
            controls.add(SearchField(
                this,
                onSearch = { text -> if (text.isNotBlank()) joinParty(text) },
                placeholderText = "Party password, then Enter",
                viewName = VIEW_NAME
            ))
            controls.add(Box.createVerticalStrut(4))

            statusLabel = textLabel("Not in a party", primaryColor).apply {
                alignmentX = Component.CENTER_ALIGNMENT
            }
            controls.add(statusLabel)
            controls.add(Box.createVerticalStrut(4))

            controls.add(ButtonPanel().apply {
                background = VIEW_BACKGROUND_COLOR
                addButton("Leave party") { leaveParty() }
                setFixedSize(CARD_W, 26)
            })
            add(controls)
            add(Box.createVerticalStrut(6))

            membersPanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = VIEW_BACKGROUND_COLOR
                alignmentX = Component.CENTER_ALIGNMENT
            }
            waitingLabel = textLabel("Waiting for party members...", primaryColor).apply {
                alignmentX = Component.CENTER_ALIGNMENT
                isVisible = false
            }
            membersPanel.add(waitingLabel)
            add(membersPanel)
        }
        relayout()
    }

    override fun registerFunctions() {
        KondoKit.plugin.registerPostClientTickCallback(this)
    }

    private var serverUrlMigrated = false

    /** Move a saved legacy/blank server URL to the current one, persisting it. */
    private fun migrateServerUrl(): String {
        val url = PartyClient.resolveServerUrl(partyServerUrl)
        if (url != partyServerUrl) KondoKit.plugin.partyServerUrl = url
        return url
    }

    private fun joinParty(password: String) {
        PartyClient.serverUrl = migrateServerUrl()
        PartyClient.join(password)
        SwingUtilities.invokeLater { clearCards() }
    }

    private fun leaveParty() {
        PartyClient.leave()
        SwingUtilities.invokeLater { clearCards() }
    }

    // Game thread, once per game tick.
    override fun onPostClientTick() {
        // Once saved settings have been restored (they load after the view
        // is created), fix an old URL so the settings panel shows the new one.
        if (!serverUrlMigrated && API.IsLoggedIn()) {
            migrateServerUrl()
            serverUrlMigrated = true
        }
        LocalPartyState.tick()
        loadIcons()

        val statusKey = "${PartyClient.status}|${PartyClient.statusDetail}"
        val updates = PartyClient.drainUpdates()
        if (updates.isEmpty() && statusKey == lastStatusKey) return
        lastStatusKey = statusKey

        // Item sprites have to be rendered on the game thread, so build the
        // models here and only hand finished images to Swing.
        var present: Set<String>? = null
        val changed = LinkedHashMap<String, MemberModel>()
        for (update in updates) {
            present = update.present
            for (m in update.changed) changed[m.id] = buildModel(m.name, m.state)
        }

        SwingUtilities.invokeLater { applyUpdate(present, changed) }
    }

    // ---- Game thread ----

    private fun buildModel(name: String, state: JsonObject): MemberModel {
        fun rows(key: String): List<IntArray> =
            state.getAsJsonArray(key)?.map { row -> row.asJsonArray.map { it.asInt }.toIntArray() } ?: emptyList()

        fun items(key: String) = rows(key).map { r ->
            val objId = r.getOrElse(0) { -1 }
            if (objId >= 0) itemImage(objId, r.getOrElse(1) { 0 }) else null
        }

        return MemberModel(
            name = name,
            inv = items("inv"),
            gear = items("gear"),
            skills = rows("skills"),
            prayerPoints = rows("prayer").firstOrNull() ?: intArrayOf(0, 0),
            prayers = state.get("prayers")?.asInt ?: 0,
            spec = state.get("spec")?.asInt
        )
    }

    private fun loadIcons() {
        if (iconsLoaded) return
        val prayerSprites = PRAYER_SPRITES.map { API.GetSprite(it) ?: return }
        val glow = API.GetSprite(PRAYER_GLOW_SPRITE) ?: return
        val tabSprites = Tab.values().map { it to (API.GetSprite(it.spriteId) ?: return) }
        val highlight = API.GetSprite(TAB_HIGHLIGHT_SPRITE) ?: return
        iconsLoaded = true
        val prayers = prayerSprites.map { opaque(getBufferedImageFromSprite(it)) }
        val glowImage = opaque(getBufferedImageFromSprite(glow))
        val tabs = tabSprites.associate { (t, sprite) -> t to opaque(getBufferedImageFromSprite(sprite)) }
        val highlightImage = opaque(getBufferedImageFromSprite(highlight))
        SwingUtilities.invokeLater {
            prayerIcons = prayers
            prayerGlow = glowImage
            tabIcons = tabs
            tabHighlight = highlightImage
            cards.values.forEach { it.repaintIcons() }
        }
    }

    private fun itemImage(objId: Int, qty: Int): BufferedImage? {
        if (objId >= ObjTypeList.capacity) return null
        val key = (objId.toLong() shl 32) or (qty.toLong() and 0xFFFFFFFFL)
        return itemImageCache.getOrPut(key) {
            opaque(getBufferedImageFromSprite(API.GetObjSprite(objId, qty, true, 1, 3153952)))
        }
    }

    /** Same pixel fix-up ImageCanvas applies: any non-zero pixel is fully opaque. */
    private fun opaque(image: BufferedImage): BufferedImage {
        for (y in 0 until image.height) for (x in 0 until image.width) {
            val c = image.getRGB(x, y)
            if (c != 0) image.setRGB(x, y, (c and 0x00FFFFFF) or (0xFF shl 24))
        }
        return image
    }

    // ---- EDT only below ----

    private fun applyUpdate(present: Set<String>?, changed: Map<String, MemberModel>) {
        var layoutChanged = false

        if (present != null) {
            val gone = cards.keys.filter { it !in present }
            for (id in gone) {
                membersPanel.remove(cards.remove(id)!!.root)
                layoutChanged = true
            }
        }
        for ((id, model) in changed) {
            val card = cards[id]
            if (card == null) {
                cards[id] = MemberCard(model).also { membersPanel.add(it.root) }
                layoutChanged = true
            } else if (card.update(model)) {
                layoutChanged = true
            }
        }

        setIfChanged(statusLabel, statusText())
        val waiting = PartyClient.inParty && cards.isEmpty()
        if (waitingLabel.isVisible != waiting) {
            waitingLabel.isVisible = waiting
            layoutChanged = true
        }
        if (layoutChanged) relayout()
    }

    private fun clearCards() {
        for (card in cards.values) membersPanel.remove(card.root)
        cards.clear()
        waitingLabel.isVisible = false
        setIfChanged(statusLabel, statusText())
        relayout()
    }

    private fun statusText(): String = when (PartyClient.status) {
        PartyClient.Status.IDLE -> "Not in a party"
        PartyClient.Status.CONNECTING -> "Connecting..."
        PartyClient.Status.CONNECTED -> "In party (${cards.size} other${if (cards.size == 1) "" else "s"})"
        PartyClient.Status.ROOM_FULL -> "Party is full"
        PartyClient.Status.ERROR -> "Connection problem: ${PartyClient.statusDetail}".take(40)
    }

    /** Resize the view after cards were added, removed or changed height. */
    private fun relayout() {
        val view = partyView ?: return
        view.revalidate()
        val height = view.components.sumOf { it.preferredSize.height } + 20
        view.setViewSize(maxOf(height, ViewConstants.DEFAULT_PANEL_SIZE.height))
        view.revalidate()
        if (focusedView == VIEW_NAME) view.repaint()
    }

    private fun setIfChanged(label: JLabel, text: String) {
        if (label.text != text) label.text = text
    }

    private fun setIfChanged(label: JLabel, color: Color) {
        if (label.foreground != color) label.foreground = color
    }

    private fun textLabel(text: String, color: Color) = JLabel(text).apply {
        foreground = color
        font = ViewConstants.FONT_RUNESCAPE_SMALL_16
    }

    /**
     * Lightweight image cell. Unlike ImageCanvas (a heavyweight AWT Canvas
     * that paints straight to screen), this is double-buffered by Swing, and
     * it only repaints when handed a different image.
     */
    private class ImageSlot(w: Int, h: Int, private val outlined: Boolean) : JComponent() {
        var image: BufferedImage? = null
            set(value) {
                if (field !== value) {
                    field = value
                    repaint()
                }
            }

        init {
            isOpaque = true
            setFixedSize(w, h)
        }

        override fun paintComponent(g: Graphics) {
            g.color = WIDGET_COLOR
            g.fillRect(0, 0, width, height)
            if (outlined) {
                g.color = VIEW_BACKGROUND_COLOR
                g.drawRect(0, 0, width - 1, height - 1)
            }
            val img = image ?: return
            if (img.width <= width && img.height <= height) {
                g.drawImage(img, (width - img.width) / 2, (height - img.height) / 2, null)
            } else {
                g.drawImage(img, 0, 0, width, height, null)
            }
        }
    }

    /** One cell of the prayer book: the icon, glowing when active, dimmed when not. */
    private class PrayerSlot(private val index: Int) : JComponent() {
        var active = false
            set(value) {
                if (field != value) {
                    field = value
                    repaint()
                }
            }

        init {
            isOpaque = true
            toolTipText = PRAYER_NAMES[index]
            setFixedSize(PRAYER_CELL, PRAYER_CELL)
        }

        override fun paintComponent(g: Graphics) {
            g.color = WIDGET_COLOR
            g.fillRect(0, 0, width, height)
            val icon = prayerIcons?.getOrNull(index) ?: return
            val glow = prayerGlow
            if (active && glow != null) {
                g.drawImage(glow, (width - glow.width) / 2, (height - glow.height) / 2, null)
            }
            val g2 = g.create() as Graphics2D
            if (!active) g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f)
            g2.drawImage(icon, (width - icon.width) / 2, (height - icon.height) / 2, null)
            g2.dispose()
        }
    }

    /** A side-tab style button: the game's tab icon, on the red backing when selected. */
    private class TabIcon(private val tab: Tab, onClick: () -> Unit) : JComponent() {
        var selected = false
            set(value) {
                if (field != value) {
                    field = value
                    repaint()
                }
            }

        init {
            isOpaque = true
            toolTipText = tab.label
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            setFixedSize(TAB_W, TABS_H)
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mousePressed(e: java.awt.event.MouseEvent) = onClick()
            })
        }

        override fun paintComponent(g: Graphics) {
            g.color = WIDGET_COLOR
            g.fillRect(0, 0, width, height)
            val highlight = tabHighlight
            if (selected && highlight != null) {
                g.drawImage(highlight, (width - highlight.width) / 2, (height - highlight.height) / 2, null)
            }
            val icon = tabIcons?.get(tab)
            if (icon != null) {
                g.drawImage(icon, (width - icon.width) / 2, (height - icon.height) / 2, null)
            } else {
                // Sprites not loaded yet: fall back to the name.
                g.color = if (selected) primaryColor.brighter() else secondaryColor
                g.font = ViewConstants.FONT_RUNESCAPE_SMALL_14
                val fm = g.fontMetrics
                val text = tab.label.substringBefore(' ')
                g.drawString(text, (width - fm.stringWidth(text)) / 2, (height + fm.ascent) / 2 - 2)
            }
        }
    }

    /** The in-game special attack bar: green fill over a dark track, % on top. */
    private class SpecBar : JComponent() {
        var percent: Int? = null
            set(value) {
                if (field != value) {
                    field = value
                    repaint()
                }
            }

        init {
            isOpaque = true
            toolTipText = "Special attack energy"
            setFixedSize(CARD_W - 12, SPEC_BAR_H)
        }

        override fun paintComponent(g: Graphics) {
            val inner = width - 2
            val filled = inner * (percent ?: 0) / 100
            g.color = Color(38, 32, 24) // frame
            g.fillRect(0, 0, width, height)
            g.color = Color(56, 150, 44)
            g.fillRect(1, 1, filled, height - 2)
            g.color = Color(150, 28, 24)
            g.fillRect(1 + filled, 1, inner - filled, height - 2)

            g.font = ViewConstants.FONT_RUNESCAPE_SMALL_16
            val text = "Special Attack: " + (percent?.let { "$it%" } ?: "?")
            val fm = g.fontMetrics
            g.color = Color.BLACK
            g.drawString(text, (width - fm.stringWidth(text)) / 2, (height + fm.ascent) / 2 - 2)
        }
    }

    /** One member's panel. Built once; [update] only touches what changed. */
    private class MemberCard(initial: MemberModel) {
        val root = JPanel()
        private var tab = Tab.INVENTORY

        private val nameLabel = textLabel(" ", secondaryColor)
        private val summaryLabel = textLabel("", primaryColor)
        private val tabButtons = LinkedHashMap<Tab, TabIcon>()

        private val invSlots = List(28) { ImageSlot(SLOT_W, SLOT_H, false) }
        private val gearSlots = HashMap<Int, ImageSlot>()
        private val statLabels = HashMap<Int, JLabel>()
        private val totalLabel = textLabel("", primaryColor)
        private val specBar = SpecBar()
        private val prayerPointsLabel = textLabel("", secondaryColor)
        private val prayerSlots = List(PRAYER_NAMES.size) { PrayerSlot(it) }

        private val tabPanels: Map<Tab, JPanel>
        private var shownPanel: JPanel

        init {
            val nameRow = JPanel(BorderLayout()).apply {
                background = TITLE_BAR_COLOR
                add(nameLabel, BorderLayout.WEST)
                add(summaryLabel, BorderLayout.EAST)
                setFixedSize(CARD_W, HEADER_ROW_H)
            }
            val specRow = JPanel(FlowLayout(FlowLayout.CENTER, 0, (SPEC_ROW_H - SPEC_BAR_H) / 2 - 1)).apply {
                background = TITLE_BAR_COLOR
                add(specBar)
                setFixedSize(CARD_W, SPEC_ROW_H)
            }
            val header = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(nameRow)
                add(specRow)
                setFixedSize(CARD_W, HEADER_H)
            }
            val tabs = JPanel(FlowLayout(FlowLayout.CENTER, 6, 0)).apply {
                background = WIDGET_COLOR
                for (t in Tab.values()) tabButtons[t] = TabIcon(t) { selectTab(t) }.also { add(it) }
                setFixedSize(CARD_W, TABS_H)
            }
            tabPanels = mapOf(
                Tab.INVENTORY to buildInventory(),
                Tab.GEAR to buildGear(),
                Tab.STATS to buildStats(),
                Tab.PRAYER to buildPrayer()
            )
            shownPanel = tabPanels.getValue(tab)

            // BoxLayout lays children out by their alignmentX; a panel of
            // left-aligned labels (the prayer tab) otherwise reports 0.0, and
            // mixing that with centered siblings made the card want more than
            // CARD_W, squeezing the tab row until the last button wrapped away.
            header.alignmentX = Component.CENTER_ALIGNMENT
            tabs.alignmentX = Component.CENTER_ALIGNMENT
            tabPanels.values.forEach { it.alignmentX = Component.CENTER_ALIGNMENT }

            root.apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = WIDGET_COLOR
                alignmentX = Component.CENTER_ALIGNMENT
                border = BorderFactory.createMatteBorder(0, 0, 6, 0, VIEW_BACKGROUND_COLOR)
                add(header)
                add(tabs)
                add(shownPanel)
            }
            highlightTab()
            update(initial)
            fitHeight()
        }

        /** Apply new state. Returns true if the card's height changed. */
        fun update(m: MemberModel): Boolean {
            setIfChanged(nameLabel, " " + m.name)
            val hp = m.skills.getOrNull(3)
            setIfChanged(summaryLabel,
                "HP ${hp?.get(1) ?: "?"}/${hp?.get(0) ?: "?"}  " +
                "Pray ${m.prayerPoints.getOrNull(0) ?: "?"}/${m.prayerPoints.getOrNull(1) ?: "?"} ")

            for (slot in 0 until 28) invSlots[slot].image = m.inv.getOrNull(slot)
            for ((slot, view) in gearSlots) view.image = m.gear.getOrNull(slot)

            for ((skill, label) in statLabels) {
                val s = m.skills.getOrNull(skill)
                val boosted = s?.get(1)
                val base = s?.get(0)
                setIfChanged(label, "${boosted ?: "-"}/${base ?: "-"}")
                setIfChanged(label, when {
                    boosted == null || base == null || boosted == base -> secondaryColor
                    boosted > base -> Color(0, 200, 0)
                    else -> Color(220, 60, 60)
                })
            }
            setIfChanged(totalLabel, " Total: ${m.skills.take(24).sumOf { it.getOrElse(0) { 0 } }}")
            specBar.percent = m.spec

            setIfChanged(prayerPointsLabel,
                " Prayer points: ${m.prayerPoints.getOrNull(0) ?: "?"}/${m.prayerPoints.getOrNull(1) ?: "?"}")
            for ((i, slot) in prayerSlots.withIndex()) slot.active = (m.prayers shr i) and 1 == 1
            return false
        }

        fun repaintIcons() {
            prayerSlots.forEach { it.repaint() }
            tabButtons.values.forEach { it.repaint() }
        }

        private fun selectTab(t: Tab) {
            if (t == tab) return
            tab = t
            root.remove(shownPanel)
            shownPanel = tabPanels.getValue(t)
            root.add(shownPanel)
            highlightTab()
            fitHeight()
            relayout()
        }

        private fun highlightTab() {
            for ((t, b) in tabButtons) b.selected = t == tab
        }

        /** Size the card to the visible tab. Returns true if it changed. */
        private fun fitHeight(): Boolean {
            val h = HEADER_H + TABS_H + shownPanel.preferredSize.height + 6
            if (root.preferredSize.height == h) return false
            root.setFixedSize(CARD_W, h)
            return true
        }

        private fun buildInventory(): JPanel {
            val grid = JPanel(GridLayout(7, 4, 0, 0)).apply { background = WIDGET_COLOR }
            invSlots.forEach { grid.add(it) }
            return centered(grid, 4 * SLOT_W, 7 * SLOT_H)
        }

        private fun buildGear(): JPanel {
            val grid = JPanel(GridLayout(GEAR_LAYOUT.size, 3, 4, 0)).apply { background = WIDGET_COLOR }
            for (row in GEAR_LAYOUT) for (slot in row) {
                val view = ImageSlot(SLOT_W, SLOT_H, outlined = slot >= 0)
                if (slot >= 0) gearSlots[slot] = view
                grid.add(view)
            }
            return centered(grid, 3 * SLOT_W + 8, GEAR_LAYOUT.size * SLOT_H)
        }

        private fun buildStats(): JPanel {
            val order = ViewConstants.SKILL_DISPLAY_ORDER
            val rows = (order.size + 2) / 3
            val grid = JPanel(GridLayout(rows + 1, 3, 0, 0)).apply { background = WIDGET_COLOR }
            for (skill in order) {
                val cell = JPanel(FlowLayout(FlowLayout.LEFT, 2, 0)).apply { background = WIDGET_COLOR }
                cell.add(ImageSlot(ViewConstants.SKILL_SPRITE_SIZE.width, ViewConstants.SKILL_SPRITE_SIZE.height, false)
                    .apply { image = skillIcons[skill] })
                val label = textLabel("-/-", secondaryColor)
                statLabels[skill] = label
                cell.add(label)
                grid.add(cell)
            }
            grid.add(totalLabel)
            return centered(grid, CARD_W, (rows + 1) * STAT_ROW_H)
        }

        private fun buildPrayer(): JPanel {
            val rows = (prayerSlots.size + PRAYER_COLS - 1) / PRAYER_COLS
            val grid = JPanel(GridLayout(rows, PRAYER_COLS, 0, 0)).apply { background = WIDGET_COLOR }
            prayerSlots.forEach { grid.add(it) }
            repeat(rows * PRAYER_COLS - prayerSlots.size) { grid.add(JPanel().apply { background = WIDGET_COLOR }) }
            grid.setFixedSize(PRAYER_COLS * PRAYER_CELL, rows * PRAYER_CELL)

            prayerPointsLabel.alignmentX = Component.CENTER_ALIGNMENT
            grid.alignmentX = Component.CENTER_ALIGNMENT
            return JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = WIDGET_COLOR
                add(prayerPointsLabel)
                add(Box.createVerticalStrut(2))
                add(grid)
                setFixedSize(CARD_W, TEXT_ROW_H + 2 + rows * PRAYER_CELL + 4)
            }
        }

        private fun centered(inner: JComponent, w: Int, h: Int): JPanel {
            inner.setFixedSize(w, h)
            return JPanel(FlowLayout(FlowLayout.CENTER, 0, 2)).apply {
                background = WIDGET_COLOR
                add(inner)
                setFixedSize(CARD_W, h + 4)
            }
        }
    }
}
