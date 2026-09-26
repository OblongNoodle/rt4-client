package KondoKit

import KondoKit.util.Helpers.getSpriteId
import KondoKit.util.Helpers.showAlert
import KondoKit.views.*
import KondoKit.ui.OnUpdateCallback
import KondoKit.ui.OnDrawCallback
import KondoKit.ui.OnXPUpdateCallback
import KondoKit.ui.OnKillingBlowNPCCallback
import KondoKit.ui.OnPostClientTickCallback
import KondoKit.util.AltCanvas
import KondoKit.util.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.util.ImageCanvas
import KondoKit.util.setFixedSize
import KondoKit.ui.ViewConstants
import KondoKit.ui.theme.Themes.Theme
import KondoKit.ui.theme.Themes.ThemeType
import KondoKit.ui.theme.Themes.getTheme
import KondoKit.ui.components.ScrollablePanel
import plugin.Plugin
import plugin.PluginRepository
import plugin.api.*
import plugin.api.API.*
import plugin.api.FontColor.fromColor
import rt4.*
import rt4.DisplayMode
import rt4.GameShell.canvas
import rt4.GameShell.frame
import rt4.client.js5Archive8
import rt4.client.mainLoadState
import java.awt.*
import java.awt.Font
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import KondoKit.ui.View
import KondoKit.util.Helpers


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Exposed(val description: String = "")

class plugin : Plugin() {
    companion object {
        val IMAGE_SIZE = Dimension(25, 23)

        // Default Theme Colors
        var WIDGET_COLOR = Color(30, 30, 30)
        var TITLE_BAR_COLOR = Color(21, 21, 21)
        var VIEW_BACKGROUND_COLOR = Color(40, 40, 40)
        var primaryColor = Color(165, 165, 165)   // Color for "XP Gained:"
        var secondaryColor = Color(255, 255, 255) // Color for "0"
        var POPUP_BACKGROUND = Color(45, 45, 45)
        var POPUP_FOREGROUND = Color(220, 220, 220)
        var TOOLTIP_BACKGROUND = Color(50,50,50)
        var SCROLL_BAR_COLOR = Color(64, 64, 64)
        var PROGRESS_BAR_FILL = Color(61, 56, 49)
        var NAV_TINT: Color? = null
        var NAV_GREYSCALE = false
        var BOOST = 1f

        var appliedTheme = ThemeType.RUNELITE

        @Exposed("Theme colors for KondoKit, requires a relaunch to apply.")
        var theme = ThemeType.RUNELITE

        @Exposed("Default: true, Use Local JSON or the prices from the Live/Stable server API")
        var useLiveGEPrices = true

        @Exposed("Used to calculate Combat Actions until next level.")
        var playerXPMultiplier = 5

        @Exposed("Start minimized/collapsed by default")
        var launchMinimized = false

        @Exposed("Default 16 on Windows, 0 Linux/macOS. If Kondo is not " +
                "perfectly snapped to the edge of the game due to window chrome you can update this to fix it")
        var uiOffset = 0

        @Exposed("Stretched/Scaled Fixed Mode Support")
        var useScaledFixed = false

        @Exposed("Party: relay server URL shared by everyone in the party")
        var partyServerUrl = KondoKit.party.PartyClient.DEFAULT_SERVER

        const val FIXED_WIDTH = 765
        const val FIXED_HEIGHT = 503
        private const val NAVBAR_WIDTH = 30
        private const val MAIN_CONTENT_WIDTH = 242
        const val WRENCH_ICON = 907
        const val LOOT_ICON = 777
        const val MAG_SPRITE = 1423
        const val LVL_ICON = 898
        private lateinit var cardLayout: CardLayout
        private lateinit var mainContentPanel: JPanel
        private var rightPanelWrapper: JScrollPane? = null
        private var accumulatedTime = 0L
        private var reloadInterfaces = false
        private const val TICK_INTERVAL = 600L
        private var pluginsReloaded = false
        private var loginScreen = 160
        private var lastLogin = ""
        private var initialized = false
        private var lastClickTime = 0L
        private var lastUIOffset = 0
        private var themeName = "RUNELITE"
        private const val HIDDEN_VIEW = "HIDDEN"
        private var altCanvas: AltCanvas? = null
        private val drawActions = mutableListOf<() -> Unit>()
        private val views = mutableListOf<View>()
        private val updateCallbacks = mutableListOf<OnUpdateCallback>()
        private val drawCallbacks = mutableListOf<OnDrawCallback>()
        private val xpUpdateCallbacks = mutableListOf<OnXPUpdateCallback>()
        private val killingBlowNPCCallbacks = mutableListOf<OnKillingBlowNPCCallback>()
        private val postClientTickCallbacks = mutableListOf<OnPostClientTickCallback>()

        // Generic persistence for every plugin's @Exposed fields, so none of
        // them need to hand-roll their own API.GetData/StoreData boilerplate
        // (SettingsPanel's "=>" button and any ::command handler that sets
        // an @Exposed field directly both only ever touch the in-memory
        // field via reflection or a plain assignment - neither one has ever
        // persisted anything to plsto on its own).
        //
        // Keyed by instance (WeakHashMap, not by plugin name/class) rather
        // than a plain seen-once set, since a hot-reloaded plugin (see
        // OnPluginsReloaded) gets a genuinely new instance with its fields
        // back at their compiled defaults - restoration needs to run again
        // for that new instance, not be skipped because the class was
        // "already handled" once before.
        private val restoredFieldsByInstance = java.util.WeakHashMap<Plugin, MutableSet<String>>()
        private val lastKnownExposedValues = mutableMapOf<String, Any?>()
        private var lastPersistenceScan = 0L
        private const val PERSISTENCE_SCAN_INTERVAL_MS = 2000L

        // Confirmed live: this plugin manager reloads plugins multiple
        // times shortly after startup (a "check for plugin updates" pass),
        // giving each reloaded plugin a brand new instance back at its
        // compiled defaults. The "detect a changed value and persist it"
        // step below must never run for a field before its restore has
        // actually had a fair chance to succeed - doing so treats a
        // still-default, not-yet-restored value as a genuine user change
        // and immediately overwrites the real saved value on disk with the
        // default, which is a real, confirmed-live data-loss bug (this
        // plugin manager's own reload path calls SaveStorage() itself,
        // permanently flushing whatever damage was already done in
        // memory). A field only becomes safe to track/store once its OWN
        // restore has either succeeded, or come back empty on enough
        // separate cycles in a row to be confident that's genuinely because
        // nothing was ever saved - not because this cycle just landed on a
        // short-lived instance moments before it gets replaced. Keyed by
        // key (not instance) since what matters is "has THIS SETTING been
        // given a fair chance yet at all", across however many instances
        // came and went before things settled.
        private val confirmedAbsentCounts = mutableMapOf<String, Int>()
        private const val CONFIRM_ABSENT_THRESHOLD = 3

        // Only types the JVM's bootstrap/platform classloader already knows
        // about, unconditionally - safe to deserialize back regardless of
        // whether any plugin classloader has been set up yet. Deliberately
        // excludes enums and other classes defined inside a plugin (see
        // this function's call site for why that's not just "won't work
        // for that one field" but "breaks loading every plugin's settings
        // until the file gets manually fixed or deleted").
        private fun isSafeToAutoPersist(type: Class<*>): Boolean {
            return type == Int::class.javaPrimitiveType || type == java.lang.Integer::class.java ||
                    type == Boolean::class.javaPrimitiveType || type == java.lang.Boolean::class.java ||
                    type == Float::class.javaPrimitiveType || type == java.lang.Float::class.java ||
                    type == Double::class.javaPrimitiveType || type == java.lang.Double::class.java ||
                    type == Long::class.javaPrimitiveType || type == java.lang.Long::class.java ||
                    type == String::class.java
        }

        private fun syncExposedSettings() {
            val now = System.currentTimeMillis()
            if (now - lastPersistenceScan < PERSISTENCE_SCAN_INTERVAL_MS) return
            lastPersistenceScan = now

            val loadedPlugins = try {
                val field = PluginRepository::class.java.getDeclaredField("loadedPlugins")
                field.isAccessible = true
                (field.get(null) as? HashMap<*, *>)?.values ?: return
            } catch (e: Exception) {
                return
            }

            for (pluginObj in loadedPlugins) {
                val p = pluginObj as? Plugin ?: continue
                val pluginDirName = p.javaClass.`package`?.name?.substringBefore(".") ?: continue
                val restoredFields = restoredFieldsByInstance.getOrPut(p) { mutableSetOf() }
                var restoredAnything = false

                // Confirmed live (client stack trace, PluginRepository.
                // Init:81) that saving even ONE plugin-defined type (an
                // enum nested in a plugin class, e.g. XPDropPlugin's own
                // Theme) permanently breaks loading plsto for EVERY plugin,
                // not just the one that owns it: plugin classes live in a
                // separate classloader created only after plsto is read,
                // so ObjectInputStream can never resolve that class -
                // deserializing the WHOLE map throws, and pluginStorage
                // silently starts empty on every single boot from then on.
                // This was the actual, permanent root cause behind "nothing
                // ever restores" - not a timing/instance issue. Restricting
                // auto-persistence to universally-resolvable JDK types
                // avoids this entirely; a plugin whose own settings need a
                // custom/enum type still needs to hand-roll that one field
                // itself (matching how this already worked before this
                // generic system existed).
                val exposedFields = p.javaClass.declaredFields.filter { field ->
                    field.annotations.any { it.annotationClass.simpleName == "Exposed" } &&
                            isSafeToAutoPersist(field.type)
                }

                for (field in exposedFields) {
                    field.isAccessible = true
                    val key = "kondokit.autosave.$pluginDirName.${field.name}"

                    if (field.name !in restoredFields) {
                        try {
                            val saved = API.GetData(key)
                            if (saved != null) {
                                field.set(p, saved)
                                restoredFields.add(field.name)
                                restoredAnything = true
                            } else {
                                confirmedAbsentCounts[key] = (confirmedAbsentCounts[key] ?: 0) + 1
                            }
                        } catch (e: Exception) {
                            // A bad/incompatible saved value (e.g. after a
                            // field's type changed between versions) must
                            // never stop the plugin from loading with its
                            // own compiled default instead. Mark as handled
                            // regardless so a permanently-broken value
                            // doesn't retry (and fail) forever.
                            restoredFields.add(field.name)
                        }
                    }

                    // Only track/persist this field once it's actually
                    // settled (see confirmedAbsentCounts' own comment) -
                    // otherwise a not-yet-restored default gets treated as
                    // a genuine change and overwrites the real saved value.
                    val settled = field.name in restoredFields ||
                            (confirmedAbsentCounts[key] ?: 0) >= CONFIRM_ABSENT_THRESHOLD
                    if (settled) {
                        try {
                            val current = field.get(p)
                            if (lastKnownExposedValues[key] != current) {
                                lastKnownExposedValues[key] = current
                                API.StoreData(key, current)
                            }
                        } catch (e: Exception) {
                            // Skip fields that can't be safely read/serialized.
                        }
                    }
                }

                // Restoring a field only changes what it holds - plugins
                // that push a setting into native/global state (anything
                // beyond just reading the field live each time it's used)
                // need a chance to re-apply it now that the real value is
                // in place. Same opt-in convention FieldNotifier already
                // uses for live GUI edits: implement a no-arg
                // OnKondoValueUpdated() and it gets called; plugins that
                // just read their fields live don't need it.
                if (restoredAnything) {
                    try {
                        val onUpdate = p::class.java.getMethod("OnKondoValueUpdated")
                        onUpdate.invoke(p)
                    } catch (e: NoSuchMethodException) {
                        // No opt-in - nothing further to do.
                    } catch (e: Exception) {
                        // Best-effort - a broken re-apply must never stop
                        // restoration of other plugins/fields.
                    }
                }
            }

            // PluginRepository.SaveStorage() (which writes plsto to disk)
            // was only ever wired to a JVM shutdown hook - meaning it only
            // saved anything on a clean exit. Given how often this whole
            // project has run into crashes/freezes, that's a real gap: a
            // session that ends any other way (crash, force-close, task-
            // killed) never flushes whatever changed that session, even
            // though it was tracked correctly in memory the whole time -
            // confirmed as the actual cause of a report that settings
            // "aren't saving" despite the restore/store logic above working
            // correctly. SaveStorage() already checks its own dirty flag
            // and no-ops when nothing changed since the last write, so
            // calling it every sync cycle is cheap - this just means a
            // crash can lose at most PERSISTENCE_SCAN_INTERVAL_MS worth of
            // changes instead of the whole session's.
            PluginRepository.SaveStorage()
        }

        fun registerDrawAction(action: () -> Unit) {
            synchronized(drawActions) {
                drawActions.add(action)
            }
        }
        
        fun registerUpdateCallback(callback: OnUpdateCallback) {
            updateCallbacks.add(callback)
        }
        
        fun registerDrawCallback(callback: OnDrawCallback) {
            drawCallbacks.add(callback)
        }
        
        fun registerXPUpdateCallback(callback: OnXPUpdateCallback) {
            xpUpdateCallbacks.add(callback)
        }
        
        fun registerKillingBlowNPCCallback(callback: OnKillingBlowNPCCallback) {
            killingBlowNPCCallbacks.add(callback)
        }
        
        fun registerPostClientTickCallback(callback: OnPostClientTickCallback) {
            postClientTickCallbacks.add(callback)
        }
    }

    override fun Init() {
        System.setProperty("sun.java2d.opengl", "false")
        System.setProperty("awt.useSystemAAFontSettings", "off")
        System.setProperty("swing.aatext", "false")
    }

    override fun OnLogin() {
        if (lastLogin != "" && lastLogin != Player.usernameInput.toString()) {
            XPTrackerView.xpTrackerView?.let { XPTrackerView.resetXPTracker(it) }
        }
        lastLogin = Player.usernameInput.toString()
    }

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        if (currentEntries != null) {
            for ((index, entry) in currentEntries.withIndex()) {
                if (entry.type == MiniMenuType.PLAYER && index == currentEntries.size - 1) {
                    val input = entry.subject
                    val cleanedInput = input
                            .trim()
                            .replace(Regex("<col=[0-9a-fA-F]{6}>"), "")
                            .replace(Regex("<img=\\d+>"), "")
                            .replace(Regex("\\(level: \\d+\\)"), "")
                            .trim()
                    InsertMiniMenuEntry("Lookup", entry.subject, searchHiscore(cleanedInput))
                }
            }
        }
    }

    override fun OnPluginsReloaded(): Boolean {
        if (!initialized) return true
        // Ensure Swing updates happen on the EDT to avoid flicker
        SwingUtilities.invokeLater {
            updateDisplaySettings()
            rightPanelWrapper?.let { wrapper ->
                wrapper.ignoreRepaint = true
                try {
                    val parent = wrapper.parent
                    val wrapperNeedsAttach = parent != frame
                    if (wrapperNeedsAttach) {
                        parent?.remove(wrapper)
                        frame.layout = BorderLayout()
                        frame.add(wrapper, BorderLayout.EAST)
                    }
                    wrapper.revalidate()
                    wrapper.repaint()
                } finally {
                    wrapper.ignoreRepaint = false
                }
            }
            frame.revalidate()
            frame.repaint()
            
            ReflectiveEditorView.addPlugins(ReflectiveEditorView.panel)
        }
        pluginsReloaded = true
        reloadInterfaces = true
        return true
    }

    override fun OnXPUpdate(skillId: Int, xp: Int) {
        xpUpdateCallbacks.forEach { callback ->
            callback.onXPUpdate(skillId, xp)
        }
    }

    override fun Draw(timeDelta: Long) {
        if (GlRenderer.enabled && GlRenderer.canvasWidth != GameShell.canvasWidth) {
            GlRenderer.canvasWidth = GameShell.canvasWidth
            GlRenderer.setViewportBounds(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
        }

        if (pluginsReloaded) {
            SwingUtilities.invokeLater {
                ReflectiveEditorView.addPlugins(ReflectiveEditorView.panel)
            }
            pluginsReloaded = false
        }

        if (reloadInterfaces){
            InterfaceList.layoutTopLevel(true) // Gets the resize working correctly
            reloadInterfaces = false
        }

        accumulatedTime += timeDelta
        if (accumulatedTime >= TICK_INTERVAL) {
            postClientTickCallbacks.forEach { callback ->
                callback.onPostClientTick()
            }
            accumulatedTime = 0L
        }

        syncExposedSettings()

        drawCallbacks.forEach { callback ->
            callback.onDraw(timeDelta)
        }

        // Draw synced actions (that require to be done between glBegin and glEnd)
        if (drawActions.isNotEmpty()) {
            synchronized(drawActions) {
                val actionsCopy = drawActions.toList()
                drawActions.clear()
                for (action in actionsCopy) {
                    action()
                }
            }
        }

        // Init in the draw call so we know we are between glBegin and glEnd for HD
        if(!initialized && mainLoadState >= loginScreen) {
            initKondoUI()
        }
    }

    override fun LateDraw(timeDelta: Long) {
        if (!initialized) return
        if(GameShell.fullScreenFrame != null) {
            DisplayMode.setWindowMode(true, 0, FIXED_WIDTH, FIXED_HEIGHT)
            showAlert("Fullscreen is not supported by KondoKit. Disable the plugin first.",
                "Error",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        if(!useScaledFixed) return
        if(GetWindowMode() == WindowMode.FIXED){
            moveAltCanvasToFront()
        } else {
            moveCanvasToFront()
        }
        altCanvas?.updateGameImage()
    }

    override fun Update() {
        updateCallbacks.forEach { callback ->
            callback.onUpdate()
        }
    }

    override fun OnKillingBlowNPC(npcID: Int, x: Int, z: Int) {
        killingBlowNPCCallbacks.forEach { callback ->
            callback.onKillingBlowNPC(npcID, x, z)
        }
    }

    private fun allSpritesLoaded() : Boolean {
        try{
            for (i in 0 until 24) {
                if(!js5Archive8.isFileReady(getSpriteId(i))){
                    return false
                }
            }
            val otherIcons = arrayOf(LVL_ICON, MAG_SPRITE, LOOT_ICON, WRENCH_ICON, ViewConstants.COMBAT_LVL_SPRITE, LootTrackerView.BAG_ICON)
            for (icon in otherIcons) {
                if(!js5Archive8.isFileReady(icon)){
                    return false
                }
            }
        } catch (e : Exception){
            return false
        }
        return true
    }

    private fun updateDisplaySettings() {
        val applyDisplaySettings = {
            val mode = GetWindowMode()
            val currentScrollPaneWidth = if (mainContentPanel.isVisible) NAVBAR_WIDTH + MAIN_CONTENT_WIDTH else NAVBAR_WIDTH
            lastUIOffset = uiOffset

            // Ensure the scroll wrapper stays attached on the EAST edge even if the game resets the layout
            rightPanelWrapper?.let { wrapper ->
                val needsLayoutReset = frame.layout !is BorderLayout
                val needsAttach = wrapper.parent != frame
                if (needsLayoutReset || needsAttach) {
                    wrapper.parent?.remove(wrapper)
                    frame.layout = BorderLayout()
                    frame.add(wrapper, BorderLayout.EAST)
                    if (altCanvas != null) {
                        moveAltCanvasToFront()
                    } else {
                        moveCanvasToFront()
                    }
                }
            }

            if(mode != WindowMode.FIXED) {
                destroyAltCanvas()
            } else if (useScaledFixed && altCanvas == null) {
                initAltCanvas()
            } else if (!useScaledFixed && altCanvas != null) {
                // Was using scaled fixed but toggled the setting
                // restore the original canvas
                moveCanvasToFront()
                destroyAltCanvas()
            }

            when (mode) {
                WindowMode.FIXED -> {
                    if (frame.width < FIXED_WIDTH + currentScrollPaneWidth + uiOffset) {
                        frame.setSize(FIXED_WIDTH + currentScrollPaneWidth + uiOffset, frame.height)
                    }

                    val difference = frame.width - (uiOffset + currentScrollPaneWidth)

                    if (useScaledFixed) {
                        GameShell.leftMargin = 0
                        val canvasWidth = difference + uiOffset / 2
                        val canvasHeight = frame.height - canvas.y // Restricting height to frame height

                        altCanvas?.size = Dimension(canvasWidth, canvasHeight)
                        altCanvas?.setLocation(0, canvas.y)
                        canvas.setLocation(0, canvas.y)
                    } else {
                        val difference = frame.width - (FIXED_WIDTH + uiOffset + currentScrollPaneWidth)
                        GameShell.leftMargin = difference / 2
                    }
                }

                WindowMode.RESIZABLE -> {
                    GameShell.canvasWidth = frame.width - (currentScrollPaneWidth + uiOffset)
                }
            }

            rightPanelWrapper?.preferredSize = Dimension(currentScrollPaneWidth, frame.height)
            rightPanelWrapper?.isDoubleBuffered = true
            rightPanelWrapper?.revalidate()
            rightPanelWrapper?.repaint()
            frame.validate()
        }

        if (SwingUtilities.isEventDispatchThread()) {
            applyDisplaySettings()
        } else {
            SwingUtilities.invokeLater { applyDisplaySettings() }
        }
    }

    fun OnKondoValueUpdated(){
        StoreData("kondoUseRemoteGE", useLiveGEPrices)
        StoreData("kondoTheme", theme.toString())
        if(appliedTheme != theme) {
            showAlert(
                "KondoKit Theme changes require a relaunch.",
                "KondoKit",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        StoreData("kondoPlayerXPMultiplier", playerXPMultiplier)
        LootTrackerView.refreshGEPrices()
        StoreData("kondoLaunchMinimized", launchMinimized)
        StoreData("kondoUIOffset", uiOffset)
        StoreData("kondoScaledFixed", useScaledFixed)
        if(lastUIOffset != uiOffset){
            reloadInterfaces = true
        }
        updateDisplaySettings()
    }

    private fun initAltCanvas(){
        if(GetWindowMode() != WindowMode.FIXED || altCanvas != null) return
        if (frame != null) {
            altCanvas = AltCanvas().apply {
                preferredSize = Dimension(FIXED_WIDTH, FIXED_HEIGHT)
            }
            altCanvas?.let { frame.add(it) }
            moveAltCanvasToFront()
            frame.setComponentZOrder(rightPanelWrapper, 2)
        }
    }

    private fun destroyAltCanvas(){
        if (altCanvas == null) return
        moveCanvasToFront()
        frame.remove(altCanvas)
        altCanvas = null
    }

    private fun moveAltCanvasToFront(){
        if (altCanvas == null) return
        frame.setComponentZOrder(canvas, 2)
        frame.setComponentZOrder(altCanvas, 1)
        frame.setComponentZOrder(rightPanelWrapper, 0)
    }

    private fun moveCanvasToFront(){
        if (altCanvas == null) return
        frame.setComponentZOrder(altCanvas, 2)
        frame.setComponentZOrder(canvas, 1)
        frame.setComponentZOrder(rightPanelWrapper, 0)
    }

    private fun searchHiscore(username: String): Runnable {
        return Runnable {
            setActiveView(HiscoresView.VIEW_NAME)
            HiscoresView.hiScoreView?.let { hiscoresPanel ->
                HiscoresView.searchPlayerForHiscores(username, hiscoresPanel)
            } ?: run {
                println("hiscoresPanel is null")
            }
        }
    }

    private fun restoreSettings(){
        themeName = (GetData("kondoTheme") as? String) ?: "RUNELITE"
        useLiveGEPrices = (GetData("kondoUseRemoteGE") as? Boolean) ?: true
        playerXPMultiplier = (GetData("kondoPlayerXPMultiplier") as? Int) ?: 5
        val osName = System.getProperty("os.name").toLowerCase()
        uiOffset = (GetData("kondoUIOffset") as? Int) ?: if (osName.contains("win")) 16 else 0
        launchMinimized = (GetData("kondoLaunchMinimized") as? Boolean) ?: false
        useScaledFixed = (GetData("kondoScaledFixed") as? Boolean) ?: false
    }

    private fun initKondoUI(){
        DrawText(FontType.LARGE, fromColor(Color(16777215)), TextModifier.CENTER, "KondoKit Loading Sprites...", GameShell.canvasWidth/2, GameShell.canvasHeight/2)
        if(!allSpritesLoaded()) return
        val frame: Frame? = GameShell.frame
        if (frame != null) {
            restoreSettings()
            theme = ThemeType.valueOf(themeName)
            applyTheme(getTheme(theme))
            appliedTheme = theme
            configureLookAndFeel()

            cardLayout = CardLayout()
            mainContentPanel = JPanel(cardLayout).apply {
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)
                background = VIEW_BACKGROUND_COLOR
                preferredSize = Dimension(MAIN_CONTENT_WIDTH, frame.height)
                isOpaque = true
            }

            val xpTrackerView = XPTrackerView
            val hiscoresView = HiscoresView
            val lootTrackerView = LootTrackerView
            val reflectiveEditorView = ReflectiveEditorView
            val partyView = PartyView
            
            xpTrackerView.createView()
            hiscoresView.createView()
            lootTrackerView.createView()
            partyView.createView()
            reflectiveEditorView.createView()
            
            views.add(xpTrackerView)
            views.add(hiscoresView)
            views.add(lootTrackerView)
            views.add(partyView)
            views.add(reflectiveEditorView)
            
            xpTrackerView.registerFunctions()
            hiscoresView.registerFunctions()
            lootTrackerView.registerFunctions()
            partyView.registerFunctions()
            reflectiveEditorView.registerFunctions()

            mainContentPanel.add(ScrollablePanel(xpTrackerView.panel), xpTrackerView.name)
            mainContentPanel.add(ScrollablePanel(hiscoresView.panel), hiscoresView.name)
            mainContentPanel.add(ScrollablePanel(lootTrackerView.panel), lootTrackerView.name)
            mainContentPanel.add(ScrollablePanel(partyView.panel), partyView.name)
            mainContentPanel.add(ScrollablePanel(reflectiveEditorView.panel), reflectiveEditorView.name)

            val navPanel = Panel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = WIDGET_COLOR
                preferredSize = Dimension(NAVBAR_WIDTH, frame.height)
            }

            navPanel.add(createNavButton(xpTrackerView.iconSpriteId, xpTrackerView.name))
            navPanel.add(createNavButton(hiscoresView.iconSpriteId, hiscoresView.name))
            navPanel.add(createNavButton(lootTrackerView.iconSpriteId, lootTrackerView.name))
            navPanel.add(createNavButton(partyView.iconSpriteId, partyView.name))
            navPanel.add(createNavButton(reflectiveEditorView.iconSpriteId, reflectiveEditorView.name))

            val rightPanel = Panel(BorderLayout()).apply {
                add(mainContentPanel, BorderLayout.CENTER)
                add(navPanel, BorderLayout.EAST)
            }

            rightPanelWrapper = JScrollPane(rightPanel).apply {
                preferredSize = Dimension(NAVBAR_WIDTH + MAIN_CONTENT_WIDTH, frame.height)
                background = VIEW_BACKGROUND_COLOR
                border = BorderFactory.createEmptyBorder()
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
            }

            val desiredView = if (launchMinimized) HIDDEN_VIEW else xpTrackerView.name
            // Commit layout synchronously on the EDT to avoid initial misplacement
            val commit = Runnable {
                frame.layout = BorderLayout()
                rightPanelWrapper?.let { frame.add(it, BorderLayout.EAST) }
                setActiveView(desiredView)
                frame.validate()
                frame.repaint()
            }
            if (SwingUtilities.isEventDispatchThread()) {
                commit.run()
            } else {
                try {
                    SwingUtilities.invokeAndWait(commit)
                } catch (e: Exception) {
                    // Fallback to async if invokeAndWait fails for any reason
                    SwingUtilities.invokeLater(commit)
                }
            }
            initialized = true
            pluginsReloaded = true
            updateDisplaySettings()
        }
    }

    private fun setActiveView(viewName: String) {
        val runUpdate: () -> Unit = {
            // Track visibility change to decide if we need to resize/reload interfaces
            val wasVisible = mainContentPanel.isVisible

            // Handle the visibility of the main content panel and card switch
            if (viewName == HIDDEN_VIEW) {
                mainContentPanel.isVisible = false
            } else {
                if (!mainContentPanel.isVisible) {
                    mainContentPanel.isVisible = true
                }
                cardLayout.show(mainContentPanel, viewName)
            }

            val visibilityChanged = wasVisible != mainContentPanel.isVisible

            // Batch painting to avoid intermediate repaints
            rightPanelWrapper?.ignoreRepaint = true
            try {
                if (visibilityChanged) {
                    // Only touch layout and client interfaces if width actually changes
                    updateDisplaySettings()
                    reloadInterfaces = true
                    rightPanelWrapper?.revalidate()
                    frame?.validate()
                } else {
                    // Just a card switch; avoid full frame revalidate
                    mainContentPanel.revalidate()
                }
            } finally {
                rightPanelWrapper?.ignoreRepaint = false
            }

            // Targeted repaint for snappy feedback
            if (visibilityChanged) {
                rightPanelWrapper?.repaint()
                frame?.repaint()
            } else {
                mainContentPanel.repaint()
            }
            StateManager.focusedView = viewName
        }

        if (SwingUtilities.isEventDispatchThread()) {
            runUpdate()
        } else {
            SwingUtilities.invokeLater { runUpdate() }
        }
    }

    private fun createNavButton(spriteId: Int, viewName: String): JPanel {
        val bufferedImageSprite = getBufferedImageFromSprite(GetSprite(spriteId), NAV_TINT, NAV_GREYSCALE, BOOST)
        val buttonSize = Dimension(NAVBAR_WIDTH, 32)
        val imageSize = Dimension((bufferedImageSprite.width / 1.2f).toInt(), (bufferedImageSprite.height / 1.2f).toInt())
        val cooldownDuration = 100L

        val actionListener = ActionListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < cooldownDuration) {
                return@ActionListener
            }
            lastClickTime = currentTime

            if (StateManager.focusedView == viewName) {
                setActiveView("HIDDEN")
            } else {
                setActiveView(viewName)
            }
        }

        val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
            background = WIDGET_COLOR
            setFixedSize(imageSize)
        }

        // Wrapping the ImageCanvas in another JPanel to prevent stretching
        val imageCanvasWrapper = JPanel().apply {
            layout = GridBagLayout() // Keeps the layout of the wrapped panel minimal
            setFixedSize(imageSize)
            isOpaque = false // No background for the wrapper
            add(imageCanvas) // Adding ImageCanvas directly, layout won't stretch it
        }

        val panelButton = JPanel().apply {
            layout = GridBagLayout()
            setFixedSize(buttonSize)
            background = WIDGET_COLOR
            isOpaque = true

            val gbc = GridBagConstraints().apply {
                anchor = GridBagConstraints.CENTER
                fill = GridBagConstraints.NONE // Prevents stretching
            }

            add(imageCanvasWrapper, gbc)

            val hoverListener = object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    background = WIDGET_COLOR.darker()
                    imageCanvas.fillColor = WIDGET_COLOR.darker()
                    imageCanvas.repaint()
                    repaint()
                }

                override fun mouseExited(e: MouseEvent?) {
                    background = WIDGET_COLOR
                    imageCanvas.fillColor = WIDGET_COLOR
                    imageCanvas.repaint()
                    repaint()
                }

                override fun mouseClicked(e: MouseEvent?) {
                    actionListener.actionPerformed(null)
                }
            }

            addMouseListener(hoverListener)
            imageCanvas.addMouseListener(hoverListener)
        }

        return panelButton
    }

    private fun configureLookAndFeel(){
        loadFont()
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")

            // Modify the UI properties to match theme
            UIManager.put("control", VIEW_BACKGROUND_COLOR)
            UIManager.put("info", VIEW_BACKGROUND_COLOR)
            UIManager.put("nimbusBase", WIDGET_COLOR)
            UIManager.put("nimbusBlueGrey", TITLE_BAR_COLOR)

            UIManager.put("nimbusDisabledText", primaryColor)
            UIManager.put("nimbusSelectedText", secondaryColor)
            UIManager.put("text", secondaryColor)

            UIManager.put("nimbusFocus", TITLE_BAR_COLOR)
            UIManager.put("nimbusInfoBlue", POPUP_BACKGROUND)
            UIManager.put("nimbusLightBackground", WIDGET_COLOR)
            UIManager.put("nimbusSelectionBackground", PROGRESS_BAR_FILL)

            UIManager.put("Button.background", WIDGET_COLOR)
            UIManager.put("Button.foreground", secondaryColor)

            UIManager.put("CheckBox.background", VIEW_BACKGROUND_COLOR)
            UIManager.put("CheckBox.foreground", secondaryColor)
            UIManager.put("CheckBox.icon", UIManager.getIcon("CheckBox.icon"))

            UIManager.put("ComboBox.background", WIDGET_COLOR)
            UIManager.put("ComboBox.foreground", secondaryColor)
            UIManager.put("ComboBox.selectionBackground", PROGRESS_BAR_FILL)
            UIManager.put("ComboBox.selectionForeground", primaryColor)
            UIManager.put("ComboBox.buttonBackground", WIDGET_COLOR)

            UIManager.put("Spinner.background", WIDGET_COLOR)
            UIManager.put("Spinner.foreground", secondaryColor)
            UIManager.put("Spinner.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("TextField.background", WIDGET_COLOR)
            UIManager.put("TextField.foreground", secondaryColor)
            UIManager.put("TextField.caretForeground", secondaryColor)
            UIManager.put("TextField.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("ScrollBar.thumb", WIDGET_COLOR)
            UIManager.put("ScrollBar.track", VIEW_BACKGROUND_COLOR)
            UIManager.put("ScrollBar.thumbHighlight", TITLE_BAR_COLOR)

            UIManager.put("ProgressBar.foreground", PROGRESS_BAR_FILL)
            UIManager.put("ProgressBar.background", WIDGET_COLOR)
            UIManager.put("ProgressBar.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("ToolTip.background", VIEW_BACKGROUND_COLOR)
            UIManager.put("ToolTip.foreground", secondaryColor)
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            // Update component tree UI to apply the new theme
            SwingUtilities.updateComponentTreeUI(frame)
            frame.background = Color.BLACK
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFont(): Font? {
        val fontStream = Helpers.openResource("res/runescape_small.ttf")
        return if (fontStream != null) {
            try {
                val font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
                val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                ge.registerFont(font) // Register the font in the graphics environment
                font
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            println("Font not found!")
            null
        }
    }

    object StateManager {
        var focusedView: String = ""
    }

    private fun applyTheme(theme: Theme) {
        WIDGET_COLOR = theme.widgetColor
        TITLE_BAR_COLOR = theme.titleBarColor
        VIEW_BACKGROUND_COLOR = theme.viewBackgroundColor
        primaryColor = theme.primaryColor
        secondaryColor = theme.secondaryColor
        POPUP_BACKGROUND = theme.popupBackground
        POPUP_FOREGROUND = theme.popupForeground
        TOOLTIP_BACKGROUND = theme.tooltipBackground
        SCROLL_BAR_COLOR = theme.scrollBarColor
        PROGRESS_BAR_FILL = theme.progressBarFill
        NAV_TINT = theme.navTint
        NAV_GREYSCALE = theme.navGreyScale
        BOOST = theme.boost
    }
}
