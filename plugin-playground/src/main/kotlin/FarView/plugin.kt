package FarView

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import KondoKit.Exposed

// In-game settings for the client's distant view (rt4.FarTerrain /
// FarScenery): the ground, textures and scenery drawn beyond the loaded
// 104x104 area. Settings are in the KondoKit panel or via chat commands.
@PluginMeta(
        author = "OblongNoodle",
        description = "Distant terrain and scenery: how far, and how detailed.",
        version = 1.0
)
class plugin : Plugin() {
    @Exposed(description = "How far from the camera to draw distant ground and scenery, in tiles (Default: 200, 0 = off). Higher costs GPU memory and a little frame rate.")
    var distanceTiles: Int = 200

    @Exposed(description = "Draw walls, trees, buildings and roofs in the distance (Default: true)")
    var scenery: Boolean = true

    @Exposed(description = "Texture the distant ground like the real ground (Default: true, false = flat colours)")
    var textures: Boolean = true

    @Exposed(description = "Skip distant trees/rocks/plants shorter than this, in 1/128ths of a tile (Default: 96, 0 = keep everything). Higher = less GPU memory.")
    var sceneryMinHeight: Int = 96

    @Exposed(description = "Simplify distant scenery to a grid this size, in 1/128ths of a tile (Default: 0 = full detail). Higher = faster, but thin fences and branches break up.")
    var simplify: Int = 0

    override fun Init() {
        apply()
    }

    // KondoKit calls this (by name) after its settings panel changes a field
    fun OnKondoValueUpdated() {
        apply()
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        when {
            commandStr.equals("::farview", ignoreCase = true) -> {
                val value = args?.getOrNull(0)?.toIntOrNull()
                if (value == null || value < 0) {
                    API.SendMessage("Usage: ::farview <tiles> (current: $distanceTiles, 0 = off)")
                } else {
                    distanceTiles = value
                    apply()
                    API.SendMessage("Distant view set to $distanceTiles tiles")
                }
            }
            commandStr.equals("::farsimplify", ignoreCase = true) -> {
                val value = args?.getOrNull(0)?.toIntOrNull()
                if (value == null || value < 0) {
                    API.SendMessage("Usage: ::farsimplify <grid> (current: $simplify, 0 = full detail, 16/32 = simpler)")
                } else {
                    simplify = value
                    apply()
                    API.SendMessage("Distant scenery simplify set to $simplify")
                }
            }
            commandStr.equals("::farscenery", ignoreCase = true) -> {
                scenery = !scenery
                apply()
                API.SendMessage("Distant scenery: " + (if (scenery) "ON" else "OFF"))
            }
            commandStr.equals("::fartextures", ignoreCase = true) -> {
                textures = !textures
                apply()
                API.SendMessage("Distant ground textures: " + (if (textures) "ON" else "OFF"))
            }
        }
    }

    private fun apply() {
        API.SetFarView(distanceTiles, scenery, textures, sceneryMinHeight, simplify)
    }
}
