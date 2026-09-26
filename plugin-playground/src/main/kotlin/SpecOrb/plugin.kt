package SpecOrb

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import plugin.api.WindowMode
import rt4.*

/**
 * Adds a special attack orb to the minimap, in the fourth orb slot below run
 * energy (the one the summoning orb uses), built from the same sprites as the
 * game's own orbs. Special energy is always sent by the server as varp 300,
 * whatever is equipped, so the orb shows it even without a special weapon.
 *
 * Drawn from ComponentDraw at the moment the run orb's frame is drawn: the
 * orbs share the minimap's repaint region, so that is exactly when the spot
 * underneath has just been repainted. Drawing any other time would stack the
 * orb's translucent edges onto an unrepainted background in SD mode.
 */
@PluginMeta(
    author = "OblongNoodle",
    description = "Special attack orb beside the minimap, like the HP/prayer/run orbs.",
    version = 1.0
)
class plugin : Plugin() {
    companion object {
        private const val SPECIAL_ENERGY_VARP = 300 // energy x10
        private const val RUN_ORB_FRAME = (750 shl 16) or 1
        private const val SUMMONING_ORB_FRAME = (747 shl 16) or 1

        // Where the fourth orb slot sits relative to the run orb's slot
        // (interface 548 comps 72 -> 73 fixed, 746 comps 15 -> 16 resizable).
        private const val FIXED_DX = -16
        private const val FIXED_DY = 38
        private const val RESIZABLE_DX = -23
        private const val RESIZABLE_DY = 29

        private const val ORB_W = 57
        private const val ORB_H = 34
        private const val FILL_SIZE = 31

        private const val FRAME_SPRITE = 1206   // orb frame shared by HP/prayer/summoning
        private const val FILL_SPRITE = 1244    // teal fill (summoning's)
        private const val EMPTY_SPRITE = 1245   // dark overlay over the drained part
        private const val ICON_SPRITE = 197     // Attack skill sword
        // The sword is a 25x25 canvas holding a 21x21 diagonal blade, too big
        // for the orb (its tips would cross the rim) and off-centre in its
        // canvas. Drawn scaled to a 21x21 canvas (~18px blade, like the other
        // orb icons), which puts the blade's centre on the orb's centre here.
        private const val ICON_SIZE = 21
        private const val ICON_X = 7
        private const val ICON_Y = 6
        // Number position: centred where the run orb centres its own (its
        // text box is 2px left of the HP-style frame's).
        private const val TEXT_CENTER_X = 41
        private const val TEXT_BASELINE_Y = 26
    }

    private var frame: Sprite? = null
    private var fill: Sprite? = null
    private var empty: Sprite? = null
    private var icon: Sprite? = null
    private var spritesForHd: Boolean? = null

    private var lastPercent = -1
    private var runOrbFrame: Component? = null
    private var summoningSeenAt = 0L

    override fun ComponentDraw(componentIndex: Int, component: Component, screenX: Int, screenY: Int) {
        when (component.id) {
            SUMMONING_ORB_FRAME -> summoningSeenAt = System.currentTimeMillis()
            RUN_ORB_FRAME -> {
                runOrbFrame = component
                // Never draw over a real orb if the server ever docks one there.
                if (System.currentTimeMillis() - summoningSeenAt < 2000) return
                val resizable = API.GetWindowMode() != WindowMode.FIXED
                drawOrb(
                    screenX + if (resizable) RESIZABLE_DX else FIXED_DX,
                    screenY + if (resizable) RESIZABLE_DY else FIXED_DY
                )
            }
        }
    }

    override fun Draw(timeDelta: Long) {
        // Ask for the minimap region to be repainted when the value changes;
        // the orb itself is only ever drawn from ComponentDraw above.
        val percent = specPercent()
        if (percent != lastPercent) {
            lastPercent = percent
            runOrbFrame?.let { InterfaceList.redraw(it) }
        }
    }

    override fun OnLogout() {
        runOrbFrame = null
        lastPercent = -1
    }

    private fun specPercent(): Int = (VarpDomain.activeVarps[SPECIAL_ENERGY_VARP] / 10).coerceIn(0, 100)

    private fun loadSprites(): Boolean {
        // SD and HD use different sprite classes; reload after a switch.
        if (spritesForHd != GlRenderer.enabled) {
            frame = null; fill = null; empty = null; icon = null
            spritesForHd = GlRenderer.enabled
        }
        if (frame == null) frame = API.GetSprite(FRAME_SPRITE)
        if (fill == null) fill = API.GetSprite(FILL_SPRITE)
        if (empty == null) empty = API.GetSprite(EMPTY_SPRITE)
        if (icon == null) icon = API.GetSprite(ICON_SPRITE)
        return frame != null && fill != null && empty != null && icon != null
    }

    private fun drawOrb(x: Int, y: Int) {
        if (!loadSprites()) return
        val percent = specPercent()

        // The run orb's own clip is its 57x34 slot; widen it to ours and put
        // it back afterwards, since the client carries on drawing with it.
        val saved = Clip.save()
        try {
            Clip.set(x, y, x + ORB_W, y + ORB_H)
            frame!!.render(x, y)
            fill!!.render(x + 1, y + 1)
            val drained = FILL_SIZE * (100 - percent) / 100
            if (drained > 0) {
                Clip.set(x + 1, y + 1, x + 1 + FILL_SIZE, y + 1 + drained)
                empty!!.render(x + 1, y + 1)
                Clip.set(x, y, x + ORB_W, y + ORB_H)
            }
            icon!!.renderResized(x + ICON_X, y + ICON_Y, ICON_SIZE, ICON_SIZE)

            // Same spot and colours as the game's orb numbers: green when
            // full, through yellow, to red when empty.
            Fonts.p11Full.renderCenter(JagString.of(percent.toString()), x + TEXT_CENTER_X, y + TEXT_BASELINE_Y, textColor(percent), 0)
        } finally {
            Clip.restore(saved)
        }
    }

    private fun textColor(percent: Int): Int {
        val red = if (percent >= 50) 255 * (100 - percent) / 50 else 255
        val green = if (percent >= 50) 255 else 255 * percent / 50
        return (red shl 16) or (green shl 8)
    }

    /** Current raster clip for whichever renderer is active. */
    private object Clip {
        // GlRaster keeps its right edge private; everything else is public.
        private val glClipRight = GlRaster::class.java.getDeclaredField("clipRight").apply { isAccessible = true }

        fun save(): IntArray = if (GlRenderer.enabled) {
            intArrayOf(GlRaster.clipLeft, GlRaster.clipTop, glClipRight.getInt(null), GlRaster.clipBottom)
        } else {
            intArrayOf(SoftwareRaster.clipLeft, SoftwareRaster.clipTop, SoftwareRaster.clipRight, SoftwareRaster.clipBottom)
        }

        fun set(left: Int, top: Int, right: Int, bottom: Int) {
            if (GlRenderer.enabled) GlRaster.setClip(left, top, right, bottom)
            else SoftwareRaster.setClip(left, top, right, bottom)
        }

        fun restore(c: IntArray) = set(c[0], c[1], c[2], c[3])
    }
}
