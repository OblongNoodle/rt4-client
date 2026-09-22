package BankTagLayouts

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import rt4.Component
import rt4.InterfaceList
import rt4.Inv
import rt4.Mouse
import rt4.ObjTypeList
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Phase 1 of a RuneLite "Bank Tag Layouts" equivalent: 2009scape has no
 * bank-tags concept at all (that was itself a RuneLite plugin, not a Jagex
 * feature), so this builds tagging from scratch rather than extending
 * something that already exists.
 *
 * Layout proportions (tab size, margin, icon inset, scroll behavior) are
 * modeled directly on RuneLite's actual banktags/tabs/TabInterface.java
 * (TAB_WIDTH=39, TAB_HEIGHT=40, MARGIN=1, scroll arrows when tags overflow
 * available height) - not guessed. Their tab background/active/arrow sprites
 * are custom-bundled plugin artwork (tag-tab.png etc, SpriteOverride ids),
 * which isn't something we have access to, so this reuses 2009scape's own
 * native tab-icon-frame sprite for the button background instead - same
 * spacing/structure, native-to-this-client visuals.
 *
 * This phase covers: right-click "Tag" on any bank item, a vertical
 * scrollable column of tag-tab icons attached to the left edge of the bank
 * window, and filtering the item grid to a selected tag. Deliberately NOT
 * yet included: drag-to-reposition custom layouts, placeholder icons for
 * absent items, custom background images, and the GE/HA value header.
 *
 * Interaction with BiggerBank: filtering just sets hidden=true on
 * non-matching item components. BiggerBank's reflow already skips hidden
 * items and compacts the rest into its current column count, so tag
 * filtering "just works" at whatever size BiggerBank is set to.
 */
@PluginMeta(
        author = "OblongNoodle",
        description = "Bank Tag Layouts equivalent - tag bank items and browse by tag (phase 1: tagging + filtering).",
        version = 1.1
)
class plugin : Plugin() {

    companion object {
        const val BANK_IFACE = 762
        const val ITEM_CONTAINER_IDX = 73
        const val BANK_INV_ID = 95
        const val MAX_BANK_SLOTS = 1000

        // Bank (762) is hosted as a sub-interface inside this component of
        // the main game interface (746) - same one BiggerBank centers on
        // screen (see ::findhost 762). This is the only component whose x/y
        // reflect actual screen position; 762's own components are all local
        // to it.
        const val HOST_IFACE = 746
        const val HOST_COMPONENT_IDX = 6

        // One of the native tab-icon button frames (top row, size 48x48) -
        // reused as our button background so tag icons look native rather
        // than a flat placeholder rectangle.
        const val TAB_BUTTON_FRAME_IDX = 39

        // Proportions from RuneLite's real TabInterface.java, scaled down
        // slightly to sit better against 2009scape's smaller UI scale.
        const val TAB_SIZE = 34
        const val MARGIN = 2
        const val ARROW_HEIGHT = 16
        const val TOP_MARGIN = 40 // clears the bank's title bar area

        var instance: plugin? = null
    }

    // tag name -> item ids tagged with it. LinkedHashMap keeps tab order stable.
    private val tags = LinkedHashMap<String, MutableSet<Int>>()
    private var activeTag: String? = null
    private var scrollOffset = 0

    private sealed class Slot(val x: Int, val y: Int, val w: Int, val h: Int) {
        class NewTab(x: Int, y: Int, size: Int) : Slot(x, y, size, size)
        class ScrollUp(x: Int, y: Int, w: Int, h: Int) : Slot(x, y, w, h)
        class ScrollDown(x: Int, y: Int, w: Int, h: Int) : Slot(x, y, w, h)
        class Tag(x: Int, y: Int, size: Int, val name: String) : Slot(x, y, size, size)
    }

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
            // item container's own component id, and intArg1 as the bank
            // slot index - confirmed via ::dumpmenu, not documented anywhere.
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

    // ─── Shared layout math (used by both drawing and click handling, so ───
    // ─── they can never drift out of sync with each other)              ───

    private fun buildLayout(host: Component, container: Component): List<Slot> {
        val x = host.x - TAB_SIZE
        var y = host.y + TOP_MARGIN
        val slots = mutableListOf<Slot>()

        slots += Slot.NewTab(x, y, TAB_SIZE)
        y += TAB_SIZE + MARGIN

        val bottomLimit = host.y + host.height - 48 // clear the deposit button row
        val availableForTabs = bottomLimit - y - ARROW_HEIGHT * 2
        val visibleCount = maxOf(1, availableForTabs / (TAB_SIZE + MARGIN))
        val names = tags.keys.toList()
        val needsScroll = names.size > visibleCount

        if (needsScroll) {
            slots += Slot.ScrollUp(x, y, TAB_SIZE, ARROW_HEIGHT)
            y += ARROW_HEIGHT + MARGIN
        }

        val clampedOffset = scrollOffset.coerceIn(0, maxOf(0, names.size - visibleCount))
        for (i in clampedOffset until minOf(names.size, clampedOffset + visibleCount)) {
            slots += Slot.Tag(x, y, TAB_SIZE, names[i])
            y += TAB_SIZE + MARGIN
        }

        if (needsScroll) {
            slots += Slot.ScrollDown(x, y, TAB_SIZE, ARROW_HEIGHT)
        }

        return slots
    }

    // ─── Drawing ─────────────────────────────────────────────────────────

    override fun Draw(timeDelta: Long) {
        val components = InterfaceList.components[BANK_IFACE] ?: return
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val frame = components.getOrNull(TAB_BUTTON_FRAME_IDX)

        val slots = buildLayout(host, container)
        for (slot in slots) {
            when (slot) {
                is Slot.NewTab -> {
                    drawFrame(frame, slot.x, slot.y, slot.w)
                    API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "+", slot.x + slot.w / 2, slot.y + slot.h / 2 + 4)
                }
                is Slot.ScrollUp -> {
                    API.FillRect(slot.x, slot.y, slot.w, slot.h, 0x1a1a1a, 220)
                    API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "^", slot.x + slot.w / 2, slot.y + slot.h - 3)
                }
                is Slot.ScrollDown -> {
                    API.FillRect(slot.x, slot.y, slot.w, slot.h, 0x1a1a1a, 220)
                    API.DrawText(FontType.SMALL, WHITE, TextModifier.CENTER, "v", slot.x + slot.w / 2, slot.y + slot.h - 3)
                }
                is Slot.Tag -> {
                    drawFrame(frame, slot.x, slot.y, slot.w)
                    if (slot.name == activeTag) {
                        API.DrawRect(slot.x, slot.y, slot.w, slot.h, 0xffff00)
                    }
                    val iconItem = tags[slot.name]?.firstOrNull()
                    if (iconItem != null) {
                        API.GetObjSprite(iconItem, 1, false, 0, 0)?.render(slot.x, slot.y - 2)
                    } else {
                        API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.CENTER, "?", slot.x + slot.w / 2, slot.y + slot.h / 2 + 4)
                    }
                }
            }
        }

        activeTag?.let {
            API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.LEFT, "Tag: $it", host.x - TAB_SIZE, host.y + TOP_MARGIN - 8)
        }

        applyFilter(container)
    }

    private val WHITE = FontColor.fromColor(java.awt.Color.WHITE)

    private fun drawFrame(frame: Component?, x: Int, y: Int, size: Int) {
        if (frame != null && frame.spriteId != -1) {
            // The native frame sprite renders at its own native size (48x48);
            // draw it at the same top-left so it still frames the smaller
            // tab area reasonably rather than trying to force-scale it.
            API.GetSprite(frame.spriteId)?.render(x - (48 - size) / 2, y - (48 - size) / 2)
        } else {
            API.FillRect(x, y, size, size, 0x2b2b2b, 220)
        }
    }

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

    private fun hostAndContainer(): Pair<Component, Component>? {
        val components = InterfaceList.components[BANK_IFACE] ?: return null
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return null
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return null
        return host to container
    }

    object ClickHandler : MouseAdapter() {
        // mousePressed, not mouseClicked: the native Mouse listener (registered
        // before this plugin's, so it always runs first on the same event)
        // latches the click into Mouse.pendingClickX/Y/Button on press, which
        // the next game tick turns into a walk-here/world action. Zeroing
        // pendingClickButton here, synchronously on the same AWT dispatch,
        // erases it before that tick ever sees it - this is a custom-drawn
        // overlay, not a real interface component, so the game has no other
        // way to know a click here shouldn't reach the world.
        override fun mousePressed(e: MouseEvent?) {
            e ?: return
            val p = instance ?: return
            val (host, container) = p.hostAndContainer() ?: return

            for (slot in p.buildLayout(host, container)) {
                if (e.x in slot.x until (slot.x + slot.w) && e.y in slot.y until (slot.y + slot.h)) {
                    Mouse.pendingClickButton = 0
                    when (slot) {
                        is Slot.NewTab -> p.promptNewTag()
                        is Slot.ScrollUp -> p.scrollOffset = maxOf(0, p.scrollOffset - 1)
                        is Slot.ScrollDown -> p.scrollOffset += 1
                        is Slot.Tag -> p.activeTag = if (p.activeTag == slot.name) null else slot.name
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
            val (host, container) = p.hostAndContainer() ?: return
            val layout = p.buildLayout(host, container)
            val bounds = layout.firstOrNull() ?: return
            val overBar = e.x in bounds.x until (bounds.x + TAB_SIZE) &&
                    e.y in (host.y + TOP_MARGIN) until (host.y + host.height - 40)
            if (overBar) {
                p.scrollOffset = (p.scrollOffset + e.wheelRotation).coerceAtLeast(0)
            }
        }
    }
}
