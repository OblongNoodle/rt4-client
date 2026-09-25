package BiggerBank

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import plugin.api.FontColor
import plugin.api.FontType
import plugin.api.TextModifier
import KondoKit.Exposed
import rt4.Component
import rt4.GameShell
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

    private var hoverWatchLastState = ""

    // Live tracer for InterfaceList.hoveringScrollableComponent getting
    // stuck true (the exact bug class already fixed once for the bank-close
    // case - see commit "Fix hoveringScrollableComponent getting stuck true
    // after bank closes"). Logs every time the flag or its owning component
    // changes, so a fresh stuck-true report can be traced to the exact
    // component/bounds without depending on BasicInputQOL's own debug
    // overlay (which has a separate, unrelated problem in at least one
    // deployment environment - this sidesteps it entirely by writing
    // straight to a file from a plugin already confirmed to be running
    // there).
    private fun watchHoverState() {
        val state = "hovering=${InterfaceList.hoveringScrollableComponent} id=${InterfaceList.hoveringComponentId} debug=${InterfaceList.hoveringComponentDebug}"
        if (state != hoverWatchLastState) {
            hoverWatchLastState = state
            try {
                java.io.PrintWriter(java.io.FileWriter("hoverstate_watch.log", true)).use {
                    it.println("[${System.currentTimeMillis()}] $state")
                }
            } catch (e: Exception) {
                // best-effort
            }
        }
    }

    // Set by reflowItems, drawn from ComponentDraw - see its comment for why
    // a direct API.DrawText call from inside Draw()/reflowItems() never
    // reaches the screen (painted over by the bank window's own later
    // render pass in the same frame). Learned the hard way: the previous
    // attempt at this used a clip rect scoped to the host window instead of
    // the full canvas, which stuck around for whatever drew after it that
    // same frame and cut off unrelated content - BankTagLayouts' own label
    // drawing already established the full-canvas-clip pattern for exactly
    // this reason.
    private var pendingLabels: List<Pair<Int, String>> = emptyList()
    private var labelsDrawnThisFrame = false
    private var debugOverlay = false
    private var debugLine1 = ""
    private var debugLine2 = ""

    companion object {
        const val BANK_IFACE = 762
        const val SLOT = 44
        const val ORIG_COLS = 10

        // Placed way outside any plausible visible range - not just above/
        // below the container's own bounds, since the container itself gets
        // centered/repositioned around the canvas and its absolute screen Y
        // varies with window size.
        const val OFFSCREEN_Y = -10000

        // Items start at yOffset=5 (see reflowItems). Native's own item
        // container height (205) leaves (205-5)/44 = 4.5 rows - already a
        // fractional row natively, which is exactly the kind of boundary
        // this whole feature was fighting. Bumping the base height by this
        // amount makes (height-5) an exact multiple of SLOT for ANY
        // extraRows value (adding whole SLOT multiples on top preserves an
        // exact-multiple base), so the visible window always holds a whole
        // number of rows with zero leftover fractional space - nothing left
        // for a partial row to occupy in the first place.
        const val BASE_HEIGHT_ADJUSTMENT = 20

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
        //  stretchWidth, stretchHeight, anchorRight, anchorBottom, centerH)
        data class Rule(
                val idx: Int, val ox: Int, val oy: Int, val ow: Int, val oh: Int,
                val stretchW: Boolean = false, val stretchH: Boolean = false,
                val anchorRight: Boolean = false, val anchorBottom: Boolean = false,
                val centerH: Boolean = false
        )

        // Exposed so other plugins (BankTagLayouts) can compute item
        // container's true absolute screen position - it's nested two levels
        // deep (host -> this root component -> item container), not
        // directly under host.
        const val ROOT_COMPONENT_IDX = 61
        val ROOT_RULE = Rule(ROOT_COMPONENT_IDX, 0, 0, 512, 334, stretchW = true, stretchH = true)

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
                // the bottom row (were left floating at their original absolute y
                // when everything below them shifted down), and kept centered as
                // a group within the widening bottom bar rather than left-stuck.
                Rule(14, 184, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(15, 184, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(16, 221, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(17, 221, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(18, 258, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(19, 258, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(20, 295, 287, 35, 35, anchorBottom = true, centerH = true),
                Rule(21, 295, 287, 35, 35, anchorBottom = true, centerH = true),
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
        // Was 15 - our hand-drawn label sits close to the bottom of this
        // band (see ComponentDraw's divider line at HEADER_HEIGHT-2), which
        // left almost no breathing room before the next row of items.
        // Native's own header apparently had internal padding baked into
        // its own rendering that we don't get for free when drawing text
        // ourselves, so the reserved band needs to be taller instead.
        const val HEADER_HEIGHT = 19

        // A separate highlight/indent graphic sits behind the first item of
        // EVERY tab, including the unlabeled main tab (unlike the text
        // headers, which skip it) - found via a fresh ::dumpinterface 762.
        // [64] is an unused spare (always hidden); [65] = main tab's indent,
        // [66] = "Tab 2", ... [72] = "Tab 8". The script positions it 2px up
        // and left of the first item it decorates.
        val INDENT_INDICES = listOf(64, 65, 66, 67, 68, 69, 70, 71, 72)

        // script_1467 (the bank's tab-range CS2 script) computes each tab's
        // item-slot range as a cumulative sum of these per-tab item counts.
        // Verified via ::dumpvarbits against known bank contents rather than
        // trusted from decompiled pseudocode alone (which has at least one
        // provably-impossible self-referential branch elsewhere in that
        // script, i.e. a decompiler artifact, not real game logic).
        // Index 0 = tab 1 (main, unlabeled), indices 1-7 = "Tab 2".."Tab 8".
        val TAB_SIZE_VARBITS = listOf(4885, 4886, 4887, 4888, 4889, 4890, 4891, 4892)

        // Other plugins (BankTagLayouts) that need genuine, non-clipped extra
        // space to the left of the bank window set this instead of resizing
        // the host container themselves - avoids two plugins fighting over
        // the same shared component every frame. BiggerBank stays the single
        // authority for host sizing/positioning; this just adds to it.
        var externalLeftMargin: Int = 0

        // Called at the very end of reflowItems(), after this plugin's own
        // default packed-grid positions have already been set on every
        // visible item this frame - lets another plugin (BankTagLayouts)
        // override specific item positions with certainty about ordering.
        // A timing-based approach (e.g. having the other plugin reposition
        // from its own Draw() or LateDraw()) doesn't work here: BOTH
        // plugins' Draw() fire from the very same render-tree checkpoint
        // every single frame, forever, so whichever one "wins" depends on
        // plugin iteration order - and even LateDraw (which runs after the
        // whole frame) only sets up state that this plugin's OWN next-frame
        // Draw() call immediately overwrites again before that frame ever
        // renders. A direct callback sidesteps the ordering question
        // entirely.
        var afterReflow: ((container: Component) -> Unit)? = null
    }

    override fun Draw(timeDelta: Long) {
        labelsDrawnThisFrame = false
        // Unconditional - the bug this traces is specifically about the
        // stuck state OUTLIVING the bank being open/closed/enabled, so this
        // has to keep watching regardless of any of that.
        watchHoverState()
        // Safety net for clipItemContainer()'s narrow clip below: if last
        // frame's ComponentDraw (which is supposed to reset it - see that
        // function's own comment) didn't fire for any reason, this undoes
        // it before anything else renders this frame, so a missed reset
        // costs at most one frame instead of persisting indefinitely.
        API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
        if (!enabled) return
        val components = InterfaceList.components[BANK_IFACE] ?: return

        resizeHostContainer()
        forceRedrawHost()

        // ROOT_RULE (component 61) is the only one of these positioned
        // directly against the host container - everything else (chrome,
        // item container, search overlay, scrollbar; confirmed via
        // ::dumpinterface 762, all report overlayer=<component 61's id>) is a
        // CHILD of component 61, so its x/y is already relative to component
        // 61's own (already-shifted) origin. Adding externalLeftMargin to
        // those too double-counted the shift: right-anchored pieces ended up
        // positioned past component 61's own (unshifted) width and got
        // clipped by its own bounds, while anything with no Rule entry never
        // shifted at all, leaving it misaligned against siblings that did -
        // that combination was the "right side cut off, top-left corner
        // looks wrong" bug.
        apply(components, ROOT_RULE, externalLeftMargin)
        for (rule in CHROME_RULES) apply(components, rule)
        apply(components, ITEM_CONTAINER_RULE)
        apply(components, SEARCH_OVERLAY_RULE)
        apply(components, SCROLLBAR_RULE)
        resizeScrollbarParts(components)
        reflowItems(components)
        clipItemContainer(components)
    }

    // Narrows the raster clip to exactly the item container's own visible
    // rectangle, BEFORE the engine's own rendering pass for this frame
    // draws its children (the real bank items) - unlike ComponentDraw
    // (which only fires AFTER that rendering, too late to constrain it;
    // see its own comment), Draw() runs early enough in the frame that a
    // clip set here is still active when native item rendering happens
    // later. Confirmed clip rects DO persist across a frame's later
    // rendering in this exact codebase already - that's the whole reason
    // ComponentDraw has to explicitly reset it afterward instead of it
    // resetting on its own.
    //
    // This is what makes a genuinely PARTIAL row (say, 13 of its 44 pixels
    // above the container's top edge) show only the portion actually
    // inside the container - real pixel clipping of the native item
    // sprites, not the show/hide-the-whole-row compromise from before.
    // clampRowToVisible's own off-screen push is kept for rows ENTIRELY
    // outside (cheap, avoids relying on the clip for something with zero
    // visible pixels anyway), but no longer needed for partial overlap -
    // that's now handled here, for real.
    private fun clipItemContainer(components: Array<Component?>) {
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX) ?: return
        val root = components.getOrNull(ROOT_COMPONENT_IDX) ?: return
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val absX = host.x + root.x + container.x
        val absY = host.y + root.y + container.y
        API.ClipRect(absX, absY, absX + container.width, absY + container.height)
    }

    private fun resizeHostContainer() {
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        // Clamped to the actual canvas size: an uncapped extraColumns/extraRows
        // (or a small client window) can otherwise ask for a window wider/taller
        // than the screen, which pushes the centered x/y negative - clipping the
        // left/top edge off-screen while the right/bottom edge overflows past the
        // canvas on the other side. Leaving a few px of margin keeps the window's
        // own border sprite from butting exactly against the canvas edge.
        val newWidth = (512 + extraColumns * SLOT + externalLeftMargin).coerceAtMost(GameShell.canvasWidth - 8)
        val newHeight = (334 + extraHeightPx()).coerceAtMost(GameShell.canvasHeight - 8)
        host.width = newWidth
        host.height = newHeight
        // Center on the actual game canvas rather than growing from its
        // original top-left anchor, so the window stays centered regardless
        // of size.
        host.x = (GameShell.canvasWidth - newWidth) / 2
        host.y = (GameShell.canvasHeight - newHeight) / 2
    }

    // The engine uses dirty-rectangle redraw: a screen region the engine
    // doesn't think changed this frame just keeps whatever was last drawn
    // there, with no draw calls (and no clip rect) re-applied to it at all.
    // That's what let items "completely ignore" clipItemContainer's clip
    // rather than just clip incorrectly - a sprite that ever bled into the
    // tab-icon strip or button row on some earlier frame could sit there as
    // a stale leftover indefinitely, since neither of those neighboring
    // regions is otherwise something this plugin gives the engine a reason
    // to consider "dirty" every frame. Forcing the WHOLE host window's
    // rectangle(s) to redraw every frame - not just the item container's -
    // guarantees any such leftover gets genuinely re-rendered (and this
    // time properly clipped) instead of persisting untouched. Same native
    // utility MiniMenu already uses to force its own bounds to redraw.
    private fun forceRedrawHost() {
        val host = InterfaceList.components[HOST_IFACE]?.getOrNull(HOST_COMPONENT_IDX) ?: return
        InterfaceList.forceRedrawScreen(host.x, host.y, host.height, host.width)
    }

    // Shared by every place that used to just say "extraRows * SLOT" for a
    // height/vertical-offset calculation - see BASE_HEIGHT_ADJUSTMENT for
    // why every one of them needs this same constant folded in, not just
    // the item container itself (chrome that stretches/anchors to the
    // bottom needs to grow/shift by the same total amount, or things drift
    // out of alignment with each other).
    private fun extraHeightPx(): Int = extraRows * SLOT + BASE_HEIGHT_ADJUSTMENT

    private fun apply(components: Array<Component?>, rule: Rule, extraMargin: Int = 0) {
        val c = components.getOrNull(rule.idx) ?: return
        val extraW = extraColumns * SLOT
        val extraH = extraHeightPx()
        c.x = extraMargin + rule.ox + (if (rule.anchorRight) extraW else if (rule.centerH) extraW / 2 else 0)
        c.y = rule.oy + (if (rule.anchorBottom) extraH else 0)
        c.width = rule.ow + (if (rule.stretchW) extraW else 0)
        c.height = rule.oh + (if (rule.stretchH) extraH else 0)
    }

    private fun resizeScrollbarParts(components: Array<Component?>) {
        val scrollbar = components.getOrNull(SCROLLBAR_IDX) ?: return
        val children = scrollbar.createdComponents ?: return
        val extraH = extraHeightPx()
        // [0] track background spans the full height minus the arrow buttons
        children.getOrNull(0)?.let { it.height = 176 + extraH }
        // [5] bottom end cap sits right above the down-arrow, at the new bottom
        children.getOrNull(5)?.let { it.y = 192 + extraH }
    }

    // Sets a native item/indent component's position from its computed
    // content-space Y, EXCEPT when any part of its row falls outside the
    // container's current scrolled-to visible window - then it's parked far
    // off-screen instead.
    //
    // With the top edge now snapped to a row/header boundary every frame
    // (see reflowItems' scrollY snap), a row can still partially overlap
    // the BOTTOM edge whenever a header's HEADER_HEIGHT (not a normal SLOT)
    // falls somewhere inside the current window and throws off uniform
    // spacing - clipItemContainer's clip rect is a native-rendering-timing-
    // dependent fix for that (unreliable per the dirty-rectangle findings
    // logged in biggerbank_clamp_debug.log's history), so this is the
    // actual, timing-independent guarantee: anything not ENTIRELY within
    // bounds simply doesn't render there, rather than needing the clip (or
    // an exact row-count fit every time) to work out.
    private fun clampRowToVisible(component: Component, contentY: Int, container: Component, tag: String = "") {
        val relY = contentY - container.scrollY
        val offscreen = relY < 0 || relY + SLOT > container.height
        component.y = if (offscreen) OFFSCREEN_Y else contentY
        if (debugOverlay) {
            try {
                java.io.PrintWriter(java.io.FileWriter("biggerbank_clamp_debug.log", true)).use {
                    it.println("[${System.currentTimeMillis()}] tag=$tag compId=${component.createdComponentId} contentY=$contentY scrollY=${container.scrollY} containerH=${container.height} relY=$relY offscreen=$offscreen finalY=${component.y}")
                }
            } catch (e: Exception) {
                // best-effort
            }
        }
    }

    private fun reflowItems(components: Array<Component?>) {
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val children = container.createdComponents ?: return
        val cols = ORIG_COLS + extraColumns

        // Cumulative item count before each tab boundary, read straight from
        // the real per-tab sizes instead of inferred from screen position.
        // Every tab (including the unlabeled main one) gets an indent graphic;
        // only tabs 2-8 also get a text header.
        //
        // Native's own "Tab 2".."Tab 8" text numbers groups with no idea the
        // leftover/Tab-1 group (see below) even exists - once Tab 1 is
        // properly counted as tab #1, every native label reads one number
        // low, and the real last group (native's own "Tab 8") is actually
        // tab #9, which native has no text for at all. Confirmed against
        // real item names and the user's own tab-by-tab comparison, not
        // guessed. Native's header components are hidden and replaced with
        // our own correctly-numbered text (drawn from ComponentDraw - see
        // its comment for why); indents are left completely alone, both for
        // their own native positioning and as the read signal for "should
        // this boundary exist at all" (checking headers for that would be
        // reading back our own hidden-flag override a frame later, since
        // we're the one writing it now).
        data class Boundary(val itemsBefore: Int, val indentIdx: Int?, val labelText: String?)
        var cumulative = 0
        val boundaries = mutableListOf<Boundary>()
        val nativeHeadersToHide = mutableListOf<Int>()
        val indentActiveDebug = StringBuilder()
        for ((i, varbitId) in TAB_SIZE_VARBITS.withIndex()) {
            val size = VarpDomain.getVarbit(varbitId)
            val itemsBefore = cumulative
            cumulative += size

            val indentIdx = INDENT_INDICES.getOrNull(i + 1)
            val indentActive = indentIdx?.let { components.getOrNull(it)?.hidden == false } == true
            indentActiveDebug.append(if (indentActive) "1" else "0")
            if (!indentActive) continue

            // Tab 2 (i==0) never had a native header component to begin
            // with (nothing to hide there), but gets the same custom label
            // treatment as every other numbered tab now for consistency.
            if (i > 0) HEADER_INDICES.getOrNull(i - 1)?.let { nativeHeadersToHide.add(it) }
            boundaries.add(Boundary(itemsBefore, indentIdx, "Tab ${i + 2}"))
        }
        if (debugOverlay) {
            debugLine1 = "indentActive=$indentActiveDebug boundaries=${boundaries.size} scrollY=${container.scrollY} scrollMaxV=${container.scrollMaxV} containerH=${container.height}"
        }

        // Sorted by real bank slot (createdComponentId), NOT raw array order -
        // same lesson already learned the hard way on applyFilter (see its
        // comment): once BankTagLayouts's repositionLoadoutItems has injected/
        // removed clones from this same array at least once, array order for
        // the real items is no longer guaranteed to match ascending slot
        // order. TAB_SIZE_VARBITS' cumulative boundaries are counts over the
        // bank's real, contiguous per-tab slot ranges, so itemIndex has to
        // walk items in that same real-slot order to land on the right side
        // of a boundary.
        val bySlot = children.filterNotNull()
                .filter { !it.hidden && it.width == 36 && it.height == 32 }
                .sortedBy { it.createdComponentId }

        // Tab 1 (the default/leftover tab) has no size varbit of its own -
        // its real content is simply whatever isn't claimed by the 8 counted
        // tabs above (TAB_SIZE_VARBITS' combined `cumulative`). That's also
        // why untagged deposits appear to land "in the last tab": new items
        // get appended past everything else in real slot order, even though
        // Tab 1's icon sits FIRST in the tab bar. This reorders the render
        // sequence to match the tab bar (leftover group first, then the 8
        // counted tabs in their normal order) instead of raw slot order -
        // confirmed against the user's own tab-by-tab comparison. Native
        // header/indent components are repositioned same as before, just
        // shifted along with everything else.
        val remainderCount = maxOf(0, bySlot.size - cumulative)
        val visibleItems = bySlot.filter { it.createdComponentId >= cumulative } +
                bySlot.filter { it.createdComponentId < cumulative }
        // Every boundary shifts, including tab 2's own (itemsBefore=0) - it
        // used to mark "the very first item" back when tab 2 genuinely was
        // first; now that the leftover group has taken that spot, tab 2's
        // boundary has to move to wherever it now actually starts
        // (remainderCount).
        val shiftedBoundaries = boundaries.map { it.copy(itemsBefore = it.itemsBefore + remainderCount) }

        // The leftover/Tab 1 group needs its own indent too - native's own
        // documented convention is that EVERY tab gets one, only tabs 2+
        // also get a header. INDENT_INDICES[0] (component 64) has been an
        // unused "always hidden" spare up to now, precisely because nothing
        // needed a 9th indent slot before Tab 1 was recognized as its own
        // real tab - it's unhidden here since we're the one giving it a job
        // for the first time, not native.
        val finalBoundaries = if (remainderCount > 0) {
            components.getOrNull(INDENT_INDICES[0])?.hidden = false
            listOf(Boundary(0, INDENT_INDICES[0], null)) + shiftedBoundaries
        } else {
            shiftedBoundaries
        }

        var col = 0
        var yOffset = 5
        var itemIndex = 0
        var boundaryPos = 0
        val labelsToDraw = mutableListOf<Pair<Int, String>>() // relY -> text
        // Every content-space Y the container's top edge is allowed to land
        // on exactly - the top of the very first row, plus every point a
        // new row (or, at a tab boundary, a header) begins. Snapping
        // scrollY to the nearest of these after the loop (below) is what
        // guarantees the visible window always starts and ends exactly on
        // a row/header boundary - never mid-row - regardless of how scrollY
        // got wherever it currently is (wheel, arrow clicks, or dragging
        // the scrollbar thumb all just set it directly; this corrects
        // whatever any of them left it at, every single frame).
        val rowStarts = mutableListOf(yOffset)
        for (child in visibleItems) {
            // Place any header/indent whose boundary falls right before this
            // item, so each reads as "here comes the next tab's items"
            // rather than trailing the group it was actually generated for.
            while (boundaryPos < finalBoundaries.size && finalBoundaries[boundaryPos].itemsBefore <= itemIndex) {
                val boundary = finalBoundaries[boundaryPos]
                // Force a fresh row for EVERY boundary, not just labeled
                // ones - the leftover group's own boundary has no label, and
                // whichever tab used to render first always landed at
                // col==0 for free. Now that render order doesn't match slot
                // order, a boundary can land mid-row without this, putting
                // its indent graphic behind whatever item happens to be at
                // that column instead of at the start of its own section.
                if (col != 0) {
                    col = 0
                    yOffset += SLOT
                    rowStarts.add(yOffset)
                }
                if (boundary.labelText != null) {
                    labelsToDraw.add(yOffset to boundary.labelText)
                    yOffset += HEADER_HEIGHT
                    rowStarts.add(yOffset)
                }
                // Indent sits directly behind the first item it decorates,
                // 2px up and to the left - matches the script's own offset.
                boundary.indentIdx?.let { idx ->
                    components.getOrNull(idx)?.let {
                        it.x = 8 - 2
                        clampRowToVisible(it, yOffset - 2, container, "indent")
                    }
                }
                boundaryPos++
            }

            child.x = 8 + col * SLOT
            clampRowToVisible(child, yOffset, container, "item")
            col++
            itemIndex++
            if (col >= cols) {
                col = 0
                yOffset += SLOT
                rowStarts.add(yOffset)
            }
        }
        if (col != 0) yOffset += SLOT
        container.scrollMaxV = maxOf(yOffset, container.height)

        // Snap to whichever tracked row/header start is closest to wherever
        // native scroll handling (wheel/arrows/thumb-drag) already left
        // scrollY this frame - what makes each scroll step reveal/hide a
        // whole row at the TOP, regardless of input method.
        //
        // v1 also required the resulting BOTTOM edge to land exactly on
        // another tracked boundary, to account for headers (HEADER_HEIGHT
        // instead of a normal SLOT) shifting things out of uniform SLOT
        // spacing. Correct in principle, but in a bank with lots of small
        // tabs, headers are frequent enough relative to the window height
        // that almost no top position actually satisfies that - scrolling
        // could then only reach a couple of valid spots total, which is the
        // "stuck"/flickering regression this replaces. Snapping the top
        // alone doesn't need that guarantee: clampRowToVisible already
        // treats ANY row not entirely within bounds as off-screen (not just
        // fully-outside ones - see its own comment), so whatever doesn't
        // cleanly fit at the bottom because of a header just doesn't render
        // there, rather than needing a mathematically perfect fit every
        // time. Occasionally shows one row fewer than the window could
        // otherwise fit; never shows a partial one.
        //
        // maxScroll itself is a special case worth adding as its own always-
        // valid candidate: scrolling to exactly here puts the LAST row's own
        // bottom edge exactly flush with the container's bottom edge, by
        // definition (maxScroll = contentEnd - height, and the last row ends
        // exactly at contentEnd) - so the true final row is ALWAYS shown
        // complete from this position, regardless of whether maxScroll
        // happens to land on a "normal" row start. Without this, the
        // nearest tracked row-start could be short of maxScroll whenever a
        // header disrupts uniform spacing near the end, and the real last
        // row would fail clampRowToVisible's fits-entirely check from THAT
        // position and never be reachable at all - permanently hidden no
        // matter how far down you scrolled.
        // Same reasoning applies at the TOP: 0 (true start of content) is
        // always safe - nothing exists in negative content-space to clip -
        // but it was never itself a tracked row start (the first one is 5,
        // where the very first item sits). Resting at true scrollY=0
        // without this would get pulled to 5 as the "nearest" candidate,
        // which pushes the leftover/"infinity" tab's own indent (sitting a
        // couple pixels above the first item) just outside the strict
        // visibility check - explains it flickering specifically at the
        // very top: native's own clamp pulls scrollY back toward 0 for a
        // frame on every scroll-up tick before this correction pulls it
        // back to 5 again.
        val maxScroll = maxOf(0, container.scrollMaxV - container.height)
        rowStarts.add(maxScroll)
        rowStarts.add(0)
        container.scrollY = rowStarts.minByOrNull { kotlin.math.abs(it - container.scrollY) }
                ?.coerceIn(0, maxScroll) ?: container.scrollY.coerceIn(0, maxScroll)

        // Hide every native header we're replacing - done as its own pass,
        // after the indent-based active checks above, so it's never read
        // back as "native says hide this" on some future frame.
        for (idx in nativeHeadersToHide) {
            components.getOrNull(idx)?.hidden = true
        }
        pendingLabels = labelsToDraw
        if (debugOverlay) {
            debugLine2 = "remainderCount=$remainderCount visibleItems=${visibleItems.size} labelsComputed=${labelsToDraw.size} labelRelYs=${labelsToDraw.map { it.first }}"
            try {
                java.io.PrintWriter(java.io.FileWriter("biggerbank_clamp_debug.log", true)).use {
                    it.println("[${System.currentTimeMillis()}] tag=labels scrollY=${container.scrollY} containerH=${container.height} boundaries=${finalBoundaries.size} labelsComputed=${labelsToDraw.size} labels=$labelsToDraw rowStarts=$rowStarts")
                }
            } catch (e: Exception) {
                // best-effort
            }
        }

        afterReflow?.invoke(container)
    }

    // Draw()/reflowItems() fires too early in the frame - anything drawn
    // there gets painted over once the bank window's own chrome composites
    // on top later in the same frame. ComponentDraw for a LATER-indexed
    // component of the same interface fires after that compositing, since
    // Cs1ScriptRunner.renderComponent() processes a component array in
    // ascending index order.
    //
    // Pinning this to exactly the scrollbar's own index (95) was the bug
    // behind the disappearing labels: ComponentDraw only fires for
    // components the engine actually redraws that frame, and the scrollbar
    // apparently isn't guaranteed to be one of them on every frame (worse at
    // smaller sizes/certain scroll positions - even the debug overlay,
    // despite being drawn before any other logic, vanished right along with
    // it, proving the callback itself wasn't firing at all rather than
    // firing with empty data). BankTagLayouts' own identical trick for
    // interface 746 never pins to one specific index either - it accepts
    // ANY component index past its threshold, so it only needs ONE of
    // several to redraw that frame. Matching that here instead of requiring
    // the scrollbar specifically.
    //
    // Clip rect has two different, easy-to-conflate failure modes here, and
    // this needs to dodge both: clipping to the whole host window (tried
    // first) cut off unrelated content, because nothing resets the raster
    // clip automatically and whatever drew next that frame inherited a clip
    // narrower than it needed. Clipping to the full canvas (tried next) let
    // labels render outside the bank window entirely at scroll positions/
    // sizes where their absolute Y fell outside the window's own bounds -
    // native headers never do this because the engine clips real components
    // to their container automatically, but our hand-drawn text bypasses
    // that. The actual fix is BOTH: clip to the container's own visible
    // rectangle specifically (not the whole host, not the full canvas) while
    // drawing, then explicitly put the clip back to the full canvas
    // immediately after so nothing later in the frame inherits ours.
    override fun ComponentDraw(componentIndex: Int, component: Component, screenX: Int, screenY: Int) {
        if (labelsDrawnThisFrame) return
        if ((component.id ushr 16) != BANK_IFACE || componentIndex <= ITEM_CONTAINER_IDX) return
        labelsDrawnThisFrame = true

        // Resets the narrow clip clipItemContainer() set back in Draw() -
        // by this point in the frame the container's real children (bank
        // items) have already been rendered by the engine, constrained to
        // that clip; everything drawn from here on this frame (including
        // this function's own label/debug drawing below) needs the full
        // canvas again. This is the paired reset Draw()'s own safety-net
        // comment refers to.
        API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)

        // Fixed-position readout, drawn unconditionally (before the early
        // return below) specifically so it still shows up on the exact
        // frames where pendingLabels is empty - the previous version
        // returned before ever reaching this when there was nothing to
        // draw, which is exactly the situation it exists to diagnose.
        if (debugOverlay) {
            val white = FontColor.fromColor(java.awt.Color.WHITE)
            // Top-left instead of bottom-left - bottom overlapped the
            // chatbox/other bottom-anchored UI, making it unreadable.
            API.DrawText(FontType.SMALL, white, TextModifier.LEFT, debugLine1, 10, 40)
            API.DrawText(FontType.SMALL, white, TextModifier.LEFT, debugLine2, 10, 55)
        }

        if (pendingLabels.isEmpty()) return
        val components = InterfaceList.components?.getOrNull(BANK_IFACE) ?: return
        val host = InterfaceList.components?.getOrNull(HOST_IFACE)?.getOrNull(HOST_COMPONENT_IDX) ?: return
        val root = components.getOrNull(ROOT_COMPONENT_IDX) ?: return
        val container = components.getOrNull(ITEM_CONTAINER_IDX) ?: return
        val absX = host.x + root.x + container.x
        val containerAbsY = host.y + root.y + container.y
        val baseY = containerAbsY - container.scrollY
        // API.ClipRect's 3rd/4th args are absolute right/bottom coordinates
        // (it passes straight through to native setClip(left, top, right,
        // bottom)), NOT a width/height - confirmed by reading
        // SoftwareRaster.setClip's own parameter names. Passing
        // container.width/height directly here (as if they were deltas) is
        // what made the clip degenerate/inverted and blanked the labels out
        // entirely at some sizes.
        //
        // No padding past the container's exact edges - a previous version
        // padded this by 16px to chase what turned out to be a different
        // bug entirely (ComponentDraw not firing some frames, fixed above by
        // accepting any qualifying component index instead of pinning to
        // the scrollbar). The padding wasn't needed for that, and instead
        // let real bank items bleed into the tab-icon row above and the
        // button row below when scrolled near either edge - this clip is
        // reached during the SAME render pass as the container's own
        // children, not safely after it.
        API.ClipRect(absX, containerAbsY, absX + container.width, containerAbsY + container.height)
        for ((relY, text) in pendingLabels) {
            API.DrawText(FontType.SMALL, FontColor.YELLOW, TextModifier.LEFT, text, absX + 2, baseY + relY + 12)
            // Divider line replacing the one native drew as part of the
            // header component we hid along with its text - moved up from
            // HEADER_HEIGHT-2 to leave a real gap before the next row of
            // items starts at HEADER_HEIGHT, instead of nearly touching it.
            API.FillRect(absX, baseY + relY + HEADER_HEIGHT - 6, container.width, 1, 0x5C4630, 0)
        }
        API.ClipRect(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (commandStr.equals("::biggerbank", ignoreCase = true)) {
            enabled = !enabled
            API.SendMessage("Bigger bank: " + (if (enabled) "enabled" else "disabled"))
        }
        if (commandStr.equals("::biggerbank_debug", ignoreCase = true)) {
            debugOverlay = !debugOverlay
            API.SendMessage("Bank label debug overlay: " + (if (debugOverlay) "ON" else "OFF"))
        }
    }
}
