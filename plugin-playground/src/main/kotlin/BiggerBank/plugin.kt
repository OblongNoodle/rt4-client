package BiggerBank

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import KondoKit.Exposed
import rt4.Component
import rt4.InterfaceList
import rt4.VarpDomain

/**
 * Enlarges the bank interface (762) by directly resizing/repositioning its live
 * Component objects, rather than drawing an overlay - since click hit-testing
 * reads the exact same x/y/width/height fields we're writing, clicks stay
 * correct at the new layout automatically.
 *
 * The item grid's positions are recomputed by a server-sent script on every
 * bank interaction (tab switch, search, deposit/withdraw) using a hardcoded
 * 10-columns-per-row layout, so this has to keep re-applying every frame
 * rather than running once.
 *
 * "View all tabs at once" mode's "Tab N" boundary headers are separate
 * top-level components (not nested under the item container), positioned by
 * the script using the old 10-column math. We infer how many items precede
 * each header from its script-assigned position, then re-place it at the
 * correct point in our own, wider-column layout.
 */
@PluginMeta(
        author = "OblongNoodle",
        description = "Resizes the bank window to show more item columns/rows at once.",
        version = 1.0
)
class plugin : Plugin() {

    @Exposed(description = "Enable the resized bank window (Default: true)")
    var enabled: Boolean = true

    @Exposed(description = "Extra item columns to show (Default: 2)")
    var extraColumns: Int = 2

    @Exposed(description = "Extra item rows visible before scrolling (Default: 2)")
    var extraRows: Int = 2

    companion object {
        const val BANK_IFACE = 762
        const val SLOT = 44
        const val ORIG_COLS = 10

        const val ITEM_CONTAINER_IDX = 73
        const val SEARCH_OVERLAY_IDX = 102
        const val SCROLLBAR_IDX = 95

        // Bank (762) is opened as a sub-interface hosted inside this component of
        // the main game interface (746) - found via ::findhost 762. Its size is
        // the actual clip boundary for the whole bank window; resizing 762's own
        // components alone gets clipped at this container's original 512x334.
        const val HOST_IFACE = 746
        const val HOST_COMPONENT_IDX = 6

        // (index, original x, original y, original width, original height,
        //  stretchWidth, stretchHeight, anchorRight, anchorBottom)
        data class Rule(
                val idx: Int, val ox: Int, val oy: Int, val ow: Int, val oh: Int,
                val stretchW: Boolean = false, val stretchH: Boolean = false,
                val anchorRight: Boolean = false, val anchorBottom: Boolean = false
        )

        val ROOT_RULE = Rule(61, 0, 0, 512, 334, stretchW = true, stretchH = true)

        val CHROME_RULES = listOf(
                Rule(0, 17, 25, 488, 305, stretchW = true, stretchH = true),
                Rule(1, 14, 25, 485, 300, stretchW = true, stretchH = true),
                Rule(2, 19, 79, 475, 210, stretchW = true, stretchH = true),
                Rule(3, 19, 79, 3, 210, stretchH = true),
                Rule(4, 0, 52, 32, 242, stretchH = true),
                Rule(6, 480, 51, 32, 243, anchorRight = true, stretchH = true),
                Rule(7, 44, 20, 424, 32, stretchW = true),
                Rule(8, 45, 306, 423, 32, stretchW = true, anchorBottom = true),
                Rule(9, 13, 294, 32, 32, anchorBottom = true),
                Rule(10, 468, 20, 32, 32, anchorRight = true),
                Rule(11, 468, 294, 32, 32, anchorRight = true, anchorBottom = true),
                Rule(12, 20, 272, 473, 32, stretchW = true, anchorBottom = true),
                Rule(13, 19, 65, 474, 32, stretchW = true),
                // Deposit-mode toggle buttons (inventory/equipment/etc) - pinned to
                // the bottom row, were left floating at their original absolute y
                // when everything below them shifted down.
                Rule(14, 184, 287, 35, 35, anchorBottom = true),
                Rule(15, 184, 287, 35, 35, anchorBottom = true),
                Rule(16, 221, 287, 35, 35, anchorBottom = true),
                Rule(17, 221, 287, 35, 35, anchorBottom = true),
                Rule(18, 258, 287, 35, 35, anchorBottom = true),
                Rule(19, 258, 287, 35, 35, anchorBottom = true),
                Rule(20, 295, 287, 35, 35, anchorBottom = true),
                Rule(21, 295, 287, 35, 35, anchorBottom = true),
                Rule(22, 477, 23, 16, 16, anchorRight = true),
                Rule(23, 459, 23, 16, 16, anchorRight = true),
                Rule(24, 118, 23, 273, 15, stretchW = true),
                Rule(96, 449, 43, 47, 38, anchorRight = true),
                Rule(97, 458, 49, 32, 14, anchorRight = true),
                Rule(98, 458, 64, 32, 13, anchorRight = true)
        )

        val ITEM_CONTAINER_RULE = Rule(ITEM_CONTAINER_IDX, 26, 83, 447, 205, stretchW = true, stretchH = true)
        val SEARCH_OVERLAY_RULE = Rule(SEARCH_OVERLAY_IDX, 26, 83, 447, 205, stretchW = true, stretchH = true)
        val SCROLLBAR_RULE = Rule(SCROLLBAR_IDX, 477, 81, 16, 208, anchorRight = true, stretchH = true)

        // The "Tab N" boundary labels shown in "view all tabs" mode are NOT
        // nested inside the item container's createdComponents - they're
        // separate top-level entries in interface 762 that share the
        // container's overlayer scope (found via ::dumpinterface 762).
        // Component 43 = "Tab 2" header, 44 = "Tab 3", ... 49 = "Tab 8"
        // (sequential allocation, confirmed against real varbit data - see
        // VARBIT_IDS below). 50/62/63 are spare slots for tabs beyond 8.
        val HEADER_INDICES = listOf(43, 44, 45, 46, 47, 48, 49, 50, 62, 63)
        const val HEADER_HEIGHT = 15

        // script_1467 (the bank's tab-range CS2 script) computes each tab's
        // item-slot range as a cumulative sum of these per-tab item counts.
        // Verified via ::dumpvarbits against known bank contents rather than
        // trusted from decompiled pseudocode alone (which has at least one
        // provably-impossible self-referential branch elsewhere in that
        // script, i.e. a decompiler artifact, not real game logic).
        // Index 0 = tab 1 (main, unlabeled), indices 1-7 = "Tab 2".."Tab 8".
        val TAB_SIZE_VARBITS = listOf(4885, 4886, 4887, 4888, 4889, 4890, 4891, 4892)
    }

    override fun Draw(timeDelta: Long) {
        if (!enabled) return
        val components = InterfaceList.components[BANK_IFACE] ?: return

        resizeHostContainer()

        apply(components, ROOT_RULE)
        for (rule in CHROME_RULES) apply(components, rule)
        apply(components, ITEM_CONTAINER_RULE)
        apply(components, SEARCH_OVERLAY_RULE)
        apply(components, SCROLLBAR_RULE)
        resizeScrollbarParts(components)
        reflowItems(components)
    }

    private fun resizeHostContainer() {
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        host.width = 512 + extraColumns * SLOT
        host.height = 334 + extraRows * SLOT
    }

    private fun apply(components: Array<Component?>, rule: Rule) {
        val c = components.getOrNull(rule.idx) ?: return
        val extraW = extraColumns * SLOT
        val extraH = extraRows * SLOT
        c.x = rule.ox + (if (rule.anchorRight) extraW else 0)
        c.y = rule.oy + (if (rule.anchorBottom) extraH else 0)
        c.width = rule.ow + (if (rule.stretchW) extraW else 0)
        c.height = rule.oh + (if (rule.stretchH) extraH else 0)
    }

    private fun resizeScrollbarParts(components: Array<Component?>) {
        val scrollbar = components.getOrNull(SCROLLBAR_IDX) ?: return
        val children = scrollbar.createdComponents ?: return
        val extraH = extraRows * SLOT
        // [0] track background spans the full height minus the arrow buttons
        children.getOrNull(0)?.let { it.height = 176 + extraH }
        // [5] bottom end cap sits right above the down-arrow, at the new bottom
        children.getOrNull(5)?.let { it.y = 192 + extraH }
    }

    private fun reflowItems(components: Array<Component?>) {
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val children = container.createdComponents ?: return
        val cols = ORIG_COLS + extraColumns

        // Cumulative item count before each "Tab N" boundary, read straight
        // from the real per-tab sizes instead of inferred from screen
        // position. TAB_SIZE_VARBITS[0] is tab 1 (main, no header); each
        // subsequent entry's cumulative total is where that tab's header
        // (HEADER_INDICES[i - 1]) belongs.
        data class Boundary(val headerIdx: Int, val itemsBefore: Int)
        var cumulative = 0
        val boundaries = mutableListOf<Boundary>()
        for ((i, varbitId) in TAB_SIZE_VARBITS.withIndex()) {
            val size = VarpDomain.getVarbit(varbitId)
            cumulative += size
            if (i == 0) continue // tab 1 (main) has no header
            val headerIdx = HEADER_INDICES.getOrNull(i - 1) ?: continue
            val header = components.getOrNull(headerIdx) ?: continue
            if (header.hidden) continue
            boundaries.add(Boundary(headerIdx, cumulative - size))
        }

        var col = 0
        var yOffset = 5
        var itemIndex = 0
        var boundaryPos = 0
        for (child in children) {
            child ?: continue
            if (child.hidden) continue
            if (child.width != 36 || child.height != 32) continue

            // Place any header whose boundary falls right before this item,
            // so it reads as "here comes the next tab's items" rather than
            // trailing the group it was actually generated for.
            while (boundaryPos < boundaries.size && boundaries[boundaryPos].itemsBefore <= itemIndex) {
                if (col != 0) {
                    col = 0
                    yOffset += SLOT
                }
                components.getOrNull(boundaries[boundaryPos].headerIdx)?.let {
                    it.x = 0
                    it.y = yOffset
                }
                yOffset += HEADER_HEIGHT
                boundaryPos++
            }

            child.x = 8 + col * SLOT
            child.y = yOffset
            col++
            itemIndex++
            if (col >= cols) {
                col = 0
                yOffset += SLOT
            }
        }
        if (col != 0) yOffset += SLOT
        container.scrollMaxV = maxOf(yOffset, container.height)
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (commandStr.equals("::biggerbank", ignoreCase = true)) {
            enabled = !enabled
            API.SendMessage("Bigger bank: " + (if (enabled) "enabled" else "disabled"))
        }
    }
}
