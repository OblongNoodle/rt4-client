package BankTagLayouts

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import rt4.Component
import rt4.InterfaceList
import rt4.Inv
import rt4.ObjTypeList
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/**
 * Phase 1 of a RuneLite "Bank Tag Layouts" equivalent: 2009scape has no
 * bank-tags concept at all (that was itself a RuneLite plugin, not a Jagex
 * feature), so this builds tagging from scratch rather than extending
 * something that already exists.
 *
 * This phase covers: right-click "Tag" on any bank item, a vertical column
 * of tag-tab icons on the left edge of the bank window, and filtering the
 * item grid to a selected tag. Deliberately NOT yet included: drag-to-
 * reposition custom layouts, placeholder icons for absent items, custom
 * background images, and the GE/HA value header - those are real, separate
 * pieces of work for a later pass once this foundation is confirmed solid.
 *
 * Interaction with BiggerBank: filtering just sets hidden=true on
 * non-matching item components. BiggerBank's reflow already skips hidden
 * items and compacts the rest into its current column count, so tag
 * filtering "just works" at whatever size BiggerBank is set to, with no
 * coordination code needed - this only holds for auto-flowed items, though;
 * a future custom-layout phase would need to disable BiggerBank's reflow
 * for tag views specifically, since explicit saved positions and
 * auto-compaction are incompatible.
 */
@PluginMeta(
        author = "OblongNoodle",
        description = "Bank Tag Layouts equivalent - tag bank items and browse by tag (phase 1: tagging + filtering).",
        version = 1.0
)
class plugin : Plugin() {

    companion object {
        const val BANK_IFACE = 762
        const val ROOT_IDX = 61
        const val ITEM_CONTAINER_IDX = 73
        const val BANK_INV_ID = 95
        const val MAX_BANK_SLOTS = 1000

        // Bank (762) is hosted as a sub-interface inside this component of
        // the main game interface (746) - same one BiggerBank centers on
        // screen (see ::findhost 762). Component 61 (762's own root) always
        // sits at LOCAL (0,0) relative to this host, since it just fills it -
        // it does NOT give screen position, only this host component does.
        const val HOST_IFACE = 746
        const val HOST_COMPONENT_IDX = 6

        const val ICON_SIZE = 32
        const val ICON_MARGIN = 4
        const val BAR_OFFSET_X = 36 // bar sits just to the left of the window

        var instance: plugin? = null
    }

    // tag name -> item ids tagged with it. LinkedHashMap keeps tab order stable.
    private val tags = LinkedHashMap<String, MutableSet<Int>>()
    private var activeTag: String? = null

    override fun Init() {
        instance = this
        loadTags()
        API.AddMouseListener(ClickHandler)
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

    // ─── Drawing the tag bar + filtering ────────────────────────────────────

    override fun Draw(timeDelta: Long) {
        val components = InterfaceList.components[BANK_IFACE] ?: return
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return

        drawTagBar(host)
        applyFilter(container)
    }

    private fun barX(host: Component) = host.x - BAR_OFFSET_X
    private fun barTopY(host: Component) = host.y + 40

    private fun drawTagBar(root: Component) {
        val x = barX(root)
        var y = barTopY(root)

        API.FillRect(x, y, ICON_SIZE, ICON_SIZE, 0x2b2b2b, 220)
        API.DrawText(FontType.SMALL, FontColor.fromColor(java.awt.Color.WHITE), TextModifier.CENTER, "+", x + ICON_SIZE / 2, y + ICON_SIZE / 2 + 4)
        y += ICON_SIZE + ICON_MARGIN

        for ((name, ids) in tags) {
            val selected = name == activeTag
            API.FillRect(x, y, ICON_SIZE, ICON_SIZE, if (selected) 0x4a4a20 else 0x2b2b2b, 220)
            val iconItem = ids.firstOrNull()
            if (iconItem != null) {
                API.GetObjSprite(iconItem, 1, false, 0, 0)?.render(x + 2, y + 2)
            } else {
                API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.CENTER, "?", x + ICON_SIZE / 2, y + ICON_SIZE / 2 + 4)
            }
            y += ICON_SIZE + ICON_MARGIN
        }

        activeTag?.let {
            API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.LEFT, "Tag: $it", x, y + 10)
        }
    }

    private fun applyFilter(container: Component) {
        val tag = activeTag
        val children = container.createdComponents ?: return

        if (tag == null) {
            // No tag active - make sure nothing is left hidden from a
            // previous filter pass (native tab switching already manages
            // hidden state for its own items, so only touch what we set).
            return
        }
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
        override fun mouseClicked(e: MouseEvent?) {
            e ?: return
            val p = instance ?: return
            val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return

            val x = p.barX(host)
            var y = p.barTopY(host)

            if (e.x in x..(x + ICON_SIZE) && e.y in y..(y + ICON_SIZE)) {
                p.promptNewTag()
                return
            }
            y += ICON_SIZE + ICON_MARGIN

            for (name in p.tags.keys) {
                if (e.x in x..(x + ICON_SIZE) && e.y in y..(y + ICON_SIZE)) {
                    p.activeTag = if (p.activeTag == name) null else name
                    return
                }
                y += ICON_SIZE + ICON_MARGIN
            }
        }
    }
}
