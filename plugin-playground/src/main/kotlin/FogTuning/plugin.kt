package FogTuning

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import KondoKit.Exposed

// v1.0/v1.1 both targeted the wrong mechanism. Confirmed live (disabling
// fog entirely still cut off at the same distance as the "widened fade"
// version) that GL_FOG was never what was limiting how far you could see -
// GlobalConfig.VIEW_DISTANCE is what actually matters, and it does two
// separate things:
//   1. FogManager.setFogParams derives the fog range from it (what v1.0/
//      v1.1 were tuning).
//   2. GlRenderer.setupPerspectiveView passes it straight in as the
//      camera's far clip plane, every frame - a hard, fog-independent cutoff.
// Crucially it's eye-space distance from the CAMERA, not ground distance
// from the player. Zoomed out, the camera itself sits much farther back,
// so most of that fixed distance budget gets spent just reaching from the
// camera to nearby terrain, leaving very little left beyond the player -
// exactly why this shrank so much with zoom, and why fixing only the fog
// blend could never have fixed the actual cutoff.
//
// The game's real loaded area (SceneGraph: a fixed 104x104 tiles around the
// player, ~52 tiles from center) is well within reach of this client's
// existing visibility grid (already sized for 56 tiles, comfortably more
// than 52) - the grid was never the bottleneck. viewDistanceTiles raises
// VIEW_DISTANCE itself, which raises both the far clip plane and the fog
// range together, letting the camera actually reach that already-loaded
// edge instead of clipping well short of it.
//
// fadeScale and disableFogEntirely (from v1.1) are kept as a secondary,
// complementary control - once the real cutoff is pushed back with
// viewDistanceTiles, fadeScale still controls how gradually the approach
// to THAT (now much farther) edge is disguised.
@PluginMeta(
        author = "OblongNoodle",
        description = "Raises the camera's actual render/clip distance, plus fog fade tuning.",
        version = 1.2
)
class plugin : Plugin() {
    @Exposed(description = "Render/clip distance in tiles (Default: 200 - native project default is 56, original stock client was 28). Higher shows more of the loaded world when zoomed out, at some GPU cost.")
    var viewDistanceTiles: Int = 200

    @Exposed(description = "Disable world distance fog entirely (Default: false - leaves a hard edge instead of a fade, most people will prefer fadeScale)")
    var disableFogEntirely: Boolean = false

    // VIEW_FADE_DISTANCE (what this multiplies) already scales up
    // proportionally with viewDistanceTiles on its own - going from 56 to
    // 200 tiles already widened the native fade band by the same ~3.6x on
    // its own, before this multiplier even applies. 6.0 was tuned back
    // when viewDistanceTiles was still 56 and never got re-checked against
    // the new base - stacked together the fade band ended up ~21x native
    // width, wide enough to cover most of the visible world in some amount
    // of haze at almost any zoom. 1.5 keeps proportionally the same
    // gradual-not-abrupt fade as before, just without compounding on top
    // of the already-widened base.
    @Exposed(description = "Fade length multiplier vs. native, AFTER accounting for viewDistanceTiles already widening it proportionally (Default: 1.5, 1.0 = pure native ratio)")
    var fadeScale: Float = 1.5f

    override fun Init() {
        apply()
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        when {
            commandStr.equals("::viewdistance", ignoreCase = true) || commandStr.equals("::rd", ignoreCase = true) -> {
                val value = args?.getOrNull(0)?.toIntOrNull()
                if (value == null || value <= 0) {
                    API.SendMessage("Usage: ::viewdistance <tiles> (current: $viewDistanceTiles)")
                } else {
                    viewDistanceTiles = value
                    apply()
                    API.SendMessage("View distance set to $viewDistanceTiles tiles")
                }
            }
            commandStr.equals("::disablefog", ignoreCase = true) || commandStr.equals("::togglefog", ignoreCase = true) -> {
                disableFogEntirely = !disableFogEntirely
                apply()
                API.SendMessage("World fog disabled entirely: " + (if (disableFogEntirely) "ON" else "OFF"))
            }
            commandStr.equals("::fogfade", ignoreCase = true) -> {
                val value = args?.getOrNull(0)?.toFloatOrNull()
                if (value == null || value <= 0f) {
                    API.SendMessage("Usage: ::fogfade <number> (current: $fadeScale, native is 1.0)")
                } else {
                    fadeScale = value
                    apply()
                    API.SendMessage("Fog fade scale set to $fadeScale")
                }
            }
        }
    }

    private fun apply() {
        API.SetViewDistanceTiles(viewDistanceTiles)
        API.SetWorldFogEnabled(!disableFogEntirely)
        API.SetFogFadeScale(fadeScale)
    }
}
