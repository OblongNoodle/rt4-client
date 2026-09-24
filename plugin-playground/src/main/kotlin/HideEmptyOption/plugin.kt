package HideEmptyOption

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import plugin.api.MiniMenuEntry
import KondoKit.Exposed

// Removes the "Empty" option from potions specifically - a common misclick,
// since it sits right next to "Drink" in the exact same menu.
//
// v1.0 removed "Empty" from ANY item, which also silently ate the
// Ectophial's only option (its "Empty" IS its primary use, teleporting you
// home - it has no "Drink"). Name-matching on "potion" wouldn't fully fix
// this either - plenty of real potions (Zamorak brew, Super restore,
// Sanfew serum...) don't have "potion" anywhere in their name. The
// reliable, name-independent signal is structural: a genuine drinkable
// potion always has BOTH "Drink" and "Empty" on the same item; anything
// with only "Empty" and no "Drink" sibling (like the Ectophial) is left
// alone entirely.
//
// Native has no way to remove a menu entry once built (only
// API.InsertMiniMenuEntry to add one), so this relies on a new
// API.RemoveMiniMenuEntry added alongside it, which just wraps
// MiniMenu.remove(entry.index) - the same native removal Protocol.java
// itself already uses elsewhere.
//
// currentEntries is built in ascending native-index order (see
// API.GetMiniMenuEntries), so walking it backwards and removing matches as
// they're found deletes from the highest index down - removing a lower
// index first would shift every entry after it down by one and invalidate
// the indices baked into any other MiniMenuEntry still queued for removal.
@PluginMeta(
        author = "OblongNoodle",
        description = "Removes the 'Empty' option from potions (only items that also have 'Drink'), to stop misclicking it instead of Drink.",
        version = 1.1
)
class plugin : Plugin() {
    @Exposed(description = "Hide the 'Empty' menu option on potions (Default: true)")
    var enabled: Boolean = true

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        if (!enabled || currentEntries == null) return
        val drinkableSubjects = currentEntries
            .filter { it.verb == "Drink" }
            .map { it.subjectIndex }
            .toSet()
        if (drinkableSubjects.isEmpty()) return
        for (i in currentEntries.indices.reversed()) {
            val entry = currentEntries[i]
            if (entry.verb == "Empty" && entry.subjectIndex in drinkableSubjects) {
                API.RemoveMiniMenuEntry(entry)
            }
        }
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (commandStr.equals("::hideemptyoption", ignoreCase = true)) {
            enabled = !enabled
            API.SendMessage("Hide 'Empty' option: " + (if (enabled) "ON" else "OFF"))
        }
    }
}
