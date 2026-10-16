package BankTagLayouts

import BiggerBank.plugin as BiggerBankPlugin
import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import rt4.Component
import rt4.ComponentPointer
import rt4.GameShell
import rt4.InterfaceList
import rt4.Inv
import rt4.ObjTypeList
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import rt4.Keyboard
import rt4.Mouse
import rt4.Sprite

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

        // Standard RS2-lineage container ids (BANK_INV_ID=95, already
        // confirmed correct in this project) - INVENTORY/EQUIPMENT=93/94
        // immediately precede it in that well-known sequence, shared across
        // most forks from this era. Not independently verified yet; the
        // "Save current gear" action reports how many items it actually
        // found so a wrong id is obvious immediately on first use.
        const val INVENTORY_INV_ID = 93
        const val EQUIPMENT_INV_ID = 94
        const val INVENTORY_SLOTS = 28
        const val EQUIPMENT_SLOTS = 14

        const val HOST_IFACE = 746
        const val HOST_COMPONENT_IDX = 6

        // Proportions from RuneLite's real TabInterface.java (TAB_WIDTH=39,
        // TAB_HEIGHT=40, MARGIN=1) - TAB_SIZE stayed at 48 (matching the
        // bank's native tab-button size) even after dropping the native
        // frame sprite as the background, since that size read fine on its
        // own. MARGIN is widened well past RuneLite's own 1px, since -
        // unlike RuneLite - our tag names are drawn permanently above each
        // tile rather than only in a floating hover tooltip, and need room
        // to not collide with the tile above/below.
        const val TAB_SIZE = 48
        const val MARGIN = 18
        const val ARROW_HEIGHT = 16
        // Aligns the "+" button's top edge with the window's own visible
        // top edge, rather than an arbitrary value chosen to roughly match
        // the native tab row's height. Same lesson as LEFT_MARGIN_TOTAL:
        // chrome[0] and chrome[1] both report oy=25 (relative to root,
        // confirmed via ::banktags_tabframes), and root.y is always 0 (the
        // ROOT_RULE never gets a vertical margin the way it gets
        // externalLeftMargin horizontally) - so the window's visible
        // content simply starts at host-relative y=25, unconditionally.
        const val TOP_MARGIN = 25
        // Button x/width never depend on this constant - a button's own
        // absolute right edge is always host.x + 2 + TAB_SIZE, fixed. This
        // constant only controls where the WINDOW's root (component 61)
        // gets positioned (root.x = this value, via BiggerBank's
        // externalLeftMargin), and the window's actual visible content sits
        // a further FIXED 14px inside root's own origin regardless of this
        // value (chrome[1]'s ox=14, confirmed live via ::banktags_margin) -
        // so INCREASING this constant pushes the window further AWAY from
        // the button, not closer. That's backwards from what an earlier
        // attempt assumed (raising it from +2 to +14 measurably widened the
        // gap, confirmed via ::banktags_margin showing 12px -> 29px).
        // Solving host.x+2+TAB_SIZE = host.x+LEFT_MARGIN_TOTAL+14 for
        // LEFT_MARGIN_TOTAL gives TAB_SIZE-12.
        const val LEFT_MARGIN_TOTAL = TAB_SIZE - 12

        // The bank's own native tab-frame sprite (1421) had corner rounding
        // shaped for tabs sitting edge-to-edge in a HORIZONTAL row (each
        // corner rounded differently depending on which side has a
        // neighbor), which didn't translate cleanly to our vertical column -
        // one corner read as a clean square (the one with no neighbor in the
        // original layout) while the other three looked built to connect to
        // tabs that aren't there. ::banktags_exportsprite <id> writes a
        // sprite's live pixel data out as a real PNG (SoftwareSprite/
        // GlSprite both expose a public pixels: IntArray) so it could be
        // edited in an image editor - customTabSprite loads the result back
        // via API.GetSpriteFromPNG() and draws it ourselves in ComponentDraw,
        // independent of the game's own cached asset (1421 itself is
        // untouched - this never affects anything else that uses it).
        const val TAB_BG = 0x4A3826
        const val TAB_BORDER = 0x241C12
        const val TAB_HOVER_BG = 0x5C4630
        const val TAB_ACTIVE_BG = 0x6B4E2E
        const val TAB_ACTIVE_BORDER = 0xFFB800

        // Loadout tags (populated via "Save current gear") render items at
        // fixed positions matching the real equipment/inventory screens,
        // instead of BiggerBank's default packed grid - equipment on the
        // left, inventory (4 cols x 7 rows, standard OSRS-era layout) on the
        // right. Ghosts for anything not currently in the bank (real
        // placeholders don't exist server-side - see drawMissingItemGhosts)
        // render at that same fixed position rather than a separate strip.
        const val SLOT_PX = 44
        const val LOADOUT_TOP = 5
        const val LOADOUT_EQUIP_LEFT = 8
        const val EQUIP_COLS = 3
        const val INV_COLS = 4
        const val LOADOUT_INV_LEFT = LOADOUT_EQUIP_LEFT + EQUIP_COLS * SLOT_PX + 24
        const val GHOST_ALPHA = 160

        // 2009scape's actual equipment screen layout (slot id -> col,row) -
        // more compact than modern OSRS's: gloves/legs/ring share one row
        // directly under weapon/body/shield, and boots sit alone in the row
        // below, centered under legs (confirmed against a screenshot of the
        // real equipment tab). Slots 6/8/11 are unused (arms/offhand-legacy/
        // legacy) and deliberately absent.
        val EQUIP_SLOT_GRID: Map<Int, Pair<Int, Int>> = mapOf(
                0 to (1 to 0),  // head
                1 to (0 to 1),  // cape
                2 to (1 to 1),  // neck
                13 to (2 to 1), // ammo
                3 to (0 to 2),  // weapon
                4 to (1 to 2),  // body
                5 to (2 to 2),  // shield
                9 to (0 to 3),  // hands (gloves) - directly under weapon
                7 to (1 to 3),  // legs
                12 to (2 to 3), // ring - directly under shield
                10 to (1 to 4)  // feet (boots) - directly under legs
        )

        var instance: plugin? = null
    }

    private val tags = LinkedHashMap<String, MutableSet<Int>>()
    // Populated only for tags saved via "Save current gear" - slot index (the
    // real inventory/equipment slot at capture time) -> item id. A tag with
    // no entry here behaves exactly as before (packed-grid filter view).
    private val loadoutInventory = LinkedHashMap<String, LinkedHashMap<Int, Int>>()
    private val loadoutEquipment = LinkedHashMap<String, LinkedHashMap<Int, Int>>()
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

    // PluginRepository.Draw() (Cs1ScriptRunner, clientCode==1405) fires once
    // per RENDERED FRAME on the game's own render thread. Our click/scroll
    // listeners are raw AWT MouseListeners firing on the EDT, completely
    // unsynchronized with that thread. rebuildButtons() used to run on every
    // single Draw() call, building brand-new Component() objects each time -
    // and a freshly-constructed Component starts with mouseOver=false. If a
    // click landed between this frame's rebuild swapping in new objects and
    // that same frame's native hover pass (InterfaceList.processComponents)
    // getting a chance to set mouseOver=true on THOSE objects, the click
    // handler saw mouseOver=false on a button the player was genuinely
    // hovering and silently did nothing - a pure timing race, hence
    // "sometimes it works, sometimes it doesn't." Rebuilding only when the
    // actual button set changes lets the same Component instances persist
    // across many frames, so their mouseOver stays correctly maintained.
    private var lastSignature: List<Any>? = null

    // host (component 6 of interface 746) is the MAIN GAME INTERFACE's
    // permanent modal-docking slot, reused for the bank, shops, trade, etc.
    // - it is NOT specific to the bank and is always present regardless of
    // whether the bank is open. Whatever children it has when something
    // OTHER than the bank is docked there (or nothing is) must be preserved,
    // not overwritten - captured once per bank-open, restored on close.
    private var nativeHostChildren: Array<Component?>? = null
    private var trackingNativeChildren = false

    // Loaded once at startup from whatever's saved at this path - the same
    // file ::banktags_exportsprite 1421 wrote out and the user edited by
    // hand (square corners instead of the native sprite's horizontal-row
    // shaping). Absent entirely if the file was never created/edited, in
    // which case buttons just fall back to their plain TAB_BG rect with no
    // art on top - see ComponentDraw.
    private var customTabSprite: Sprite? = null

    private fun loadCustomTabSprite() {
        try {
            val file = java.io.File("banktags_sprite_1421.png")
            if (!file.exists()) return
            val image = javax.imageio.ImageIO.read(file) ?: return
            customTabSprite = API.GetSpriteFromPNG(image)
        } catch (e: Exception) {
            // Best-effort - falls back to no custom art if this fails.
        }
    }

    // The bank window's own background texture (component[1] of interface
    // 762, confirmed via ::banktags_tabframes) - reused as-is (no edit
    // needed, unlike the tab frame's corner-shape problem, since a plain
    // texture fill has no corners to clash with a vertical layout) as a
    // small backing plate behind each tag's name label, stretched to fit.
    //
    // Loaded lazily (see ensureLabelBgSpriteLoaded, called from Draw())
    // rather than in Init() - API.GetSprite() reads client.js5Archive8,
    // which is still null at plugin-load time (that only gets set up later,
    // during actual game/login startup), so calling it from Init() crashed
    // the whole plugin with an NPE on launch. Reading our own local PNG
    // file for customTabSprite doesn't have this problem since it doesn't
    // depend on any game state being ready yet.
    private var labelBgSprite: Sprite? = null

    private fun ensureLabelBgSpriteLoaded() {
        if (labelBgSprite != null) return
        try {
            labelBgSprite = API.GetSprite(297)
        } catch (e: Exception) {
            // Not ready yet - retried next frame, harmless either way.
        }
    }

    override fun Init() {
        instance = this
        loadTags()
        loadLoadouts()
        loadCustomTabSprite()
        auditLog("Init: loaded tags=${tags.keys}")
        API.AddMouseListener(ClickHandler)
        API.AddMouseWheelListener(ScrollHandler)
        API.AddKeyboardListener(TextInputHandler)
        // Direct callback instead of a Draw()/LateDraw() timing trick - both
        // plugins' Draw() fire from the same render-tree checkpoint every
        // single frame forever, so there's no "settle" point a timing-based
        // approach can rely on; BiggerBank calls this the instant it
        // finishes setting its own default positions, guaranteeing order.
        BiggerBankPlugin.afterReflow = { container -> repositionLoadoutItems(container) }
        BiggerBankPlugin.suppressTabBoundaries = { activeTag != null }
    }

    // Temporary audit trail for the "tags disappear" report - appends
    // (never overwrites) a timestamped line for every tag-data mutation and
    // every load/save, so if it happens again the exact sequence of events
    // leading up to an empty tags map is visible, rather than just an
    // after-the-fact snapshot of the end state.
    private fun auditLog(message: String) {
        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_audit.log", true)).use {
                it.println("[${System.currentTimeMillis()}] $message")
            }
        } catch (e: Exception) {
            // best-effort only
        }
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
        auditLog("saveTags: tags=${tags.keys}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadLoadouts() {
        val savedInv = API.GetData("banktaglayouts-loadout-inv") as? LinkedHashMap<String, LinkedHashMap<Int, Int>>
        if (savedInv != null) loadoutInventory.putAll(savedInv)
        val savedEquip = API.GetData("banktaglayouts-loadout-equip") as? LinkedHashMap<String, LinkedHashMap<Int, Int>>
        if (savedEquip != null) loadoutEquipment.putAll(savedEquip)
    }

    private fun saveLoadouts() {
        API.StoreData("banktaglayouts-loadout-inv", LinkedHashMap(loadoutInventory))
        API.StoreData("banktaglayouts-loadout-equip", LinkedHashMap(loadoutEquipment))
    }

    // ─── Tagging via right-click ────────────────────────────────────────────

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        currentEntries ?: return
        val components = InterfaceList.components[BANK_IFACE]
        val container = components?.getOrNull(ITEM_CONTAINER_IDX)
        if (container != null) {
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

        // Real "Choose Option" native menu for a hovered tag button, instead
        // of a custom-drawn one - the same InsertMiniMenuEntry mechanism as
        // "Tag" above.
        //
        // Root cause of "tags keep disappearing": our tag button has no
        // native ops[] of its own (component.ops was never populated -
        // these menu rows only ever came from InsertMiniMenuEntry), so
        // nothing was competing for whichever slot a plain left-click
        // resolves to by default. Delete ended up occupying it, so every
        // ordinary left-click meant to just toggle the tag filter was ALSO
        // silently deleting the tag. Inserting this "View"/"Close" entry
        // FIRST claims that slot with the action a left-click should
        // actually perform. The redundant toggle logic that used to live in
        // ClickHandler for Tag buttons is removed - handling the toggle in
        // both places at once would double-fire on every click (toggle on,
        // then immediately back off) since AWT delivers the same click
        // event to our listener and to native processing independently.
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX)
        val hoveredTag = host?.let { buttonAt(it, Mouse.lastMouseX, Mouse.lastMouseY) }?.value as? ButtonInfo.Tag
        if (hoveredTag != null) {
            val name = hoveredTag.name
            val selectLabel = if (activeTag == name) "Close" else "View"
            API.InsertMiniMenuEntry(selectLabel, name) {
                activeTag = if (activeTag == name) null else name
            }
            API.InsertMiniMenuEntry("Delete", name) { deleteTagFlow(name) }
            API.InsertMiniMenuEntry("Rename", name) { renameTagFlow(name) }
            API.InsertMiniMenuEntry("Save current gear", name) { saveCurrentGearFlow(name) }
        }
    }

    private fun promptTag(itemId: Int) {
        val existing = tags.entries.filter { itemId in it.value }.map { it.key }
        val title = if (existing.isEmpty())
            "Enter tag name:"
        else
            "Enter tag name (already tagged: ${existing.joinToString(", ")}):"
        TextPrompt.open(title) { name ->
            val trimmed = name.trim()
            if (trimmed.isNotEmpty()) {
                tags.getOrPut(trimmed) { mutableSetOf() }.add(itemId)
                saveTags()
                API.SendMessage("Tagged as \"$trimmed\"")
            }
        }
    }

    private fun promptNewTag() {
        TextPrompt.open("New tag name:") { name ->
            val trimmed = name.trim()
            if (trimmed.isNotEmpty() && !tags.containsKey(trimmed)) {
                tags[trimmed] = mutableSetOf()
                saveTags()
                activeTag = trimmed
            }
        }
    }

    // ─── Gear loadouts (snapshot equipment+inventory, ghost placeholders) ───

    // Captures the REAL slot index each item currently occupies, not just a
    // flat set of item ids - that's what lets the loadout view later place
    // each item back at the position matching the actual inventory/equipment
    // screens.
    private fun captureCurrentGearPositions(): Pair<LinkedHashMap<Int, Int>, LinkedHashMap<Int, Int>> {
        val inv = LinkedHashMap<Int, Int>()
        for (slot in 0 until INVENTORY_SLOTS) {
            val id = Inv.getItemType(INVENTORY_INV_ID, slot)
            if (id > 0) inv[slot] = id
        }
        val equip = LinkedHashMap<Int, Int>()
        for (slot in 0 until EQUIPMENT_SLOTS) {
            val id = Inv.getItemType(EQUIPMENT_INV_ID, slot)
            if (id > 0) equip[slot] = id
        }
        return inv to equip
    }

    private fun scanBankItemIds(): Set<Int> {
        val ids = mutableSetOf<Int>()
        for (slot in 0 until MAX_BANK_SLOTS) {
            val id = Inv.getItemType(BANK_INV_ID, slot)
            if (id > 0) ids.add(id)
        }
        return ids
    }

    // ─── Building real components each frame ────────────────────────────────

    private fun isBankDockedAt(host: Component): Boolean {
        val ptr = InterfaceList.openInterfaces.get(host.id.toLong()) as? ComponentPointer
        return ptr != null && ptr.interfaceId == BANK_IFACE
    }

    override fun Draw(timeDelta: Long) {
        labelsDrawnThisFrame = false
        ensureLabelBgSpriteLoaded()
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return

        if (!isBankDockedAt(host)) {
            // Bank isn't open (or something else is docked at this shared
            // slot right now) - restore whatever was really there and stop
            // touching it until the bank opens again.
            if (trackingNativeChildren) {
                host.createdComponents = nativeHostChildren
                trackingNativeChildren = false
                nativeHostChildren = null
                liveButtons = emptyMap()
                lastSignature = null
            }
            BiggerBankPlugin.externalLeftMargin = 0
            return
        }

        BiggerBankPlugin.externalLeftMargin = LEFT_MARGIN_TOTAL

        if (!trackingNativeChildren) {
            // First frame the bank is open this session - whatever is here
            // right now belongs to the bank's own docking, not us. Remember
            // it so we can add to it instead of replacing it.
            nativeHostChildren = host.createdComponents
            trackingNativeChildren = true
        }

        val bankComponents = InterfaceList.components[BANK_IFACE] ?: return
        val container = bankComponents.getOrNull(ITEM_CONTAINER_IDX) ?: return

        // Only rebuild when the actual button set needs to change - see the
        // comment on lastSignature for why rebuilding unconditionally every
        // frame caused clicks to intermittently miss. activeTag isn't part
        // of this signature - which button reads as "selected" is decided
        // fresh every frame directly in ComponentDraw, not baked into the
        // component itself, so switching tags needs no rebuild at all.
        val signature = listOf(tags.keys.toList(), scrollOffset, host.height)
        if (signature != lastSignature) {
            rebuildButtons(host)
            lastSignature = signature
        }
        applyFilter(container)
    }

    // client.java calls GlRenderer.swapBuffers() BEFORE PluginRepository.
    // LateDraw() when running in GL/HD mode (confirmed live via
    // GlRenderer.enabled=true in this session) - so anything LateDraw draws
    // is issued into what's now the NEXT frame's back buffer, which gets
    // fully overwritten by that next frame's own render pass before its own
    // swap turn ever comes. It never reaches the displayed buffer at all -
    // not covered, just silently discarded every single frame. That's why
    // the text vanished entirely rather than appearing behind anything.
    //
    // Draw() (the clientCode==1405 hook shared with the FPS/debug-HUD
    // counter) runs earlier and does reach the screen, but still BEFORE the
    // bank window's own docked interface composites on top - same ordering
    // problem ComponentDraw had.
    //
    // The fix: piggyback on ANY OTHER component belonging to interface 746
    // (the main game interface host lives in) whose index is greater than
    // HOST_COMPONENT_IDX. Cs1ScriptRunner.renderComponent() processes a
    // component array in ascending index order, so by the time such a
    // component's own ComponentDraw fires, host's entire subtree - our tag
    // buttons AND the bank window docked at it - has already drawn this
    // frame. Interface 746 (chat, inventory tabs, orbs, etc.) always has
    // several such components active, so this fires reliably every frame
    // regardless of what's open. Crucially this still happens before
    // GlRenderer.swapBuffers(), unlike LateDraw.
    private var labelsDrawnThisFrame = false

    private fun maybeDrawTagLabelsAfter(componentIndex: Int, component: Component) {
        if (labelsDrawnThisFrame) return
        if ((component.id ushr 16) != HOST_IFACE || componentIndex <= HOST_COMPONENT_IDX) return
        labelsDrawnThisFrame = true
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX) ?: return
        if (!isBankDockedAt(host)) return
        drawTagLabels(host)
    }

    // Permanent name label above each tag tile, centered on it, instead of
    // only showing on hover - widened MARGIN above leaves room for these
    // without the tiles themselves needing to move. Each label gets its own
    // backing plate (the window's own background texture, stretched to fit)
    // filling exactly the MARGIN-tall gap directly above that tile - drawn
    // per-tag here rather than as one strip down the whole column, so it
    // appears/disappears together with whichever tag it belongs to as tags
    // are added/removed, instead of being a fixed feature of the column
    // itself.
    private fun drawTagLabels(host: Component) {
        val tagButtons = liveButtons.entries.filter { it.value is ButtonInfo.Tag }
        if (tagButtons.isNotEmpty()) {
            // The raster clip is a static field left over from whatever was
            // drawn last in the frame - nothing resets it to full-screen
            // automatically, so without this the text was being silently
            // clipped to whatever tiny region that leftover state left
            // active.
            API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
            for ((component, info) in tagButtons) {
                info as ButtonInfo.Tag
                val absX = host.x + component.x
                val absY = host.y + component.y
                labelBgSprite?.renderResized(absX, absY - MARGIN, TAB_SIZE, MARGIN)
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, info.name, absX + TAB_SIZE / 2, absY - 4)
            }
            drawMissingItemGhosts(host)
        }
        // Not gated on tagButtons - naming your very first tag happens
        // while there are none yet.
        drawTextPromptIfActive()
    }

    // Real bank placeholders (an item staying in its slot at 0 count after a
    // full withdrawal) don't exist on 2009scape's server - confirmed via
    // researching the server source: Container.shift() and
    // BankContainer.takeItem() unconditionally compact the bank on a full
    // withdrawal, with no PLACEHOLDER varbit or setting anywhere. Since we
    // also play on the official live server (not a self-hosted one), that's
    // not something addable from this project even if we wanted to patch
    // the server. Faking it using real bank slot positions doesn't work
    // either - ANY full withdrawal anywhere in the bank re-triggers that same
    // compaction and renumbers every slot after it, so a client-only
    // "remembered slot index" would desync constantly, not just for tagged
    // items.
    //
    // Instead: for a loadout tag, whatever items aren't anywhere in the bank
    // right now (because they're equipped/held) get drawn as a faint "ghost"
    // icon with a "0" AT THE SAME fixed equipment/inventory-screen position
    // real items in this loadout use (see repositionLoadoutItems) - a
    // plugin-only overlay independent of real bank slots, so it can never
    // desync from the server's actual view of the bank. Reuses the same
    // piggyback timing as drawTagLabels (called from there) so this draws
    // after the bank window itself, not underneath it.
    private fun drawMissingItemGhosts(host: Component) {
        val tag = activeTag ?: return
        val invMap = loadoutInventory[tag]
        val equipMap = loadoutEquipment[tag]
        if (invMap == null && equipMap == null) return
        val (containerX, containerY) = containerAbsolutePosition(host) ?: return
        val owned = scanBankItemIds()

        API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)

        equipMap?.forEach { (slot, itemId) ->
            if (itemId in owned) return@forEach
            val (col, row) = EQUIP_SLOT_GRID[slot] ?: return@forEach
            drawGhost(containerX + LOADOUT_EQUIP_LEFT + col * SLOT_PX, containerY + LOADOUT_TOP + row * SLOT_PX, itemId)
        }
        invMap?.forEach { (slot, itemId) ->
            if (itemId in owned) return@forEach
            val col = slot % INV_COLS
            val row = slot / INV_COLS
            drawGhost(containerX + LOADOUT_INV_LEFT + col * SLOT_PX, containerY + LOADOUT_TOP + row * SLOT_PX, itemId)
        }
    }

    private fun drawGhost(x: Int, y: Int, itemId: Int) {
        API.GetObjSprite(itemId, 1, false, 0, 0)?.renderAlpha(x, y, GHOST_ALPHA)
        API.DrawText(FontType.SMALL, GHOST_TEXT_COLOR, TextModifier.LEFT, "0", x, y + 30)
    }

    private val GHOST_TEXT_COLOR = FontColor.fromColor(java.awt.Color.LIGHT_GRAY)

    // Item container (interface 762, component ITEM_CONTAINER_IDX) is nested
    // TWO levels under host - host -> component 61 (BiggerBank's
    // ROOT_COMPONENT_IDX, the bank interface's own root) -> the item
    // container - so its true absolute screen position needs both offsets,
    // not just host's own (confirmed via ::dumpinterface 762: the container
    // reports overlayer=<component 61's id>, meaning its x/y are relative to
    // component 61, which is itself relative to host).
    private fun containerAbsolutePosition(host: Component): Pair<Int, Int>? {
        val bankComponents = InterfaceList.components?.getOrNull(BANK_IFACE) ?: return null
        val root = bankComponents.getOrNull(BiggerBankPlugin.ROOT_COMPONENT_IDX) ?: return null
        val container = bankComponents.getOrNull(ITEM_CONTAINER_IDX) ?: return null
        val x = host.x + root.x + container.x - container.scrollX
        val y = host.y + root.y + container.y - container.scrollY
        return x to y
    }

    // A bank stack is genuinely one slot server-side (e.g. one slot holds
    // all 90 Monkfish) even though the snapshot captured it from 10 separate
    // inventory slots. To show 10 clickable stacks - each reflecting the
    // real, live remaining count, and each independently able to withdraw
    // from the SAME real slot - one real component gets moved into the
    // first inventory position; the other 9 positions get CLONES of it.
    //
    // This works because native click-menu generation (MiniMenu.
    // addComponentEntries, the if3 slot-options branch) resolves which bank
    // slot a click applies to via component.createdComponentId (and
    // verifies component.id matches the container) - both copied faithfully
    // here - not from the component's screen position or object identity.
    // That's also already relied on elsewhere: OnMiniMenuCreate reads
    // entry.intArg1 as the bank slot directly from that same field, and
    // BiggerBank has been freely repositioning these components for the
    // entire time this plugin's been in development without ever breaking
    // withdraw - both are strong, already-proven evidence this field (not
    // position or instance identity) is what the native system keys on.
    // getServerActiveProperties() (which the click-menu's option text/
    // enablement checks go through) looks itself up via a hashtable keyed
    // on (id, createdComponentId) - copying just those two fields is enough
    // for it to resolve the SAME entry for a clone as for the original.
    //
    // Refreshing every frame directly from the real component's CURRENT
    // objCount is also what makes the clones' displayed numbers - and the
    // real slot's remaining amount after any one of them is used to
    // withdraw - stay live and in sync automatically, with no bookkeeping
    // of our own.
    //
    // The clone OBJECTS themselves must persist across frames, though (one
    // per loadout position, in cloneCache) - only their fields get
    // refreshed. A left-click on a bank item doesn't fire its menu option
    // immediately: InterfaceList.processComponents starts a native drag on
    // that exact Component instance, and Cs1ScriptRunner.updateComponentDrag
    // only fires the click (MiniMenu.processClick) on mouse release if it
    // still finds that same instance in the tree (dragSourceFound). Brand-new
    // clones every frame meant the drag source vanished one frame later, the
    // drag was silently dropped, and only the real (persistent) first copy
    // could ever withdraw.
    private var injectedClones: MutableSet<Component> = mutableSetOf()
    private val cloneCache = HashMap<Pair<Boolean, Int>, Component>()

    private fun cloneItemComponent(source: Component, c: Component, x: Int, y: Int): Component {
        c.id = source.id
        c.createdComponentId = source.createdComponentId
        c.type = source.type
        c.overlayer = source.overlayer
        c.width = source.width
        c.height = source.height
        c.hidden = false
        c.noClickThrough = source.noClickThrough
        c.if3 = source.if3
        c.hasEventHandlers = source.hasEventHandlers
        c.spriteId = source.spriteId
        c.objId = source.objId
        c.objCount = source.objCount
        c.objDrawText = source.objDrawText
        c.outlineThickness = source.outlineThickness
        c.shadowColor = source.shadowColor
        c.ops = source.ops
        c.optionBase = source.optionBase
        // Same drag behaviour as the real item - without the script-set
        // deadzone/deadtime (default 0) the slightest mouse movement during
        // a click turns it into a drag instead of a withdraw.
        c.dragDeadzone = source.dragDeadzone
        c.dragDeadtime = source.dragDeadtime
        c.dragRenderBehavior = source.dragRenderBehavior
        c.dragParent = source.dragParent
        c.dragTargets = source.dragTargets
        c.onDragStart = source.onDragStart
        c.onDrag = source.onDrag
        c.onDragRelease = source.onDragRelease
        c.x = x
        c.y = y
        return c
    }

    // BiggerBank's "Tab N" header labels and per-tab indent graphics are
    // positioned purely from cumulative real item counts across the whole
    // bank (server-tracked tab-size varbits) - they have no concept of our
    // tag filtering at all, so they leak into ANY active custom tag's view,
    // not just loadout ones. Most visible on a brand new, still-empty tag
    // (no loadout data yet, and no items either) since there's nothing else
    // in the view to draw attention away from a stray indent box and "Tab N"
    // label - originally this only checked for loadout data specifically,
    // which is why an empty freshly-created tag still showed them. Their
    // hidden state is otherwise native/server-controlled (BiggerBank only
    // ever reads it, never sets it), so - same lesson as applyFilter's
    // lastHiddenByUs - the ORIGINAL value has to be saved and restored on
    // deactivation rather than force-set back to a guess, or this would
    // trade "headers leak into the tag view" for "headers vanish forever
    // after leaving it".
    private var chromeHiddenForTag = false
    private val savedChromeHidden = HashMap<Component, Boolean>()

    private fun setChromeVisibilityForTag(active: Boolean) {
        val bankComponents = InterfaceList.components?.getOrNull(BANK_IFACE) ?: return
        val indices = BiggerBankPlugin.HEADER_INDICES + BiggerBankPlugin.INDENT_INDICES
        if (active) {
            if (!chromeHiddenForTag) {
                savedChromeHidden.clear()
                for (idx in indices) {
                    val c = bankComponents.getOrNull(idx) ?: continue
                    savedChromeHidden[c] = c.hidden
                }
                chromeHiddenForTag = true
            }
            for (idx in indices) bankComponents.getOrNull(idx)?.hidden = true
        } else if (chromeHiddenForTag) {
            for ((c, wasHidden) in savedChromeHidden) c.hidden = wasHidden
            savedChromeHidden.clear()
            chromeHiddenForTag = false
        }
    }

    // Called directly from BiggerBank via afterReflow, the instant it
    // finishes setting its own default packed-grid positions on every
    // visible item this frame - see the comment on afterReflow for why a
    // Draw()/LateDraw() timing approach can't work here (both plugins'
    // Draw() fire from the same render-tree checkpoint every single frame
    // forever, so there's no settle point to land on).
    private fun repositionLoadoutItems(container: Component) {
        val tag = activeTag
        val invMap = tag?.let { loadoutInventory[it] }
        val equipMap = tag?.let { loadoutEquipment[it] }
        val isLoadout = invMap != null || equipMap != null
        // Any active tag needs the native chrome hidden, not just loadout
        // ones - the reposition/clone logic below this point IS still
        // loadout-specific and correctly skipped for a plain tag.
        setChromeVisibilityForTag(tag != null)
        if (!isLoadout) {
            // Not a loadout tag right now - drop any clones left over from
            // a previously-viewed loadout tag instead of leaving them stuck
            // in the array forever.
            if (injectedClones.isNotEmpty()) {
                container.createdComponents = container.createdComponents
                        ?.filter { it == null || it !in injectedClones }?.toTypedArray()
                injectedClones = mutableSetOf()
            }
            cloneCache.clear()
            return
        }

        // Drop last frame's clones before re-appending this frame's below -
        // otherwise the array grows unbounded and stale ones (a slot that
        // no longer needs duplicating) linger. Nulls are KEPT, not
        // filtered: the native array is indexed by createdComponentId
        // (InterfaceList.getComponent(id, childId) - what a withdraw click
        // resolves through - reads createdComponents[childId] directly), so
        // compacting it would shift every real item after a gap onto the
        // wrong index. Clones only ever go on the END, past every real one.
        val baseChildren = (container.createdComponents ?: emptyArray())
                .filter { it == null || it !in injectedClones }

        // real bank slot (component.createdComponentId) -> its component
        val realBySlot = HashMap<Int, Component>()
        for (child in baseChildren) {
            if (child != null && child.width == 36 && child.height == 32) realBySlot[child.createdComponentId] = child
        }
        // item id -> the one real component currently representing it
        val realByItemId = HashMap<Int, Component>()
        for ((slot, child) in realBySlot) {
            val itemId = Inv.getItemType(BANK_INV_ID, slot)
            if (itemId > 0) realByItemId[itemId] = child
        }

        // item id -> every (isEquipment, slot) position this loadout wants
        // it shown at - almost always one entry, but a stackable item (like
        // 10 inventory slots' worth of the same potion) can want several.
        val targets = HashMap<Int, MutableList<Pair<Boolean, Int>>>()
        equipMap?.forEach { (slot, id) -> targets.getOrPut(id) { mutableListOf() }.add(true to slot) }
        invMap?.forEach { (slot, id) -> targets.getOrPut(id) { mutableListOf() }.add(false to slot) }

        val newClones = mutableListOf<Component>()
        var maxRow = 0
        for ((itemId, positions) in targets) {
            val real = realByItemId[itemId] ?: continue
            if (real.hidden) continue
            for ((index, pos) in positions.withIndex()) {
                val (isEquip, slot) = pos
                val (x, y) = if (isEquip) {
                    val (col, row) = EQUIP_SLOT_GRID[slot] ?: continue
                    maxRow = maxOf(maxRow, row)
                    (LOADOUT_EQUIP_LEFT + col * SLOT_PX) to (LOADOUT_TOP + row * SLOT_PX)
                } else {
                    val col = slot % INV_COLS
                    val row = slot / INV_COLS
                    maxRow = maxOf(maxRow, row)
                    (LOADOUT_INV_LEFT + col * SLOT_PX) to (LOADOUT_TOP + row * SLOT_PX)
                }
                if (index == 0) {
                    real.x = x
                    real.y = y
                } else {
                    newClones += cloneItemComponent(real, cloneCache.getOrPut(pos) { Component() }, x, y)
                }
            }
        }

        injectedClones = newClones.toMutableSet()
        cloneCache.keys.retainAll { key -> newClones.any { cloneCache[key] === it } }
        container.createdComponents = (baseChildren + newClones).toTypedArray()
        container.scrollMaxV = maxOf((maxRow + 1) * SLOT_PX + 10, container.height)
    }

    // Plain type=3 rect for every button now - the visible background is
    // our own custom-edited sprite (see customTabSprite), drawn manually in
    // ComponentDraw, not a native component.spriteId. type=3 always calls
    // ComponentDraw after drawing its rect, so this is purely a click/hover
    // hitbox underneath - whatever color it's filled with is fully covered
    // by the custom sprite drawn on top afterward.
    private fun makeButton(host: Component, x: Int, y: Int, size: Int): Component {
        val c = Component()
        c.id = -1
        c.type = 3
        c.filled = true
        c.color = TAB_BG
        c.overlayer = host.id
        c.x = x
        c.y = y
        c.width = size
        c.height = size
        c.hidden = false
        c.noClickThrough = true
        // InterfaceList's hover/click gate (processComponents, line ~640)
        // requires if3 components to also set hasEventHandlers, or they're
        // skipped from hover/mouseOver processing entirely - without it,
        // component.mouseOver (which ClickHandler/ScrollHandler/ComponentDraw's
        // hover highlight all poll) would never update.
        c.if3 = true
        c.hasEventHandlers = true
        return c
    }

    private fun rebuildButtons(host: Component) {
        val newButtons = LinkedHashMap<Component, ButtonInfo>()
        val children = mutableListOf<Component>()

        var y = TOP_MARGIN
        children += makeButton(host, 2, y, TAB_SIZE).also { newButtons[it] = ButtonInfo.NewTab }
        y += TAB_SIZE + MARGIN

        val bottomLimit = host.height - 48
        val availableForTabs = bottomLimit - y - ARROW_HEIGHT * 2
        val visibleCount = maxOf(1, availableForTabs / (TAB_SIZE + MARGIN))
        val names = tags.keys.toList()
        val needsScroll = names.size > visibleCount

        if (needsScroll) {
            children += makeButton(host, 2, y, TAB_SIZE).also {
                it.height = ARROW_HEIGHT
                newButtons[it] = ButtonInfo.ScrollUp
            }
            y += ARROW_HEIGHT + MARGIN
        }

        val clampedOffset = scrollOffset.coerceIn(0, maxOf(0, names.size - visibleCount))
        scrollOffset = clampedOffset
        for (i in clampedOffset until minOf(names.size, clampedOffset + visibleCount)) {
            val name = names[i]
            children += makeButton(host, 2, y, TAB_SIZE).also { newButtons[it] = ButtonInfo.Tag(name) }
            y += TAB_SIZE + MARGIN
        }

        if (needsScroll) {
            children += makeButton(host, 2, y, TAB_SIZE).also {
                it.height = ARROW_HEIGHT
                newButtons[it] = ButtonInfo.ScrollDown
            }
        }

        // Preserve whatever native children this shared slot actually had
        // (captured when the bank first opened this session) instead of
        // replacing them - that's what destroyed the window's own rendering
        // before (this component isn't bank-specific, other things dock here
        // too, and native content already there was silently wiped out).
        val native = nativeHostChildren?.filterNotNull() ?: emptyList()
        host.createdComponents = (native + children).toTypedArray()
        liveButtons = newButtons
    }

    override fun ComponentDraw(componentIndex: Int, component: Component, screenX: Int, screenY: Int) {
        maybeDrawTagLabelsAfter(componentIndex, component)

        val info = liveButtons[component] ?: return
        val w = component.width
        val h = component.height

        when (info) {
            is ButtonInfo.ScrollUp, is ButtonInfo.ScrollDown -> {
                // No native art for the scroll arrows - a solid (fully
                // opaque, not see-through) fill change on hover instead of
                // a semi-transparent overlay.
                if (component.mouseOver) {
                    API.FillRect(screenX, screenY, w, h, TAB_HOVER_BG, 0)
                }
                API.DrawRect(screenX, screenY, w, h, TAB_BORDER)
            }
            else -> {
                // NewTab/Tag: our own edited sprite (see customTabSprite),
                // drawn manually here rather than through native
                // component.spriteId - the button's plain TAB_BG rect
                // (drawn natively before ComponentDraw fires) shows through
                // underneath only if the sprite failed to load. A gold
                // border on top is the only extra treatment, and only for
                // the active tag - the edited art's own border already
                // covers the default look, and per feedback there's no
                // separate hover fill overlay on these.
                customTabSprite?.renderResized(screenX, screenY, w, h)
                if (info is ButtonInfo.Tag && info.name == activeTag) {
                    API.DrawRect(screenX, screenY, w, h, TAB_ACTIVE_BORDER)
                }
            }
        }

        when (info) {
            is ButtonInfo.NewTab ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "+", screenX + w / 2, screenY + h / 2 + 4)
            is ButtonInfo.ScrollUp ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "^", screenX + w / 2, screenY + h - 3)
            is ButtonInfo.ScrollDown ->
                API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "v", screenX + w / 2, screenY + h - 3)
            is ButtonInfo.Tag -> {
                val iconItem = tags[info.name]?.firstOrNull()
                if (iconItem != null) {
                    // Center the 36x32 item icon within the larger 48x48
                    // native frame.
                    API.GetObjSprite(iconItem, 1, false, 0, 0)?.render(screenX + (w - 36) / 2, screenY + (h - 32) / 2)
                } else {
                    API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.CENTER, "?", screenX + w / 2, screenY + h / 2 + 4)
                }
            }
        }
    }

    private val WHITE = FontColor.fromColor(java.awt.Color.WHITE)

    // Tracks exactly which components WE hid last frame, so clearing/
    // changing our own filter only ever touches those - never items the
    // native single-tab view (Tab 2, Tab 3, ...) hid on its own. An earlier
    // version unconditionally forced hidden=false on everything whenever no
    // tag was active, to fix tags not un-hiding their own items on
    // unfocus - but that also clobbered native per-tab hiding every single
    // frame, which is why every tab but the native "view all" one started
    // showing the entire bank.
    private var lastHiddenByUs: MutableSet<Component> = mutableSetOf()

    private fun applyFilter(container: Component) {
        val children = container.createdComponents?.filterNotNull() ?: return
        val allowed = activeTag?.let { tags[it] }
        val newlyHidden = mutableSetOf<Component>()

        // component.createdComponentId IS the real bank slot index directly
        // (confirmed via MiniMenu.addComponentEntries and already relied on
        // by OnMiniMenuCreate) - reading it directly is both simpler and
        // more robust than the previous approach of walking bank slots
        // sequentially and counting non-empty ones to line up with each
        // child in array order, which would desync the moment
        // repositionLoadoutItems's cloned components (also 36x32, but not
        // corresponding to their own distinct bank slot) are mixed in.
        for (child in children) {
            if (child.width != 36 || child.height != 32) continue
            // Already hidden, and not something we hid ourselves - leave
            // native tab-view hiding alone entirely, don't even inspect it.
            if (child.hidden && child !in lastHiddenByUs) continue

            val itemId = Inv.getItemType(BANK_INV_ID, child.createdComponentId)
            if (itemId <= 0) continue
            val shouldHide = allowed != null && itemId !in allowed
            child.hidden = shouldHide
            if (shouldHide) newlyHidden += child
        }
        lastHiddenByUs = newlyHidden
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (commandStr.equals("::banktags", ignoreCase = true)) {
            API.SendMessage("Tags: " + tags.keys.joinToString(", ").ifEmpty { "(none yet - right-click a bank item and choose Tag)" })
        }
        if (commandStr.equals("::banktags_debug", ignoreCase = true)) {
            dumpGearDebug()
        }
        if (commandStr.equals("::banktags_tabframes", ignoreCase = true)) {
            dumpNativeTabFrames()
        }
        if (commandStr.equals("::banktags_state", ignoreCase = true)) {
            dumpButtonState()
        }
        if (commandStr.equals("::banktags_exportsprite", ignoreCase = true)) {
            val id = args?.getOrNull(0)?.toIntOrNull()
            if (id == null) {
                API.SendMessage("Usage: ::banktags_exportsprite <spriteId>")
            } else {
                exportSpriteToPng(id)
            }
        }
        if (commandStr.equals("::banktags_margin", ignoreCase = true)) {
            dumpMarginDebug()
        }
        if (commandStr.equals("::banktags_dumpitems", ignoreCase = true)) {
            dumpItemContainer()
        }
    }

    // Diagnostic for the "tab 1 items rendering at a tab 8 position, only in
    // the infinity/view-all tab" bug: dumps every 36x32 child of the item
    // container in RAW array order (index, id, createdComponentId, itemId,
    // hidden, x, y), plus a count of how many components share each
    // createdComponentId - to see directly whether the misplaced items are
    // duplicates/clones (same createdComponentId as a real tab-1 item, extra
    // copy lingering in the array) or single components whose own
    // createdComponentId is itself anomalously high despite the item really
    // being in tab 1.
    private fun dumpItemContainer() {
        val bankComponents = InterfaceList.components?.getOrNull(BANK_IFACE)
        val container = bankComponents?.getOrNull(ITEM_CONTAINER_IDX)
        if (container == null) {
            API.SendMessage("Bank item container not loaded")
            return
        }
        val children = container.createdComponents ?: emptyArray()
        val sb = StringBuilder()
        sb.append("activeTag=$activeTag\n")
        sb.append("injectedClones.size=${injectedClones.size}\n")
        sb.append("container.createdComponents.size=${children.size}\n\n")
        val byCcid = HashMap<Int, Int>()
        for (child in children) {
            child ?: continue
            if (child.width != 36 || child.height != 32) continue
            byCcid[child.createdComponentId] = (byCcid[child.createdComponentId] ?: 0) + 1
        }
        sb.append("=== raw array order ===\n")
        for ((i, child) in children.withIndex()) {
            child ?: continue
            if (child.width != 36 || child.height != 32) continue
            val itemId = Inv.getItemType(BANK_INV_ID, child.createdComponentId)
            val isClone = child in injectedClones
            sb.append("[$i] createdComponentId=${child.createdComponentId} itemId=$itemId hidden=${child.hidden}")
                    .append(" x=${child.x} y=${child.y} isClone=$isClone dupCount=${byCcid[child.createdComponentId]}\n")
        }
        sb.append("\n=== createdComponentId values with more than 1 component ===\n")
        for ((ccid, count) in byCcid) {
            if (count > 1) sb.append("createdComponentId=$ccid appears $count times\n")
        }
        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_dumpitems.txt")).use { it.print(sb.toString()) }
            API.SendMessage("Wrote banktags_dumpitems.txt")
        } catch (e: Exception) {
            API.SendMessage("dump failed: ${e.message}")
        }
    }

    // Deep-dive diagnostic for the persistent left-margin gap: dumps the
    // ACTUAL live x position of every component in the chain (host -> root
    // -> chrome), rather than trusting the code's own math about where
    // things should be. Answers two separate questions at once: (1) is our
    // button really landing exactly where LEFT_MARGIN_TOTAL says it should
    // (host.x + LEFT_MARGIN_TOTAL), and (2) does the window's actual visible
    // content (components 0/1, its background/border chrome) start exactly
    // at the root's own origin, or does the root have internal padding of
    // its own before that content begins - which would explain a gap
    // persisting even when our own positioning math is confirmed correct.
    private fun dumpMarginDebug() {
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX)
        val bankComponents = InterfaceList.components?.getOrNull(BANK_IFACE)
        val root = bankComponents?.getOrNull(BiggerBankPlugin.ROOT_COMPONENT_IDX)
        val chrome0 = bankComponents?.getOrNull(0)
        val chrome1 = bankComponents?.getOrNull(1)
        val newTabBtn = liveButtons.entries.firstOrNull { it.value is ButtonInfo.NewTab }?.key

        val sb = StringBuilder()
        sb.append("LEFT_MARGIN_TOTAL constant = $LEFT_MARGIN_TOTAL\n")
        sb.append("BiggerBankPlugin.externalLeftMargin (live) = ${BiggerBankPlugin.externalLeftMargin}\n")
        sb.append("host: x=${host?.x} y=${host?.y} width=${host?.width} height=${host?.height}\n")
        sb.append("root/component61: x=${root?.x} y=${root?.y} width=${root?.width} height=${root?.height}\n")
        sb.append("chrome[0] (background rect): x=${chrome0?.x} y=${chrome0?.y} width=${chrome0?.width}\n")
        sb.append("chrome[1] (border sprite=297): x=${chrome1?.x} y=${chrome1?.y} width=${chrome1?.width}\n")
        sb.append("NewTab button: x=${newTabBtn?.x} y=${newTabBtn?.y} width=${newTabBtn?.width}\n")

        if (host != null && root != null && newTabBtn != null) {
            val buttonAbsRight = host.x + newTabBtn.x + newTabBtn.width
            val rootAbsLeft = host.x + root.x
            sb.append("\ncomputed:\n")
            sb.append("  button absolute right edge = host.x + button.x + button.width = $buttonAbsRight\n")
            sb.append("  root absolute left edge = host.x + root.x = $rootAbsLeft\n")
            sb.append("  gap between button and root's OWN origin = ${rootAbsLeft - buttonAbsRight}\n")
            if (chrome0 != null) {
                val chrome0AbsLeft = rootAbsLeft + chrome0.x
                sb.append("  chrome[0] absolute left edge = root absolute left + chrome0.x = $chrome0AbsLeft\n")
                sb.append("  gap between button and chrome[0]'s VISIBLE start = ${chrome0AbsLeft - buttonAbsRight}\n")
            }
        }

        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_margin_debug.txt")).use { it.print(sb.toString()) }
            API.SendMessage("Wrote banktags_margin_debug.txt")
        } catch (e: Exception) {
            API.SendMessage("dump failed: ${e.message}")
        }
    }

    // Exports a live sprite's actual pixel data as a real PNG file, so it
    // can be opened/edited in any image editor rather than guessed at from
    // code. SoftwareSprite and GlSprite both expose a public pixels: IntArray
    // (confirmed by reading both classes) alongside width/height declared on
    // the shared Sprite base - the cast just picks whichever concrete type
    // this session's renderer actually produced. Pixel value 0 is treated as
    // fully transparent (the classic "binary transparency" convention these
    // sprites often use); anything else is written through as opaque RGB
    // unless its own top byte already looks like a real alpha channel.
    private fun exportSpriteToPng(spriteId: Int) {
        val sprite = API.GetSprite(spriteId)
        if (sprite == null) {
            API.SendMessage("Sprite $spriteId not found")
            return
        }
        val pixels = (sprite as? rt4.SoftwareSprite)?.pixels ?: (sprite as? rt4.GlSprite)?.pixels
        if (pixels == null) {
            API.SendMessage("Sprite $spriteId: unrecognized sprite type, can't read pixels")
            return
        }
        val w = sprite.width
        val h = sprite.height
        if (w <= 0 || h <= 0 || pixels.size < w * h) {
            API.SendMessage("Sprite $spriteId: bad dimensions (${w}x$h, ${pixels.size} pixels) - not exporting")
            return
        }
        val img = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until h) {
            for (x in 0 until w) {
                val p = pixels[y * w + x]
                val argb = when {
                    p == 0 -> 0
                    (p ushr 24) != 0 -> p
                    else -> (0xFF shl 24) or (p and 0xFFFFFF)
                }
                img.setRGB(x, y, argb)
            }
        }
        val filename = "banktags_sprite_$spriteId.png"
        try {
            javax.imageio.ImageIO.write(img, "png", java.io.File(filename))
            API.SendMessage("Exported sprite $spriteId (${w}x$h) to $filename")
        } catch (e: Exception) {
            API.SendMessage("Export failed: ${e.message}")
        }
    }

    // Temporary diagnostic for the "tag buttons disappear after interacting
    // with them" report - distinguishes actual tag DATA loss (tags map
    // itself empties out) from a rendering/tracking problem (tags data is
    // fine, but liveButtons/host.createdComponents lost track of the
    // buttons that should represent it).
    private fun dumpButtonState() {
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX)
        val sb = StringBuilder()
        sb.append("tags.keys=${tags.keys}\n")
        sb.append("activeTag=$activeTag\n")
        sb.append("liveButtons.size=${liveButtons.size}\n")
        sb.append("liveButtons=${liveButtons.values}\n")
        sb.append("lastSignature=$lastSignature\n")
        sb.append("trackingNativeChildren=$trackingNativeChildren\n")
        sb.append("nativeHostChildren?.size=${nativeHostChildren?.size}\n")
        sb.append("host=$host\n")
        if (host != null) {
            sb.append("isBankDockedAt(host)=${isBankDockedAt(host)}\n")
            sb.append("host.createdComponents?.size=${host.createdComponents?.size}\n")
            val currentSignature = listOf(tags.keys.toList(), scrollOffset, host.height)
            sb.append("currentSignature=$currentSignature\n")
            sb.append("signature matches lastSignature=${currentSignature == lastSignature}\n")
        }
        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_state.txt")).use { it.print(sb.toString()) }
            API.SendMessage("Wrote banktags_state.txt")
        } catch (e: Exception) {
            API.SendMessage("dump failed: ${e.message}")
        }
    }

    // Temporary diagnostic for prototyping reused native tab-button art:
    // dumps interface 762's components across the range the native
    // horizontal tab row is known to live in (confirmed via an earlier
    // ::dumpinterface 762 - frame+icon sprite pairs, mostly hidden, indices
    // roughly 25-42), printing spriteId/activeSpriteId/hidden/pos/size for
    // each so the exact "selected vs not" sprite pair can be identified
    // instead of guessed.
    private fun dumpNativeTabFrames() {
        val components = InterfaceList.components?.getOrNull(BANK_IFACE)
        if (components == null) {
            API.SendMessage("Bank interface not loaded")
            return
        }
        val sb = StringBuilder()
        for (i in 0..45) {
            val c = components.getOrNull(i) ?: continue
            sb.append("[$i] type=${c.type} hidden=${c.hidden} pos=(${c.x},${c.y}) size=(${c.width}x${c.height})")
                    .append(" spriteId=${c.spriteId} activeSpriteId=${c.activeSpriteId} objId=${c.objId}")
                    .append(" overlayer=${c.overlayer}\n")
        }
        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_tabframes.txt")).use { it.print(sb.toString()) }
            API.SendMessage("Wrote banktags_tabframes.txt")
        } catch (e: Exception) {
            API.SendMessage("dump failed: ${e.message}")
        }
    }

    // Temporary diagnostic: (1) raw-scans container ids 88-100 for any
    // non-empty slots, to find the REAL inventory/equipment container ids
    // rather than trusting the 93/94 guess - cross-reference against
    // whatever's visibly in your inventory/equipped; (2) dumps the active
    // tag's captured loadout data as-stored; (3) walks the item container's
    // real children exactly like repositionLoadoutItems does, showing each
    // one's resolved item id, hidden state, and current x/y - to see
    // directly why a match is or isn't happening.
    private fun dumpGearDebug() {
        val sb = StringBuilder()
        sb.append("=== Container scan (ids 88-100, slots 0-29) ===\n")
        for (containerId in 88..100) {
            val hits = mutableListOf<String>()
            for (slot in 0 until 30) {
                val id = Inv.getItemType(containerId, slot)
                if (id > 0) {
                    val name = ObjTypeList.get(id)?.name?.toString() ?: "?"
                    hits.add("slot$slot=$id($name)")
                }
            }
            if (hits.isNotEmpty()) sb.append("container $containerId: ${hits.joinToString(", ")}\n")
        }

        sb.append("\n=== Active tag state ===\n")
        val tag = activeTag
        sb.append("activeTag=$tag\n")
        if (tag != null) {
            sb.append("tags[tag]=${tags[tag]}\n")
            sb.append("loadoutInventory[tag]=${loadoutInventory[tag]}\n")
            sb.append("loadoutEquipment[tag]=${loadoutEquipment[tag]}\n")
        }

        sb.append("\n=== Item container walk ===\n")
        val container = InterfaceList.components?.getOrNull(BANK_IFACE)?.getOrNull(ITEM_CONTAINER_IDX)
        val children = container?.createdComponents
        if (children == null) {
            sb.append("no container/children\n")
        } else {
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
                val name = if (itemId > 0) ObjTypeList.get(itemId)?.name?.toString() ?: "?" else "?"
                sb.append("itemId=$itemId($name) hidden=${child.hidden} pos=(${child.x},${child.y})\n")
            }
        }

        try {
            java.io.PrintWriter(java.io.FileWriter("banktags_gear_debug.txt")).use { it.print(sb.toString()) }
            API.SendMessage("Wrote banktags_gear_debug.txt")
        } catch (e: Exception) {
            API.SendMessage("dump failed: ${e.message}")
        }
    }

    // Actions for a tag button's real native "Choose Option" menu entries
    // (inserted from OnMiniMenuCreate) - RuneLite's own tab column supports
    // rename/remove/icon-change this way; we don't have a custom icon
    // picker UI so this covers rename + delete, plus a "Save current gear"
    // action for building a loadout tag out of whatever's currently worn +
    // in your inventory.
    private fun renameTagFlow(name: String) {
        auditLog("renameTagFlow OPENED for \"$name\"")
        TextPrompt.open("Rename tag:", name) { input ->
            val newName = input.trim()
            auditLog("renameTagFlow CONFIRMED \"$name\" -> \"$newName\"")
            if (newName.isNotEmpty() && newName != name && !tags.containsKey(newName)) {
                tags[newName] = tags.remove(name) ?: mutableSetOf()
                loadoutInventory.remove(name)?.let { loadoutInventory[newName] = it }
                loadoutEquipment.remove(name)?.let { loadoutEquipment[newName] = it }
                if (activeTag == name) activeTag = newName
                saveTags()
                saveLoadouts()
            }
        }
    }

    // Requires re-typing the tag name rather than deleting immediately -
    // the audit log showed tags being deleted within seconds of creation,
    // without deliberate action, on a rapid and repeated basis. This stops
    // the data loss regardless of what's actually triggering it, and the
    // logging around it doubles as the next diagnostic: if "OPENED" shows up
    // in the log without you having chosen Delete, that's confirmation this
    // is an unintended menu trigger rather than something else entirely.
    private fun deleteTagFlow(name: String) {
        auditLog("deleteTagFlow OPENED for \"$name\" - awaiting typed confirmation")
        TextPrompt.open("Type \"$name\" again to delete it:") { input ->
            val trimmed = input.trim()
            if (trimmed == name) {
                auditLog("deleteTagFlow CONFIRMED: deleting \"$name\" (tags before=${tags.keys})")
                tags.remove(name)
                loadoutInventory.remove(name)
                loadoutEquipment.remove(name)
                if (activeTag == name) activeTag = null
                saveTags()
                saveLoadouts()
            } else {
                auditLog("deleteTagFlow CANCELLED: input=\"$trimmed\" did not match \"$name\"")
            }
        }
    }

    private fun saveCurrentGearFlow(name: String) {
        val (inv, equip) = captureCurrentGearPositions()
        val captured = (inv.values + equip.values).toSet()
        val set = tags.getOrPut(name) { mutableSetOf() }
        val added = captured.count { it !in set }
        set.addAll(captured)
        loadoutInventory[name] = inv
        loadoutEquipment[name] = equip
        saveTags()
        saveLoadouts()
        API.SendMessage("\"$name\": saved loadout with ${captured.size} item(s) ($added new)")
    }

    // component.mouseOver is maintained by the native hover pass on the
    // render thread, but our AWT listeners fire on the EDT at whatever
    // instant the OS delivers the event - relying on mouseOver alone left a
    // real (if now much smaller, post-caching) timing gap. Hit-testing
    // against the button's own known screen bounds - host's absolute
    // position (confirmed via API.AddMouseListener attaching to the same
    // GameShell.canvas that feeds Mouse.currentMouseX/Y, so e.x/e.y are in
    // the same coordinate space) plus the button's host-relative x/y - is
    // synchronous with the click itself and has no such window.
    private fun buttonAt(host: Component, screenX: Int, screenY: Int): Map.Entry<Component, ButtonInfo>? {
        for (entry in liveButtons) {
            val c = entry.key
            val absX = host.x + c.x
            val absY = host.y + c.y
            if (screenX in absX until absX + c.width && screenY in absY until absY + c.height) return entry
        }
        return null
    }

    object ClickHandler : MouseAdapter() {
        override fun mousePressed(e: MouseEvent?) {
            e ?: return
            val p = instance ?: return

            val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
            val (component, info) = p.buttonAt(host, e.x, e.y) ?: return
            // Right-click (and anything else besides left) falls through to
            // the native "Choose Option" menu untouched - OnMiniMenuCreate
            // inserts View/Close, Save current gear, Rename, and Delete onto
            // it directly.
            if (e.button != MouseEvent.BUTTON1) return
            when (info) {
                is ButtonInfo.NewTab -> p.promptNewTag()
                is ButtonInfo.ScrollUp -> p.scrollOffset = maxOf(0, p.scrollOffset - 1)
                is ButtonInfo.ScrollDown -> p.scrollOffset += 1
                // Tag buttons deliberately NOT handled here - a plain
                // left-click on one already resolves to the native "View"/
                // "Close" menu entry inserted from OnMiniMenuCreate (see its
                // comment). Toggling here too would double-fire on every
                // click - AWT delivers the same event to this listener and
                // to native click processing independently, so the tag
                // would toggle on then immediately back off.
                is ButtonInfo.Tag -> {}
            }
        }
    }

    object ScrollHandler : MouseWheelListener {
        override fun mouseWheelMoved(e: MouseWheelEvent?) {
            e ?: return
            val p = instance ?: return
            val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
            if (p.buttonAt(host, e.x, e.y) != null) {
                p.scrollOffset = (p.scrollOffset + e.wheelRotation).coerceAtLeast(0)
            }
        }
    }
}

// ─── Shared in-game overlay prompt ──────────────────────────────────────────
// Top-level rather than nested inside the plugin class, so it can call
// refreshInputSuspension() directly without needing an outer-class instance
// reference (Kotlin nested `object`s, unlike `inner class`, have no implicit
// outer reference).

private val OVERLAY_WHITE = FontColor.fromColor(java.awt.Color.WHITE)

private var nativeInputSuspended = false

// AWT delivers a key/mouse event to every listener registered on a
// component regardless of what any other listener does with it - consuming
// the event in our own handler does NOT stop rt4.Keyboard's or rt4.Mouse's
// own listeners (registered separately on the same GameShell.canvas) from
// also seeing it. That's why typing a tag name was also typing into the
// game's chatbox. Keyboard.stop()/start() and Mouse.stop()/start() are the
// native functions for detaching/reattaching those listeners from a
// component - used here to suspend the game's own input handling for
// exactly as long as TextPrompt is open, leaving our own listeners
// (separate objects) untouched. The Rename/Delete/Save current gear choice
// no longer needs this - it now runs through the real native "Choose
// Option" menu (inserted from OnMiniMenuCreate), which suspends nothing on
// its own since it's the game's own input handling already.
private fun refreshInputSuspension() {
    val shouldSuspend = TextPrompt.active
    if (shouldSuspend && !nativeInputSuspended) {
        Keyboard.stop(GameShell.canvas)
        Mouse.stop(GameShell.canvas)
        nativeInputSuspended = true
    } else if (!shouldSuspend && nativeInputSuspended) {
        Keyboard.start(GameShell.canvas)
        Mouse.start(GameShell.canvas)
        nativeInputSuspended = false
    }
}

// In-game text entry, replacing a Swing JOptionPane popup so naming/
// renaming a tag doesn't spawn a separate OS window.
private object TextPrompt {
    var active = false
    var title = ""
    var text = StringBuilder()
    private var onConfirm: ((String) -> Unit)? = null

    fun open(title: String, initial: String = "", onConfirm: (String) -> Unit) {
        this.title = title
        this.text = StringBuilder(initial)
        this.onConfirm = onConfirm
        active = true
        refreshInputSuspension()
    }

    fun cancel() {
        active = false
        onConfirm = null
        refreshInputSuspension()
    }

    fun confirm() {
        if (!active) return
        val result = text.toString()
        val callback = onConfirm
        active = false
        onConfirm = null
        refreshInputSuspension()
        callback?.invoke(result)
    }
}

private object TextInputHandler : KeyAdapter() {
    override fun keyPressed(e: KeyEvent?) {
        e ?: return
        if (!TextPrompt.active) return
        when (e.keyCode) {
            KeyEvent.VK_ENTER -> TextPrompt.confirm()
            KeyEvent.VK_ESCAPE -> TextPrompt.cancel()
            KeyEvent.VK_BACK_SPACE -> if (TextPrompt.text.isNotEmpty()) {
                TextPrompt.text.deleteCharAt(TextPrompt.text.length - 1)
            }
        }
    }

    override fun keyTyped(e: KeyEvent?) {
        e ?: return
        if (!TextPrompt.active) return
        val c = e.keyChar
        // Plain printable ASCII only - excludes backspace/enter/etc, which
        // keyPressed already handles via keyCode.
        if (c.toInt() in 32..126 && TextPrompt.text.length < 40) {
            TextPrompt.text.append(c)
        }
    }
}

private fun drawTextPromptIfActive() {
    if (!TextPrompt.active) return
    API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
    val boxW = 260
    val boxH = 60
    val x = (GameShell.canvasWidth - boxW) / 2
    val y = (GameShell.canvasHeight - boxH) / 2
    API.FillRect(x, y, boxW, boxH, plugin.TAB_BG, 0)
    API.DrawRect(x, y, boxW, boxH, plugin.TAB_ACTIVE_BORDER)
    API.DrawText(FontType.SMALL, OVERLAY_WHITE, TextModifier.CENTER, TextPrompt.title, x + boxW / 2, y + 20)
    val blink = (System.currentTimeMillis() / 500) % 2 == 0L
    val shown = TextPrompt.text.toString() + (if (blink) "|" else "")
    API.DrawText(FontType.SMALL, OVERLAY_WHITE, TextModifier.CENTER, shown, x + boxW / 2, y + 42)
}

