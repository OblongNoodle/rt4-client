package BankTagLayouts

import BiggerBank.plugin as BiggerBankPlugin
import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import rt4.Component
import rt4.InterfaceList
import rt4.Inv
import rt4.ObjTypeList
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Rebuilt from scratch after studying the real source of both:
 *  - geheur/bank-tag-custom-layouts (the "Bank Tag Layouts" add-on)
 *  - runelite/runelite's core banktags/tabs/TabInterface.java (the base
 *    tab-column plugin the add-on extends - this is where the vertical tab
 *    bar itself actually lives, not in the add-on repo)
 *
 * The previous version drew everything as a plugin overlay (API.DrawText/
 * FillRect/sprite.render calls on top of an already-rendered frame), which
 * is fundamentally the wrong foundation: no real clipping against the
 * window, no native click integration, had to fake click-blocking by
 * resetting Mouse.pendingClickButton. RuneLite doesn't do it that way -
 * it creates real interface widgets as children of the bank's own widget
 * tree, so positioning/clipping/clicks all come from the game's own
 * systems for free.
 *
 * This version does the same: creates real rt4.Component objects (type=5,
 * if3=false so the client's existing hover-processing picks them up with no
 * script handlers needed - confirmed by reading InterfaceList.java's
 * hover-detection gate), assigns them as createdComponents of the bank's
 * host container, and polls Component.mouseOver (a field the client already
 * maintains) instead of manually computing screen bounds ourselves.
 * Component.noClickThrough handles world-click suppression natively.
 *
 * This needs genuine non-clipped screen space to the left of the bank
 * window, not just an overlay drawn there - so it coordinates with
 * BiggerBank via BiggerBank.plugin.externalLeftMargin instead of a second
 * plugin fighting over the same shared host-container fields every frame.
 * BiggerBank remains the single authority for host sizing; this just tells
 * it how much extra room to reserve.
 *
 * Layout proportions modeled on TabInterface.java's real constants
 * (TAB_WIDTH=39, TAB_HEIGHT=40, MARGIN=1, scrollable when tags overflow
 * available height), scaled down slightly for 2009scape's UI. Their tab
 * background/active/arrow sprites are custom-bundled plugin PNG art
 * (SpriteOverride ids) we don't have access to, so this reuses 2009scape's
 * own native tab-icon-frame sprite for the button background.
 *
 * Still phase 1: tagging + filtering only. Drag-to-reposition layouts,
 * placeholder icons, custom backgrounds, and the GE/HA value header are
 * separate future work.
 */
@PluginMeta(
        author = "OblongNoodle",
        description = "Bank Tag Layouts equivalent - tag bank items and browse by tag (phase 1: tagging + filtering).",
        version = 2.0
)
class plugin : Plugin() {

    companion object {
        const val BANK_IFACE = 762
        const val ITEM_CONTAINER_IDX = 73
        const val BANK_INV_ID = 95
        const val MAX_BANK_SLOTS = 1000

        const val HOST_IFACE = 746
        const val HOST_COMPONENT_IDX = 6

        // Native tab-icon button frame (top row, size 48x48) - source of the
        // sprite id we reuse for our button backgrounds.
        const val TAB_BUTTON_FRAME_IDX = 39

        // Proportions from RuneLite's real TabInterface.java (TAB_WIDTH=39,
        // TAB_HEIGHT=40, MARGIN=1), scaled down slightly.
        const val TAB_SIZE = 34
        const val MARGIN = 2
        const val ARROW_HEIGHT = 16
        const val TOP_MARGIN = 40
        const val LEFT_MARGIN_TOTAL = TAB_SIZE + 6

        var instance: plugin? = null
    }

    private val tags = LinkedHashMap<String, MutableSet<Int>>()
    private var activeTag: String? = null
    private var scrollOffset = 0

    // What each currently-live native Component represents, rebuilt every
    // frame alongside the components themselves.
    private sealed class ButtonInfo {
        object NewTab : ButtonInfo()
        object ScrollUp : ButtonInfo()
        object ScrollDown : ButtonInfo()
        class Tag(val name: String) : ButtonInfo()
    }
    private var liveButtons: Map<Component, ButtonInfo> = emptyMap()

    override fun Init() {
        instance = this
        loadTags()
        API.AddMouseListener(ClickHandler)
        API.AddMouseWheelListener(ScrollHandler)
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadTags() {
        val saved = API.GetData("banktaglayouts-tags") as? LinkedHashMap<String, HashSet<Int>>
        if (saved != null) {
            for ((name, ids) in saved) tags[name] = ids.toMutableSet()
        }
    }

    private fun saveTags() {
        val toSave = LinkedHashMap<String, HashSet<Int>>()
        for ((name, ids) in tags) toSave[name] = HashSet(ids)
        API.StoreData("banktaglayouts-tags", toSave)
    }

    // ─── Tagging via right-click ────────────────────────────────────────────

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        currentEntries ?: return
        val components = InterfaceList.components[BANK_IFACE] ?: return
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        for (entry in currentEntries) {
            // Bank item entries are type=CUSTOM with intArg2 pointing at the
            // item container's own component id, intArg1 as the bank slot
            // index - confirmed via ::dumpmenu, not documented anywhere.
            if (entry.type == MiniMenuType.CUSTOM && entry.intArg2 == container.id) {
                val slot = entry.intArg1
                val itemId = Inv.getItemType(BANK_INV_ID, slot)
                if (itemId <= 0) continue
                val itemName = ObjTypeList.get(itemId)?.name?.toString() ?: "item"
                API.InsertMiniMenuEntry("Tag", itemName) { promptTag(itemId) }
                break
            }
        }
    }

    private fun promptTag(itemId: Int) {
        SwingUtilities.invokeLater {
            val existing = tags.entries.filter { itemId in it.value }.map { it.key }
            val prompt = if (existing.isEmpty())
                "Enter tag name:"
            else
                "Enter tag name (already tagged: ${existing.joinToString(", ")}):"
            val name = JOptionPane.showInputDialog(null, prompt, "Bank Tag", JOptionPane.PLAIN_MESSAGE)
            val trimmed = name?.trim()
            if (!trimmed.isNullOrEmpty()) {
                tags.getOrPut(trimmed) { mutableSetOf() }.add(itemId)
                saveTags()
                API.SendMessage("Tagged as \"$trimmed\"")
            }
        }
    }

    private fun promptNewTag() {
        SwingUtilities.invokeLater {
            val name = JOptionPane.showInputDialog(null, "New tag name:", "Bank Tag", JOptionPane.PLAIN_MESSAGE)
            val trimmed = name?.trim()
            if (!trimmed.isNullOrEmpty() && !tags.containsKey(trimmed)) {
                tags[trimmed] = mutableSetOf()
                saveTags()
                activeTag = trimmed
            }
        }
    }

    // ─── Building real components each frame ────────────────────────────────

    override fun Draw(timeDelta: Long) {
        BiggerBankPlugin.externalLeftMargin = LEFT_MARGIN_TOTAL

        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        val bankComponents = InterfaceList.components[BANK_IFACE] ?: return
        val container = bankComponents.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val frame = bankComponents.getOrNull(TAB_BUTTON_FRAME_IDX)

        rebuildButtons(host, frame)
        applyFilter(container)
    }

    private fun makeButton(host: Component, x: Int, y: Int, size: Int, spriteId: Int): Component {
        val c = Component()
        c.id = -1
        c.type = 5
        c.overlayer = host.id
        c.x = x
        c.y = y
        c.width = size
        c.height = size
        c.hidden = false
        c.noClickThrough = true
        c.spriteId = spriteId
        return c
    }

    private fun rebuildButtons(host: Component, frame: Component?) {
        val frameSpriteId = frame?.spriteId ?: -1
        val newButtons = LinkedHashMap<Component, ButtonInfo>()
        val children = mutableListOf<Component>()

        var y = TOP_MARGIN
        children += makeButton(host, 2, y, TAB_SIZE, frameSpriteId).also { newButtons[it] = ButtonInfo.NewTab }
        y += TAB_SIZE + MARGIN

        val bottomLimit = host.height - 48
        val availableForTabs = bottomLimit - y - ARROW_HEIGHT * 2
        val visibleCount = maxOf(1, availableForTabs / (TAB_SIZE + MARGIN))
        val names = tags.keys.toList()
        val needsScroll = names.size > visibleCount

        if (needsScroll) {
            children += makeButton(host, 2, y, TAB_SIZE, -1).also {
                it.height = ARROW_HEIGHT
                newButtons[it] = ButtonInfo.ScrollUp
            }
            y += ARROW_HEIGHT + MARGIN
        }

        val clampedOffset = scrollOffset.coerceIn(0, maxOf(0, names.size - visibleCount))
        scrollOffset = clampedOffset
        for (i in clampedOffset until minOf(names.size, clampedOffset + visibleCount)) {
            val name = names[i]
            children += makeButton(host, 2, y, TAB_SIZE, frameSpriteId).also { newButtons[it] = ButtonInfo.Tag(name) }
            y += TAB_SIZE + MARGIN
        }

        if (needsScroll) {
            children += makeButton(host, 2, y, TAB_SIZE, -1).also {
                it.height = ARROW_HEIGHT
                newButtons[it] = ButtonInfo.ScrollDown
            }
        }

        host.createdComponents = children.toTypedArray()
        liveButtons = newButtons
    }

    override fun ComponentDraw(componentIndex: Int, component: Component, screenX: Int, screenY: Int) {
        when (val info = liveButtons[component] ?: return) {
            is ButtonInfo.NewTab ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "+", screenX + TAB_SIZE / 2, screenY + TAB_SIZE / 2 + 4)
            is ButtonInfo.ScrollUp ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "^", screenX + TAB_SIZE / 2, screenY + ARROW_HEIGHT - 3)
            is ButtonInfo.ScrollDown ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "v", screenX + TAB_SIZE / 2, screenY + ARROW_HEIGHT - 3)
            is ButtonInfo.Tag -> {
                if (info.name == activeTag) {
                    API.DrawRect(screenX, screenY, TAB_SIZE, TAB_SIZE, 0xffff00)
                }
                val iconItem = tags[info.name]?.firstOrNull()
                if (iconItem != null) {
                    API.GetObjSprite(iconItem, 1, false, 0, 0)?.render(screenX, screenY)
                } else {
                    API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.CENTER, "?", screenX + TAB_SIZE / 2, screenY + TAB_SIZE / 2 + 4)
                }
            }
        }
    }

    private val WHITE = FontColor.fromColor(java.awt.Color.WHITE)

    private fun applyFilter(container: Component) {
        val tag = activeTag
        val children = container.createdComponents ?: return
        if (tag == null) return
        val allowed = tags[tag] ?: return

        // createdComponents holds one entry per non-empty bank slot, in slot
        // order (confirmed while building BiggerBank). Walk real slots in
        // the same order to know which item id each visible component is.
        var slot = 0
        for (child in children) {
            child ?: continue
            if (child.width != 36 || child.height != 32) continue
            var itemId = -1
            while (slot < MAX_BANK_SLOTS) {
                val id = Inv.getItemType(BANK_INV_ID, slot)
                slot++
                if (id > 0) {
                    itemId = id
                    break
                }
            }
            if (itemId <= 0) continue
            child.hidden = itemId !in allowed
        }
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (commandStr.equals("::banktags", ignoreCase = true)) {
            API.SendMessage("Tags: " + tags.keys.joinToString(", ").ifEmpty { "(none yet - right-click a bank item and choose Tag)" })
        }
    }

    object ClickHandler : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            val p = instance ?: return
            // No manual bounds math and no pendingClickButton hack needed -
            // these are real components with noClickThrough=true, and
            // mouseOver is maintained by the client's own hover processing.
            for ((component, info) in p.liveButtons) {
                if (component.mouseOver) {
                    when (info) {
                        is ButtonInfo.NewTab -> p.promptNewTag()
                        is ButtonInfo.ScrollUp -> p.scrollOffset = maxOf(0, p.scrollOffset - 1)
                        is ButtonInfo.ScrollDown -> p.scrollOffset += 1
                        is ButtonInfo.Tag -> p.activeTag = if (p.activeTag == info.name) null else info.name
                    }
                    return
                }
            }
        }
    }

    object ScrollHandler : MouseWheelListener {
        override fun mouseWheelMoved(e: MouseWheelEvent?) {
            e ?: return
            val p = instance ?: return
            if (p.liveButtons.keys.any { it.mouseOver }) {
                p.scrollOffset = (p.scrollOffset + e.wheelRotation).coerceAtLeast(0)
            }
        }
    }
}
