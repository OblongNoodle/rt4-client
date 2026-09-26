package PoeLootFilter

import KondoKit.Exposed
import plugin.Plugin
import plugin.PluginRepository
import plugin.api.API
import plugin.api.API.*
import plugin.api.FontColor.fromColor
import plugin.api.FontType
import plugin.api.MiniMenuEntry
import plugin.api.MiniMenuType
import plugin.api.TextModifier
import com.jogamp.opengl.GL2
import rt4.*
import java.awt.AWTEvent
import java.awt.Color
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.roundToInt
import kotlin.math.sin
import plugin.annotations.PluginMeta

@PluginMeta(
        author = "OblongNoodle",
        description = "PoE style ground item labels",
        version = 1.0
)
class plugin : Plugin() {

    @Exposed(description = "Default: true, Use Local JSON or the prices from the Live/Stable server API")
    private var useLiveGEPrices = true
    @Exposed(description = "Default: true, Toggle GE/HA price visibility")
    private var displayPrices = true
    @Exposed(description = "Default: 5,000 (blue)")
    private var lowValue = 5000
    @Exposed(description = "Default: 20,000 (green)")
    private var mediumValue = 20000
    @Exposed(description = "Default: 50,000 (orange)")
    private var highValue = 50000
    @Exposed(description = "Default: 100,000 (pink/white - insane)")
    private var insaneValue = 100000
    @Exposed(description = "Default: 0, No labels will be drawn for items below this value (unless tagged)")
    private var hideBelowValue = 0
    @Exposed(description = "Tag Items (purple) add/remove with Ctrl+RightClick")
    private lateinit var taggedItems: List<Int>
    @Exposed(description = "Ignore items add/remove with Ctrl+RightClick")
    private lateinit var ignoredItems: List<Int>

    private val coinId = 995
    // Filled in off the game thread (see refreshGEPrices); empty until then.
    @Volatile private var gePriceMap: Map<String, String> = emptyMap()
    private val alertedItems = mutableSetOf<String>()
    private val FONT_H  = 14
    private val PRICE_H = 12
    private var alertVolume = -20

    // ─── Disk folder ──────────────────────────────────────────────────────────
    private val resFolder: File get() = File(
        System.getProperty("user.home") + "/2009scape/plugins/PoeLootFilter/res"
    )

    // ─── Per-tier audio paths ─────────────────────────────────────────────────
    private val tierAudioPaths = mutableMapOf(
        "insane" to "", "high" to "", "medium" to "",
        "low"    to "", "default" to "", "tagged" to "",
    )

    // ─── Per-tier beam enabled ────────────────────────────────────────────────
    private val tierBeamEnabled = mutableMapOf(
        "insane" to false, "high" to false, "medium" to false,
        "low"    to false, "default" to false, "tagged" to false,
    )

    // ─── Beam occlusion (depth test) ──────────────────────────────────────────
    private var beamOcclusion = true
    private var poeBoxOcclusion = true
    private var beamColumnEnabled = true

    // ─── Resize detection — skip GL beam drawing the frame canvas changes ─────
    private var lastCanvasW = -1
    private var lastCanvasH = -1

    // ─── Beam tile info for GL rendering ────────────────────────────────────────
    private data class BeamTileInfo(val tileX: Int, val tileZ: Int, val color: Int, val itemId: Int, val qty: Int)

    // ─── Beam world-space constants ───────────────────────────────────────────
    private val BEAM_WORLD_HEIGHT = 300.0
    private val BEAM_BASE_RADIUS  = 22.0
    private val ICON_DURATION_MS  = 5000L
    private val ICON_FADE_MS      = 1500L

    private data class BeamParticle(
        var sx: Float, var sy: Float,
        var vx: Float, var vy: Float,
        val color: Int,
        var life: Float,
        val maxLife: Float
    )

    private data class BeamIconEntry(
        val itemId: Int,
        val qty: Int,
        val spawnTime: Long,
        val tileX: Int,
        val tileY: Int,
        val beamColor: Int,
        val particles: MutableList<BeamParticle> = mutableListOf(),
        var particlesSpawned: Boolean = false,
        var lastMs: Long = System.currentTimeMillis()
    )

    private val beamIcons = mutableMapOf<String, BeamIconEntry>()

    // Broadcast drop icons — keyed by player name, shown above their head
    private data class BroadcastEntry(
        val itemId: Int,
        val qty: Int,
        val spawnTime: Long,
        val beamColor: Int,
        val particles: MutableList<BeamParticle> = mutableListOf(),
        var particlesSpawned: Boolean = false,
        var lastMs: Long = System.currentTimeMillis()
    )
    private val broadcastIcons = mutableMapOf<String, BroadcastEntry>()
    private var lastProcessedMessageId = ""
    private val BROADCAST_PREFIX = "News: "
    private val BROADCAST_MID    = " has just received: "

    // ─── Beam effect toggles ──────────────────────────────────────────────────
    private val beamEffects = mutableMapOf(
        "pool"          to true,
        "icon"          to true,
        "poeBox"        to true,
        "runes"         to false,
        "sparkles"      to false,
        "invertCone"    to false,
        "pulseRings"    to false,
        "tileHighlight" to false,
        "pentagram"     to false,
        "arrows"        to false,
        "dog"           to false,
        "poeBeam"       to false,
        "helixBeam"     to false,
    )
    private val beamEffectKeys = listOf(
        "pool","icon","poeBox","runes","sparkles","invertCone",
        "pulseRings","tileHighlight","pentagram","arrows","dog","poeBeam","helixBeam"
    )
    private val beamEffectLabels = listOf(
        "Base Pool","Float Icon","PoE Box","Orbit Runes","Sparkles","Invert Cone",
        "Pulse Rings","Tile Glow","Pentagram","Arrows","Loot Dog","PoE Beam","Helix Beam"
    )

    // World-space sparkles per tile
    private data class WorldSparkle(
        val tileKey: String,
        var wx: Int, var wz: Int, var wy: Float,
        val speed: Float, val color: Int,
        var life: Float, val maxLife: Float
    )
    private val worldSparkles = mutableListOf<WorldSparkle>()
    private val sparkleLastSpawn = mutableMapOf<String, Long>()

    // Pulse rings per tile
    private data class PulseRing(val tileKey: String, val cx: Int, val cz: Int, val color: Int, val spawnTime: Long)
    private val pulseRings = mutableListOf<PulseRing>()
    private val ringLastSpawn = mutableMapOf<String, Long>()

    // ─── GL beam rendering ─────────────────────────────────────────────────
    // GL output doesn't work from PlayerOverheadDraw, and the 3D matrices are
    // gone by the time Draw() fires. So we reconstruct both the perspective
    // projection and the camera modelview from game values, then draw from Draw().
    // Depth test uses the depth buffer which still contains wall geometry.

    // ─── Debug ────────────────────────────────────────────────────────────────


    // ─── AWT listeners ────────────────────────────────────────────────────────
    private var keyDispatcher: KeyEventDispatcher? = null
    private var mouseListener: AWTEventListener? = null

    // ─── Tier styles ──────────────────────────────────────────────────────────

    data class TierStyle(
        val key:         String,
        val borderColor: Int,
        val bgColor:     Int,
        val bgAlpha:     Int,
        val textColor:   Int,
        val priceColor:  Int,
        val padH:        Int,
        val padV:        Int,
        val stripeAlpha: Int
    )

    private val defaultStyles = mapOf(
        "insane"  to TierStyle("insane",  0xFF55AA, 0xFFF0F5, 245, 0xFF55AA, 0x884466, 10, 7, 220),
        "high"    to TierStyle("high",    0xFF9600, 0x1A0D00, 230, 0xFF9600, 0x885500,  8, 6, 200),
        "medium"  to TierStyle("medium",  0x99FF99, 0x001A00, 230, 0x99FF99, 0x446644,  7, 5, 190),
        "low"     to TierStyle("low",     0x66B2FF, 0x00091A, 220, 0x66B2FF, 0x334466,  6, 4, 180),
        "default" to TierStyle("default", 0xAAAAAA, 0x0D0D0D, 200, 0xCCCCCC, 0x666666,  5, 3, 160),
        "tagged"  to TierStyle("tagged",  0xAA00FF, 0x0D001A, 225, 0xCC66FF, 0x664488,  7, 5, 190),
    )

    private val currentStyles: MutableMap<String, TierStyle> = defaultStyles.toMutableMap()
    private val tierOrder = listOf("insane", "high", "medium", "low", "default", "tagged")
    private val stripeW = 24

    // ─── Settings panel layout (512 × 336 — fits standard mode) ──────────────

    private var PX = 10; private var PY = 10
    private val PW = 514
    private val PH = 336

    private val TITLE_H      = 20
    private val SEC_LABEL_H  = 14
    private val THRESH_ROW_H = 18   // 2 fields per row → 3 rows
    private val TIER_ROW_H   = 22   // color + audio merged
    private val OPT_H        = 18
    private val DIVIDER_H    = 6
    private val BTN_H        = 20
    private val BTN_MARGIN   = 6

    private val threshStartY   get() = PY + TITLE_H + 2 + SEC_LABEL_H
    private val tierStartY     get() = threshStartY + 3 * THRESH_ROW_H + DIVIDER_H + SEC_LABEL_H
    private val optStartY      get() = tierStartY + 6 * TIER_ROW_H + DIVIDER_H
    private val btnStartY      get() = optStartY + OPT_H + DIVIDER_H

    // ─── Slide-out beam settings panel ─────────────────────────────────────────
    private var beamSlideOpen = false
    private val SLIDE_W = 200
    private val SLIDE_EFF_ROW = 18
    // slide panel X/Y — overlays main panel in standard, adjacent in HD
    private val slideX get() = if (PX + PW + SLIDE_W + 4 > (if (API.IsHD()) GameShell.canvasWidth else 512))
        PX + PW - SLIDE_W  // overlay right side of main panel
    else PX + PW + 2       // adjacent to main panel
    private val slideY get() = PY
    // 2 header toggles (column, occlusion) + divider + label + 10 effects + boxOcclusion row
    private val slideH get() = TITLE_H + 2 * SLIDE_EFF_ROW + DIVIDER_H + SEC_LABEL_H + beamEffectKeys.size * SLIDE_EFF_ROW + SLIDE_EFF_ROW + 6

    private val CPX  get() = if (beamSlideOpen) slideX + SLIDE_W + 4 else PX + PW + 4
    private val CPY  get() = PY
    private val CPW  = 216
    private val SW   = 28; private val SH = 22; private val SCOLS = 6
    private val SROWS    get() = (colorPresets.size + SCOLS - 1) / SCOLS
    private val gridEndY get() = CPY + TITLE_H + SROWS * (SH + 4) + 4
    private val CPH      get() = gridEndY - CPY + 18 + 24 + 26 + BTN_H + BTN_MARGIN + 4

    // ─── Settings state ───────────────────────────────────────────────────────

    private var settingsOpen    = false
    private var activeField     = -1
    private var fieldBuffer     = ""
    private var pickerOpen      = false
    private var pickerTier      = ""
    private var pickerIsBorder  = true
    private var hexBuffer       = ""
    private var hexFieldActive  = false
    private var pickerOrigColor = 0

    private val colorPresets = intArrayOf(
        0xFF0000, 0xFF6600, 0xFFCC00, 0x00BB00, 0x00BBBB, 0x0066FF,
        0xFF99AA, 0xFFCC88, 0xFFFF88, 0x88FF88, 0x88FFEE, 0x88BBFF,
        0x880000, 0x884400, 0x777700, 0x005500, 0x005555, 0x003388,
        0xFFFFFF, 0xBBBBBB, 0x777777, 0x333333, 0x111111, 0x000000,
        0xFF55AA, 0xAA00FF, 0xFF9600, 0x99FF99, 0x66B2FF, 0xFFDD44,
    )

    private val commandMap = mapOf(
        "::poesetlow"    to "low-value",
        "::poesetmed"    to "medium-value",
        "::poesethigh"   to "high-value",
        "::poesetinsane" to "insane-value",
        "::poesethide"   to "hide-below-value",
    )

    // ─── Init ─────────────────────────────────────────────────────────────────

    override fun Init() {
        lowValue        = GetData("low-value")         as? Int     ?: 5000
        mediumValue     = GetData("medium-value")      as? Int     ?: 20000
        highValue       = GetData("high-value")        as? Int     ?: 50000
        insaneValue     = GetData("insane-value")      as? Int     ?: 100000
        hideBelowValue  = GetData("hide-below-value")  as? Int     ?: 0
        alertVolume     = GetData("poe-alert-volume")  as? Int     ?: -20
        useLiveGEPrices = GetData("poe-use-remote")    as? Boolean ?: true
        displayPrices   = GetData("poe-display-price") as? Boolean ?: true
        taggedItems     = GetData("poe-tags")?.let   { it.toString().split(",").mapNotNull { s -> s.toIntOrNull() } } ?: emptyList()
        ignoredItems    = GetData("poe-ignore")?.let { it.toString().split(",").mapNotNull { s -> s.toIntOrNull() } } ?: emptyList()
        refreshGEPrices()
        alertedItems.clear()
        loadColorOverrides()
        loadAudioPaths()
        loadBeamSettings()
        loadBeamEffects()
        if (!resFolder.exists()) resFolder.mkdirs()
        registerAWTListeners()
        // Restore settings panel state — Init() re-runs on resize, which would
        // otherwise reset settingsOpen to false and make the panel vanish.
        settingsOpen = false; StoreData("poe-settings-open", false)
    }

    private fun loadAudioPaths()   { for (tier in tierOrder) tierAudioPaths[tier]  = GetData("poe-audio-$tier")  as? String  ?: "" }
    private fun saveAudioPaths()   { for (tier in tierOrder) StoreData("poe-audio-$tier",  tierAudioPaths[tier]  ?: "") }
    private fun loadBeamSettings() { for (tier in tierOrder) tierBeamEnabled[tier] = GetData("poe-beam-$tier") as? Boolean ?: false; beamOcclusion = GetData("poe-beam-occlusion") as? Boolean ?: true; poeBoxOcclusion = GetData("poe-box-occlusion") as? Boolean ?: true; beamColumnEnabled = GetData("poe-beam-column") as? Boolean ?: true }
    private fun saveBeamSettings() { for (tier in tierOrder) StoreData("poe-beam-$tier", tierBeamEnabled[tier] ?: false); StoreData("poe-beam-occlusion", beamOcclusion); StoreData("poe-box-occlusion", poeBoxOcclusion); StoreData("poe-beam-column", beamColumnEnabled) }
    private fun loadBeamEffects()  { for (k in beamEffectKeys) beamEffects[k] = GetData("poe-fx-$k") as? Boolean ?: (k == "pool" || k == "icon" || k == "poeBox") }
    private fun saveBeamEffects()  { for (k in beamEffectKeys) StoreData("poe-fx-$k", beamEffects[k] ?: false) }

    // ─── AWT listeners ────────────────────────────────────────────────────────
    // IMPORTANT: reloadPlugins() creates a NEW plugin instance via new classloader.
    // Old listeners remain registered but hold references to the dead old instance.
    // A generation counter lets ghost listeners detect they're stale and self-remove.

    private val pluginGeneration: Long = System.nanoTime().also {
        PluginRepository.pluginStorage["poe-listener-gen"] = it
    }

    private fun isCurrentGeneration(): Boolean =
        (PluginRepository.pluginStorage["poe-listener-gen"] as? Long) == pluginGeneration

    private fun registerAWTListeners() {
        keyDispatcher?.let { KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it) }
        mouseListener?.let { Toolkit.getDefaultToolkit().removeAWTEventListener(it) }

        keyDispatcher = KeyEventDispatcher { e ->
            // Ghost check: if a newer instance exists, self-remove
            if (!isCurrentGeneration()) {
                KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyDispatcher)
                return@KeyEventDispatcher false
            }
            if (settingsOpen && e.id == KeyEvent.KEY_TYPED) {
                e.consume()
                if (activeField != -1 || hexFieldActive) handleKeyTyped(e.keyChar)
                true
            } else false
        }
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyDispatcher)

        mouseListener = AWTEventListener { e ->
            val me = e as? MouseEvent ?: return@AWTEventListener
            // Ghost check: if a newer instance exists, self-remove
            if (!isCurrentGeneration()) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(mouseListener)
                return@AWTEventListener
            }
            if (!settingsOpen || me.id != MouseEvent.MOUSE_PRESSED) return@AWTEventListener
            val inPanel  = me.x in PX until PX + PW  && me.y in PY until PY + PH
            val inPicker = pickerOpen && me.x in CPX until CPX + CPW && me.y in CPY until CPY + CPH
            val inSlide  = beamSlideOpen && me.x in slideX until slideX + SLIDE_W && me.y in slideY until slideY + slideH
            if (inPanel || inPicker || inSlide) {
                me.consume()
                handlePanelClick(me.x, me.y)
            } else {
                commitActiveField(); activeField = -1; hexFieldActive = false
            }
        }
        Toolkit.getDefaultToolkit().addAWTEventListener(mouseListener, AWTEvent.MOUSE_EVENT_MASK)
    }

    // ─── Key handler ──────────────────────────────────────────────────────────

    private fun handleKeyTyped(c: Char) {
        when {
            c == '\b' -> {
                if (hexFieldActive && hexBuffer.isNotEmpty())           hexBuffer   = hexBuffer.dropLast(1)
                else if (activeField != -1 && fieldBuffer.isNotEmpty()) fieldBuffer = fieldBuffer.dropLast(1)
            }
            c == '\n' || c == '\r' -> {
                if (hexFieldActive) commitHexField() else commitActiveField()
            }
            hexFieldActive && (c.isDigit() || c.toLowerCase() in 'a'..'f') && hexBuffer.length < 6 -> {
                hexBuffer += c.toUpperCase()
                if (hexBuffer.length == 6) commitHexField()
            }
            activeField != -1 && (c.isDigit() || (c == '-' && activeField == 5 && fieldBuffer.isEmpty())) && fieldBuffer.length < 9 -> {
                fieldBuffer += c
            }
        }
    }

    // ─── Draw ─────────────────────────────────────────────────────────────────
    // Draw() fires during the 2D overlay phase. GL output works here.
    // 3D matrices are reconstructed from camera values before beam drawing.

    override fun Draw(timeDelta: Long) {
        pollBroadcastChat()

        // ── Resize detection ────────────────────────────────────────────────
        // If canvas size changed, skip GL beam drawing this frame to avoid
        // corrupting GL state (pushAttrib without matching popAttrib on exception).
        var canvasStable = true
        if (API.IsHD()) {
            try {
                val cw = GameShell.canvasWidth
                val ch = GameShell.canvasHeight
                if (cw != lastCanvasW || ch != lastCanvasH) {
                    lastCanvasW = cw
                    lastCanvasH = ch
                    canvasStable = false  // skip GL beams this frame
                }
            } catch (_: Exception) {
                canvasStable = false
            }
        }

        // GL beam — skip during resize to keep GL state clean
        if (API.IsHD() && canvasStable) {
            val gl = GlRenderer.gl
            if (gl != null) {
                try {
                    drawBeamsGL(gl)
                } catch (_: Exception) {
                    // If we threw after glPushAttrib, try to clean up
                    try { gl.glPopAttrib() } catch (_: Exception) {}
                    try { gl.glMatrixMode(GL2.GL_PROJECTION); gl.glPopMatrix() } catch (_: Exception) {}
                    try { gl.glMatrixMode(GL2.GL_MODELVIEW); gl.glPopMatrix() } catch (_: Exception) {}
                }
            }
        }

        // ── Click cross overlay ──────────────────────────────────────────────
        // The game draws its cross before our Draw() hook, so GL beams cover it.
        // Re-render the exact same cross sprites on top using the game's own
        // Cross state and Sprites.crosses[] array.
        if (API.IsHD() && Cross.type != 0 && Cross.milliseconds < 400) {
            try {
                val frame = Cross.milliseconds / 100
                val sprite = if (Cross.type == 1) {
                    Sprites.crosses[frame]       // yellow frames 0-3
                } else {
                    Sprites.crosses[frame + 4]   // red frames 4-7
                }
                sprite?.render(Cross.x - 8, Cross.y - 8)
            } catch (_: Exception) {}
        }

        drawActiveIcons()
        renderGroundItemNames()
        if (settingsOpen) drawSettingsPanel()

        // ── GL transparency fix ────────────────────────────────────────────────
        // 2D draws go to SoftwareRaster.pixels; this composites them onto GL.
        // During resize, the pixel array can be reallocated mid-loop — the
        // try-catch handles that by skipping one frame gracefully.
        if (API.IsHD()) {
            try {
                val gl = GlRenderer.gl
                val pixels = SoftwareRaster.pixels
                if (gl != null && pixels != null && pixels.size > 0) {
                    val len = pixels.size
                    for (i in 0 until len) {
                        if (pixels[i] != 0) pixels[i] = pixels[i] or 0xFF000000.toInt()
                    }
                    gl.glEnable(GL2.GL_BLEND)
                    gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA)
                }
            } catch (_: Exception) {
                // Array reallocated mid-loop during resize — safe to skip one frame
            }
        }
    }

    // ─── PlayerOverheadDraw ──────────────────────────────────────────────────

    override fun PlayerOverheadDraw(player: rt4.Player, screenX: Int, screenY: Int) {

        // Broadcast drop icon rendering (unchanged)
        val name = player.name?.toString() ?: return

        val entry = broadcastIcons[name] ?: return
        val now   = System.currentTimeMillis()
        val age   = now - entry.spawnTime
        if (age > ICON_DURATION_MS) { broadcastIcons.remove(name); return }

        val fadeProgress = ((age - (ICON_DURATION_MS - ICON_FADE_MS)).toFloat() / ICON_FADE_MS).coerceIn(0f, 1f)
        val iconAlpha    = ((1f - fadeProgress) * 220f).toInt().coerceIn(0, 220)

        val sprite = API.GetObjSprite(entry.itemId, entry.qty, false, 0, 160) ?: return
        val scaledW = sprite.width  * 4
        val scaledH = sprite.height * 4

        if (!entry.particlesSpawned && age > 150L) {
            val colors = extractParticleColors(sprite, entry.beamColor)
            spawnParticles(
                entry.particles, screenX, screenY - scaledH - 10,
                scaledW, scaledH, colors
            )
            entry.particlesSpawned = true
        }

        val bobOffset = (Math.sin(now * 0.0018) * 8).toInt()
        val iconX     = screenX - scaledW / 2
        val iconY     = screenY - scaledH - 10 + bobOffset

        if (iconAlpha > 0) {
            if (fadeProgress <= 0f) {
                sprite.renderResized(iconX, iconY, scaledW, scaledH)
            } else {
                sprite.renderAlpha(screenX - sprite.width / 2, iconY, iconAlpha)
            }
        }

        val dt = ((now - entry.lastMs).toFloat() / 1000f).coerceAtMost(0.05f)
        entry.lastMs = now
        val dead = mutableListOf<BeamParticle>()
        for (p in entry.particles) {
            p.life -= dt; if (p.life <= 0f) { dead.add(p); continue }
            p.vy += 120f * dt; p.sx += p.vx * dt; p.sy += p.vy * dt
            val pFrac  = (p.life / p.maxLife).coerceIn(0f, 1f)
            val pAlpha = (pFrac * pFrac * 200f * (1f - fadeProgress)).toInt().coerceIn(0, 200)
            val size   = (pFrac * 4f).toInt().coerceAtLeast(1)
            FillRect(p.sx.toInt() - size, p.sy.toInt() - size, size * 2, size * 2, p.color, pAlpha)
            FillRect(p.sx.toInt() - size/2, p.sy.toInt() - size/2, size, size, 0xFFFFFF, (pAlpha * 0.4f).toInt())
        }
        entry.particles.removeAll(dead)
    }

    // ─── VBO batch renderer ───────────────────────────────────────────────────
    // Replaces immediate-mode glBegin/glVertex/glEnd with batched VBO draws.
    // Converts strips, fans, quads to plain triangles; line strips/loops to lines.
    // One or two glDrawArrays calls per flush instead of hundreds of glVertex calls.

    private var triVboId = -1
    private var lineVboId = -1
    private var triBuf: java.nio.FloatBuffer? = null
    private var lineBuf: java.nio.FloatBuffer? = null
    private var triBufCap = 0
    private var lineBufCap = 0

    private inner class GlBatch {
        val VS = 7  // floats per vertex: x y z r g b a
        var triData = FloatArray(60000 * VS)
        var triN = 0  // vertex count
        var lineData = FloatArray(6000 * VS)
        var lineN = 0
        var cr = 1f; var cg = 1f; var cb = 1f; var ca = 1f

        private var mode = -1
        private var tempData = FloatArray(512 * VS)
        private var tempN = 0

        fun color4f(r: Float, g: Float, b: Float, a: Float) { cr = r; cg = g; cb = b; ca = a }

        fun begin(m: Int) { mode = m; tempN = 0 }

        fun vertex3d(x: Double, y: Double, z: Double) {
            when (mode) {
                GL2.GL_TRIANGLES -> pushTri(x.toFloat(), y.toFloat(), z.toFloat())
                GL2.GL_LINES -> pushLine(x.toFloat(), y.toFloat(), z.toFloat())
                else -> {
                    if (tempN * VS >= tempData.size) tempData = tempData.copyOf(tempData.size * 2)
                    val i = tempN * VS
                    tempData[i] = x.toFloat(); tempData[i+1] = y.toFloat(); tempData[i+2] = z.toFloat()
                    tempData[i+3] = cr; tempData[i+4] = cg; tempData[i+5] = cb; tempData[i+6] = ca
                    tempN++
                }
            }
        }

        fun end() {
            when (mode) {
                GL2.GL_TRIANGLE_STRIP -> {
                    for (i in 2 until tempN) {
                        if (i % 2 == 0) { copyTempTri(i-2, i-1, i) } else { copyTempTri(i-1, i-2, i) }
                    }
                }
                GL2.GL_TRIANGLE_FAN -> {
                    for (i in 2 until tempN) { copyTempTri(0, i-1, i) }
                }
                GL2.GL_QUADS -> {
                    var i = 0
                    while (i + 3 < tempN) {
                        copyTempTri(i, i+1, i+2); copyTempTri(i, i+2, i+3); i += 4
                    }
                }
                GL2.GL_LINE_STRIP -> {
                    for (i in 1 until tempN) { copyTempLine(i-1, i) }
                }
                GL2.GL_LINE_LOOP -> {
                    for (i in 1 until tempN) { copyTempLine(i-1, i) }
                    if (tempN > 1) copyTempLine(tempN-1, 0)
                }
            }
            tempN = 0; mode = -1
        }

        private fun pushTri(x: Float, y: Float, z: Float) {
            val idx = triN * VS
            if (idx + VS > triData.size) triData = triData.copyOf(triData.size * 2)
            triData[idx]=x; triData[idx+1]=y; triData[idx+2]=z
            triData[idx+3]=cr; triData[idx+4]=cg; triData[idx+5]=cb; triData[idx+6]=ca
            triN++
        }

        private fun pushLine(x: Float, y: Float, z: Float) {
            val idx = lineN * VS
            if (idx + VS > lineData.size) lineData = lineData.copyOf(lineData.size * 2)
            lineData[idx]=x; lineData[idx+1]=y; lineData[idx+2]=z
            lineData[idx+3]=cr; lineData[idx+4]=cg; lineData[idx+5]=cb; lineData[idx+6]=ca
            lineN++
        }

        private fun copyTempTri(a: Int, b: Int, c: Int) {
            val idx = triN * VS
            if (idx + VS * 3 > triData.size) triData = triData.copyOf(triData.size * 2)
            System.arraycopy(tempData, a * VS, triData, idx, VS)
            System.arraycopy(tempData, b * VS, triData, idx + VS, VS)
            System.arraycopy(tempData, c * VS, triData, idx + VS * 2, VS)
            triN += 3
        }

        private fun copyTempLine(a: Int, b: Int) {
            val idx = lineN * VS
            if (idx + VS * 2 > lineData.size) lineData = lineData.copyOf(lineData.size * 2)
            System.arraycopy(tempData, a * VS, lineData, idx, VS)
            System.arraycopy(tempData, b * VS, lineData, idx + VS, VS)
            lineN += 2
        }

        fun flush(gl: GL2) {
            flushTris(gl); flushLines(gl)
        }

        fun flushTris(gl: GL2) {
            if (triN == 0) return
            val floats = triN * VS
            if (triBuf == null || triBufCap < floats) {
                triBuf = java.nio.ByteBuffer.allocateDirect(floats * 4)
                    .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                triBufCap = floats
            }
            triBuf!!.clear(); triBuf!!.put(triData, 0, floats); triBuf!!.flip()
            if (triVboId < 0) { val ids = IntArray(1); gl.glGenBuffers(1, ids, 0); triVboId = ids[0] }
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, triVboId)
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, floats.toLong() * 4, triBuf, GL2.GL_STREAM_DRAW)
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY)
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY)
            gl.glVertexPointer(3, GL2.GL_FLOAT, VS * 4, 0L)
            gl.glColorPointer(4, GL2.GL_FLOAT, VS * 4, 12L)
            gl.glDrawArrays(GL2.GL_TRIANGLES, 0, triN)
            gl.glDisableClientState(GL2.GL_COLOR_ARRAY)
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY)
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0)
            triN = 0
        }

        fun flushLines(gl: GL2) {
            if (lineN == 0) return
            val floats = lineN * VS
            if (lineBuf == null || lineBufCap < floats) {
                lineBuf = java.nio.ByteBuffer.allocateDirect(floats * 4)
                    .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
                lineBufCap = floats
            }
            lineBuf!!.clear(); lineBuf!!.put(lineData, 0, floats); lineBuf!!.flip()
            if (lineVboId < 0) { val ids = IntArray(1); gl.glGenBuffers(1, ids, 0); lineVboId = ids[0] }
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, lineVboId)
            gl.glBufferData(GL2.GL_ARRAY_BUFFER, floats.toLong() * 4, lineBuf, GL2.GL_STREAM_DRAW)
            gl.glEnableClientState(GL2.GL_VERTEX_ARRAY)
            gl.glEnableClientState(GL2.GL_COLOR_ARRAY)
            gl.glVertexPointer(3, GL2.GL_FLOAT, VS * 4, 0L)
            gl.glColorPointer(4, GL2.GL_FLOAT, VS * 4, 12L)
            gl.glDrawArrays(GL2.GL_LINES, 0, lineN)
            gl.glDisableClientState(GL2.GL_COLOR_ARRAY)
            gl.glDisableClientState(GL2.GL_VERTEX_ARRAY)
            gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0)
            lineN = 0
        }
    }


    // ─── GL beam rendering ────────────────────────────────────────────────────
    // Called from Draw(). Reconstructs perspective projection. Modelview is
    // RotateX(180) matching the game. Yaw and pitch are applied to vertices
    // in software, exactly as the game's scene graph does before GL submission.

    private fun drawBeamsGL(gl: GL2) {
        val plane = Player.plane

        val activeTiles = mutableListOf<BeamTileInfo>()
        try {
            val objStacks = SceneGraph.objStacks ?: return
            for (tx in 0 until 104) {
                for (tz in 0 until 104) {
                    val stack = objStacks[plane][tx][tz] ?: continue
                    var node = stack.head() as? ObjStackNode
                    while (node != null) {
                        if (shouldDisplayItem(node)) {
                            val style = getStyle(node)
                            if (tierBeamEnabled[style.key] == true) {
                                activeTiles.add(BeamTileInfo(tx, tz, style.borderColor,
                                    node.value?.type ?: 0, node.value?.amount ?: 1))
                                break
                            }
                        }
                        node = stack.next() as? ObjStackNode
                    }
                }
            }
        } catch (_: Exception) { return }

        if (activeTiles.isEmpty()) return

        // Guard: screen bounds may be invalid during resize
        if (Rasteriser.screenLowerX == 0 && Rasteriser.screenUpperX == 0) return

        // Save all GL state
        gl.glPushAttrib(GL2.GL_ALL_ATTRIB_BITS)

        val SCALE = 0.09765625f
        val left   = Rasteriser.screenLowerX.toFloat() * SCALE
        val right  = Rasteriser.screenUpperX.toFloat() * SCALE
        val bottom = -Rasteriser.screenUpperY.toFloat() * SCALE
        val top    = -Rasteriser.screenLowerY.toFloat() * SCALE
        val near   = 50.0f
        val far    = 3_000_000.0f

        val near2 = near * 2.0f
        val projMatrix = FloatArray(16)
        projMatrix[0]  = near2 / (right - left)
        projMatrix[5]  = near2 / (top - bottom)
        projMatrix[8]  = (right + left) / (right - left)
        projMatrix[9]  = (top + bottom) / (top - bottom)
        projMatrix[10] = -(far + near) / (far - near)
        projMatrix[11] = -1f
        projMatrix[14] = -(near2 * far) / (far - near)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPushMatrix()
        gl.glLoadMatrixf(projMatrix, 0)

        val pitchDeg = Camera.cameraPitch * 0.17578125f
        val yawDeg   = Camera.cameraYaw   * 0.17578125f

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glLoadIdentity()
        gl.glRotatef(180f, 1f, 0f, 0f)
        gl.glRotatef(pitchDeg, 1f, 0f, 0f)
        gl.glRotatef(yawDeg, 0f, 1f, 0f)

        if (beamOcclusion) {
            gl.glEnable(GL2.GL_DEPTH_TEST)
            gl.glDepthMask(false)
        } else {
            gl.glDisable(GL2.GL_DEPTH_TEST)
        }

        gl.glDisable(GL2.GL_TEXTURE_2D)
        gl.glDisable(GL2.GL_LIGHTING)
        gl.glDisable(GL2.GL_FOG)
        gl.glDisable(GL2.GL_ALPHA_TEST)
        gl.glDisable(GL2.GL_CULL_FACE)
        gl.glEnable(GL2.GL_BLEND)
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA)

        val batch = GlBatch()

        for (tile in activeTiles) {
            if (beamColumnEnabled) drawBeamColumnGL(gl, batch, tile.tileX, tile.tileZ, tile.color)
            drawTileEffectsGL(gl, batch, tile.tileX, tile.tileZ, tile.color)
        }

        batch.flush(gl)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPopMatrix()
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPopMatrix()
        gl.glPopAttrib()

        // ── 2D icon rendering (after GL teardown) ────────────────────────────
        // Icon uses sprite.renderAlpha which writes to SoftwareRaster — must be
        // outside GL state. Use depth readback for occlusion.
        if (beamEffects["icon"] == true) {
            val time = System.currentTimeMillis()
            for (tile in activeTiles) {
                val cx = (tile.tileX shl 7) + 64
                val cz = (tile.tileZ shl 7) + 64
                val center = API.CalculateSceneGraphScreenPosition(cx, cz, 0)
                if (center[0] < 0 || center[1] < 0) continue
                if (isTileOccludedByDepth(center[0], center[1], tile.tileX, tile.tileZ)) continue

                val sprite = API.GetObjSprite(tile.itemId, tile.qty, false, 0, 180)
                if (sprite != null) {
                    val bobOffset = (Math.sin(time * 0.0018) * 6).toInt()
                    val iconX = center[0] - sprite.width / 2
                    val iconY = center[1] - (BEAM_BASE_RADIUS * 3).toInt() - sprite.height + bobOffset
                    sprite.renderAlpha(iconX, iconY, 200)
                }
            }
        }
    }

    private fun drawBeamColumnGL(gl: GL2, batch: GlBatch, tileX: Int, tileZ: Int, color: Int) {
        val camX = Camera.renderX.toDouble()
        val camY = Camera.renderY.toDouble()
        val camZ = Camera.renderZ.toDouble()

        val worldX = ((tileX shl 7) + 64).toDouble()
        val worldZ = ((tileZ shl 7) + 64).toDouble()

        // Camera-relative position — GL modelview handles rotation
        val dx = worldX - camX
        // Use actual tile height, not zero — tiles aren't at Y=0
        val xFine = (tileX shl 7) + 64
        val zFine = (tileZ shl 7) + 64
        val groundY = SceneGraph.getTileHeight(Player.plane, xFine, zFine).toDouble()
        val dy = groundY - camY
        val dz = worldZ - camZ

        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8)  and 0xFF) / 255f
        val b = (color          and 0xFF) / 255f

        val time  = System.currentTimeMillis()
        val pulse = (0.75f + 0.25f * sin(time * 0.003).toFloat())

        val SIDES = 12
        val STEPS = 20
        val baseR = BEAM_BASE_RADIUS

        for (i in 0 until STEPS) {
            val botFrac = i.toFloat() / STEPS
            val topFrac = (i + 1).toFloat() / STEPS

            val botR = (baseR * Math.pow(1.0 - botFrac, 8.0)).coerceAtMost(64.0)
            val topR = (baseR * Math.pow(1.0 - topFrac, 8.0)).coerceAtMost(64.0)
            if (topR < 0.3) continue

            // After RotateX(180), Y is flipped — subtract to go upward on screen
            val botY = dy - botFrac * BEAM_WORLD_HEIGHT
            val topY = dy - topFrac * BEAM_WORLD_HEIGHT

            val coreAlpha = ((1f - botFrac * 0.35f) * pulse * 0.85f).coerceIn(0f, 0.85f)
            val glowAlpha = (coreAlpha * 0.18f).coerceIn(0f, 1f)
            val glowR = (r + (1f - r) * 0.5f).coerceAtMost(1f)
            val glowG = (g + (1f - g) * 0.5f).coerceAtMost(1f)
            val glowB = (b + (1f - b) * 0.5f).coerceAtMost(1f)

            if (glowAlpha > 0.01f) {
                val glowRad = botR * 1.8
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                batch.color4f(glowR, glowG, glowB, glowAlpha)
                for (s in 0..SIDES) {
                    val a = s * 2.0 * Math.PI / SIDES
                    val offX = Math.cos(a) * glowRad
                    val offZ = Math.sin(a) * glowRad
                    batch.vertex3d(dx + offX, botY, dz + offZ)
                    batch.vertex3d(dx + offX, topY, dz + offZ)
                }
                batch.end()
            }

            batch.begin(GL2.GL_TRIANGLE_STRIP)
            batch.color4f(r, g, b, coreAlpha)
            for (s in 0..SIDES) {
                val a = s * 2.0 * Math.PI / SIDES
                val offXb = Math.cos(a) * botR
                val offZb = Math.sin(a) * botR
                val offXt = Math.cos(a) * topR
                val offZt = Math.sin(a) * topR
                batch.vertex3d(dx + offXb, botY, dz + offZb)
                batch.vertex3d(dx + offXt, topY, dz + offZt)
            }
            batch.end()
        }

        // Ground glow disc
        val discRadius = BEAM_BASE_RADIUS * 2.5
        batch.begin(GL2.GL_TRIANGLE_FAN)
        batch.color4f(r, g, b, 0.35f * pulse)
        batch.vertex3d(dx, dy, dz)
        batch.color4f(r, g, b, 0.0f)
        for (s in 0..SIDES) {
            val a = s * 2.0 * Math.PI / SIDES
            batch.vertex3d(dx + Math.cos(a) * discRadius, dy, dz + Math.sin(a) * discRadius)
        }
        batch.end()
    }

    // ─── GL versions of all beam effects ────────────────────────────────────────
    // These draw inside the same GL context as the beam column, so they get
    // depth-test occlusion for free. Coordinates are camera-relative.
    // Y convention: dy = ground, dy - h = upward (RotateX(180) flips Y).

    private fun drawTileEffectsGL(gl: GL2, batch: GlBatch, tileX: Int, tileZ: Int, color: Int) {
        val camX = Camera.renderX.toDouble()
        val camY = Camera.renderY.toDouble()
        val camZ = Camera.renderZ.toDouble()
        val cx = (tileX shl 7) + 64
        val cz = (tileZ shl 7) + 64
        val groundY = SceneGraph.getTileHeight(Player.plane, cx, cz).toDouble()
        val dx = cx.toDouble() - camX
        val dy = groundY - camY
        val dz = cz.toDouble() - camZ

        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val gr = (r + (1f - r) * 0.5f).coerceAtMost(1f)
        val gg = (g + (1f - g) * 0.5f).coerceAtMost(1f)
        val gb = (b + (1f - b) * 0.5f).coerceAtMost(1f)

        val time  = System.currentTimeMillis()
        val pulse = 0.75f + 0.25f * sin(time * 0.003).toFloat()
        val tileKey = "$tileX,$tileZ"
        val SIDES = 16

        // ── Pool: concentric ground discs ──
        if (beamEffects["pool"] == true) {
            fun disc(radius: Double, cr: Float, cg: Float, cb: Float, alpha: Float) {
                if (alpha <= 0.01f) return
                batch.begin(GL2.GL_TRIANGLE_FAN)
                batch.color4f(cr, cg, cb, alpha)
                batch.vertex3d(dx, dy, dz)
                batch.color4f(cr, cg, cb, 0f)
                for (s in 0..SIDES) {
                    val a = s * 2.0 * Math.PI / SIDES
                    batch.vertex3d(dx + Math.cos(a) * radius, dy, dz + Math.sin(a) * radius)
                }
                batch.end()
            }
            disc(minOf(BEAM_BASE_RADIUS * 4.0, 60.0), gr, gg, gb, pulse * 0.16f)
            disc(minOf(BEAM_BASE_RADIUS * 2.8, 52.0), gr, gg, gb, pulse * 0.28f)
            disc(minOf(BEAM_BASE_RADIUS * 1.5, 40.0), r, g, b, pulse * 0.6f)
            disc(minOf(BEAM_BASE_RADIUS * 0.6, 16.0), 1f, 1f, 1f, pulse * 0.45f)
        }

        // ── Inverted cone: funnel (narrow ground, wide top) ──
        if (beamEffects["invertCone"] == true) {
            val STEPS = 20
            val maxR = BEAM_BASE_RADIUS * 1.6
            for (i in 0 until STEPS) {
                val botFrac = i.toFloat() / STEPS
                val topFrac = (i + 1).toFloat() / STEPS
                val botR = (maxR * Math.pow(botFrac.toDouble(), 2.0)).coerceAtMost(64.0)
                val topR = (maxR * Math.pow(topFrac.toDouble(), 2.0)).coerceAtMost(64.0)
                if (botR < 0.3 && topR < 0.3) continue
                val botY = dy - botFrac * BEAM_WORLD_HEIGHT
                val topY = dy - topFrac * BEAM_WORLD_HEIGHT
                val alpha = (botFrac * pulse * 0.85f).coerceIn(0f, 0.85f)
                val gAlpha = alpha * 0.18f

                if (gAlpha > 0.01f) {
                    batch.begin(GL2.GL_TRIANGLE_STRIP)
                    batch.color4f(gr, gg, gb, gAlpha)
                    for (s in 0..10) {
                        val a = s * 2.0 * Math.PI / 10
                        batch.vertex3d(dx + Math.cos(a) * botR, botY, dz + Math.sin(a) * botR)
                        batch.vertex3d(dx + Math.cos(a) * topR, topY, dz + Math.sin(a) * topR)
                    }
                    batch.end()
                }
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                batch.color4f(r, g, b, alpha)
                for (s in 0..10) {
                    val a = s * 2.0 * Math.PI / 10
                    batch.vertex3d(dx + Math.cos(a) * botR, botY, dz + Math.sin(a) * botR)
                    batch.vertex3d(dx + Math.cos(a) * topR, topY, dz + Math.sin(a) * topR)
                }
                batch.end()
            }
        }

        // ── Orbit runes: rotating diamonds at ground level ──
        if (beamEffects["runes"] == true) {
            val spin = (time % 6000L).toFloat() / 6000f * 2.0 * Math.PI
            val orbitR = minOf(BEAM_BASE_RADIUS * 2.8, 55.0)
            val count = 5
            val rPulse = 0.6f + 0.4f * sin(time * 0.004).toFloat()
            val sz = 5.0
            for (i in 0 until count) {
                val angle = spin + i * 2.0 * Math.PI / count
                val offX = Math.cos(angle) * orbitR
                val offZ = Math.sin(angle) * orbitR
                // Outer glow diamond (flat on ground XZ plane)
                batch.begin(GL2.GL_QUADS)
                batch.color4f(gr, gg, gb, rPulse * 0.32f)
                batch.vertex3d(dx + offX, dy, dz + offZ - sz)
                batch.vertex3d(dx + offX + sz, dy, dz + offZ)
                batch.vertex3d(dx + offX, dy, dz + offZ + sz)
                batch.vertex3d(dx + offX - sz, dy, dz + offZ)
                batch.end()
                // Inner bright diamond
                val isz = sz * 0.5
                batch.begin(GL2.GL_QUADS)
                batch.color4f(1f, 1f, 1f, rPulse * 0.78f)
                batch.vertex3d(dx + offX, dy, dz + offZ - isz)
                batch.vertex3d(dx + offX + isz, dy, dz + offZ)
                batch.vertex3d(dx + offX, dy, dz + offZ + isz)
                batch.vertex3d(dx + offX - isz, dy, dz + offZ)
                batch.end()
            }
        }

        // ── Pulse rings: expanding annuli at ground level ──
        if (beamEffects["pulseRings"] == true) {
            val now = time
            if ((now - (ringLastSpawn[tileKey] ?: 0L)) > 800L) {
                pulseRings.add(PulseRing(tileKey, cx, cz, color, now))
                ringLastSpawn[tileKey] = now
            }
            val dead = mutableListOf<PulseRing>()
            val ringDuration = 1600L
            for (ring in pulseRings) {
                if (ring.tileKey != tileKey) continue
                val age = now - ring.spawnTime
                if (age > ringDuration) { dead.add(ring); continue }
                val frac = age.toFloat() / ringDuration
                val ringR = (frac * BEAM_BASE_RADIUS * 6.0).coerceAtMost(62.0)
                val alpha = ((1f - frac) * (1f - frac) * 0.7f).coerceIn(0f, 0.7f)
                if (alpha <= 0.01f) continue
                val outerR = ringR
                val innerR = ringR * 0.82
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                batch.color4f(r, g, b, alpha)
                for (s in 0..SIDES) {
                    val a = s * 2.0 * Math.PI / SIDES
                    batch.vertex3d(dx + Math.cos(a) * outerR, dy, dz + Math.sin(a) * outerR)
                    batch.vertex3d(dx + Math.cos(a) * innerR, dy, dz + Math.sin(a) * innerR)
                }
                batch.end()
            }
            pulseRings.removeAll(dead)
        }

        // ── Tile highlight: square on ground ──
        if (beamEffects["tileHighlight"] == true) {
            batch.flush(gl)  // flush before line width change
            val hPulse = 0.4f + 0.6f * sin(time * 0.002).toFloat().let { it * it }
            val tx0 = (tileX shl 7).toDouble() - Camera.renderX.toDouble()
            val tz0 = (tileZ shl 7).toDouble() - Camera.renderZ.toDouble()
            // Fill
            batch.begin(GL2.GL_QUADS)
            batch.color4f(r, g, b, hPulse * 0.35f)
            batch.vertex3d(tx0, dy, tz0)
            batch.vertex3d(tx0 + 128, dy, tz0)
            batch.vertex3d(tx0 + 128, dy, tz0 + 128)
            batch.vertex3d(tx0, dy, tz0 + 128)
            batch.end()
            // Border
            batch.flush(gl)  // flush fill quads before lineWidth change
            gl.glLineWidth(2f)
            batch.begin(GL2.GL_LINE_LOOP)
            batch.color4f(gr, gg, gb, hPulse * 0.7f)
            batch.vertex3d(tx0, dy, tz0)
            batch.vertex3d(tx0 + 128, dy, tz0)
            batch.vertex3d(tx0 + 128, dy, tz0 + 128)
            batch.vertex3d(tx0, dy, tz0 + 128)
            batch.end()
            batch.flush(gl)  // flush border lines before lineWidth reset
            gl.glLineWidth(1f)
        }

        // ── Pentagram: rotating star on ground ──
        if (beamEffects["pentagram"] == true) {
            batch.flush(gl)  // flush before line width change
            val spin = (time % 12000L).toFloat() / 12000f * 2.0 * Math.PI
            val pPulse = 0.5f + 0.5f * sin(time * 0.002).toFloat()
            val pR = minOf(BEAM_BASE_RADIUS * 2.5, 55.0)
            val innerR = pR * 0.38
            // Fill star polygon
            batch.begin(GL2.GL_TRIANGLE_FAN)
            batch.color4f(r, g, b, pPulse * 0.22f)
            batch.vertex3d(dx, dy, dz)
            for (i in 0..10) {
                val a = spin + i * Math.PI / 5.0
                val rad = if (i % 2 == 0) pR else innerR
                batch.vertex3d(dx + Math.cos(a) * rad, dy, dz + Math.sin(a) * rad)
            }
            batch.end()
            // Star outline
            batch.flush(gl)  // flush fill before lineWidth change
            gl.glLineWidth(2f)
            batch.begin(GL2.GL_LINE_LOOP)
            batch.color4f(gr, gg, gb, pPulse * 0.6f)
            for (i in 0 until 10) {
                val a = spin + i * Math.PI / 5.0
                val rad = if (i % 2 == 0) pR else innerR
                batch.vertex3d(dx + Math.cos(a) * rad, dy, dz + Math.sin(a) * rad)
            }
            batch.end()
            batch.flush(gl)  // flush outline before lineWidth reset
            gl.glLineWidth(1f)
        }

        // ── Sparkles: rising particles ──
        if (beamEffects["sparkles"] == true) {
            batch.flush(gl)  // flush before sparkle line width changes
            val now = time
            if ((now - (sparkleLastSpawn[tileKey] ?: 0L)) > 120L) {
                val glow = blendColor(color, 0xFFFFFF, 0.6f)
                repeat(3) {
                    val angle = Math.random() * 2.0 * Math.PI
                    val dist = (Math.random() * BEAM_BASE_RADIUS * 1.5).toInt().coerceAtMost(48)
                    worldSparkles.add(WorldSparkle(tileKey,
                        cx + (Math.cos(angle) * dist).toInt(),
                        cz + (Math.sin(angle) * dist).toInt(),
                        0f, (18f + Math.random().toFloat() * 24f),
                        if (Math.random() > 0.4f) color else glow,
                        (1.2f + Math.random().toFloat() * 1.2f),
                        (1.2f + Math.random().toFloat() * 1.2f)
                    ))
                }
                sparkleLastSpawn[tileKey] = now
            }
            val dt = 0.016f
            val dead = mutableListOf<WorldSparkle>()
            gl.glLineWidth(2f)
            batch.begin(GL2.GL_LINES)
            for (sp in worldSparkles) {
                if (sp.tileKey != tileKey) continue
                sp.life -= dt
                if (sp.life <= 0f) { dead.add(sp); continue }
                sp.wy += sp.speed * dt * 60f
                val spDx = sp.wx.toDouble() - camX
                val spDz = sp.wz.toDouble() - camZ
                val spDy = dy - sp.wy  // rise upward
                val frac = (sp.life / sp.maxLife).coerceIn(0f, 1f)
                val alpha = (frac * frac * 0.86f).coerceIn(0f, 0.86f)
                val sz = (frac * 4.0).coerceAtLeast(1.0)
                val sr = ((sp.color shr 16) and 0xFF) / 255f
                val sg = ((sp.color shr 8) and 0xFF) / 255f
                val sb = (sp.color and 0xFF) / 255f
                // Small cross — GL_LINES are visible from any angle
                batch.color4f(sr, sg, sb, alpha)
                batch.vertex3d(spDx - sz, spDy, spDz)
                batch.vertex3d(spDx + sz, spDy, spDz)
                batch.vertex3d(spDx, spDy - sz, spDz)
                batch.vertex3d(spDx, spDy + sz, spDz)
                batch.vertex3d(spDx, spDy, spDz - sz)
                batch.vertex3d(spDx, spDy, spDz + sz)
            }
            batch.end()
            batch.flush(gl)  // flush sparkle lines at lineWidth 2
            gl.glLineWidth(1f)
            worldSparkles.removeAll(dead)
            if (worldSparkles.size > 300) worldSparkles.removeAll { it.tileKey != tileKey }
        }

        // ── Chevron arrows: V-shapes above beam ──
        if (beamEffects["arrows"] == true) {
            val bob = Math.sin(time * 0.003) * 8.0
            val arrowH = BEAM_WORLD_HEIGHT * 0.55 + bob
            val aPulse = 0.7f + 0.3f * sin(time * 0.004).toFloat()
            val w = minOf(BEAM_BASE_RADIUS * 1.6, 28.0)
            val h = w * 0.6
            repeat(2) { layer ->
                val yOff = arrowH + layer * (h + 4.0)
                val tipY  = dy - yOff
                val wingY = dy - (yOff + h)
                val alpha = if (layer == 0) aPulse * 0.82f else aPulse * 0.4f
                // Left wing
                batch.begin(GL2.GL_TRIANGLES)
                batch.color4f(r, g, b, alpha)
                batch.vertex3d(dx, tipY, dz)
                batch.vertex3d(dx - w, wingY, dz)
                batch.vertex3d(dx - w * 0.7, wingY, dz)
                batch.end()
                // Right wing
                batch.begin(GL2.GL_TRIANGLES)
                batch.color4f(r, g, b, alpha)
                batch.vertex3d(dx, tipY, dz)
                batch.vertex3d(dx + w, wingY, dz)
                batch.vertex3d(dx + w * 0.7, wingY, dz)
                batch.end()
            }
        }

        // ── Loot Dog: blocky 3D dog that rocks front-to-back ──
        if (beamEffects["dog"] == true) {
            val tilt = Math.sin(time * 0.004) * 12.0  // ±12 degree rock
            batch.flush(gl)  // flush before matrix change

            gl.glPushMatrix()
            gl.glTranslated(dx, dy, dz)
            // Tilt around X axis (front-to-back rocking)
            gl.glRotated(tilt, 1.0, 0.0, 0.0)

            // Dog proportions in world units (1 tile = 128)
            // "Up" is negative Y due to RotateX(180)
            // Colors: body = beam color, highlights = lighter
            val lr = (r + (1f - r) * 0.3f).coerceAtMost(1f)
            val lg = (g + (1f - g) * 0.3f).coerceAtMost(1f)
            val lb = (b + (1f - b) * 0.3f).coerceAtMost(1f)

            fun glBox(cx: Double, cy: Double, cz: Double, hx: Double, hy: Double, hz: Double,
                      cr: Float, cg: Float, cb: Float, ca: Float) {
                val x0 = cx - hx; val x1 = cx + hx
                val y0 = cy - hy; val y1 = cy + hy
                val z0 = cz - hz; val z1 = cz + hz
                batch.color4f(cr, cg, cb, ca)
                batch.begin(GL2.GL_QUADS)
                // Top (-Y = up)
                batch.vertex3d(x0, y0, z0); batch.vertex3d(x1, y0, z0)
                batch.vertex3d(x1, y0, z1); batch.vertex3d(x0, y0, z1)
                // Bottom
                batch.vertex3d(x0, y1, z0); batch.vertex3d(x0, y1, z1)
                batch.vertex3d(x1, y1, z1); batch.vertex3d(x1, y1, z0)
                // Front (-Z)
                batch.color4f(cr * 0.85f, cg * 0.85f, cb * 0.85f, ca)
                batch.vertex3d(x0, y0, z0); batch.vertex3d(x0, y1, z0)
                batch.vertex3d(x1, y1, z0); batch.vertex3d(x1, y0, z0)
                // Back (+Z)
                batch.vertex3d(x0, y0, z1); batch.vertex3d(x1, y0, z1)
                batch.vertex3d(x1, y1, z1); batch.vertex3d(x0, y1, z1)
                // Left (-X)
                batch.color4f(cr * 0.75f, cg * 0.75f, cb * 0.75f, ca)
                batch.vertex3d(x0, y0, z0); batch.vertex3d(x0, y0, z1)
                batch.vertex3d(x0, y1, z1); batch.vertex3d(x0, y1, z0)
                // Right (+X)
                batch.vertex3d(x1, y0, z0); batch.vertex3d(x1, y1, z0)
                batch.vertex3d(x1, y1, z1); batch.vertex3d(x1, y0, z1)
                batch.end()
            }

            val a = pulse * 0.9f

            // Body (main torso)
            glBox(0.0, -18.0, 0.0,  10.0, 6.0, 16.0, r, g, b, a)
            // Belly (lighter underside strip)
            glBox(0.0, -12.5, 0.0,  8.0, 0.8, 14.0, lr, lg, lb, a)

            // Head
            glBox(0.0, -28.0, -18.0,  7.0, 7.0, 7.0, r, g, b, a)
            // Snout
            glBox(0.0, -25.0, -28.0,  3.5, 3.0, 4.0, lr, lg, lb, a)
            // Nose
            glBox(0.0, -26.0, -32.5,  1.5, 1.5, 0.8, 0.1f, 0.1f, 0.1f, a)
            // Eyes
            glBox(-3.5, -30.0, -25.5,  1.2, 1.2, 0.5, 1f, 1f, 1f, a)
            glBox(3.5, -30.0, -25.5,   1.2, 1.2, 0.5, 1f, 1f, 1f, a)
            // Pupils
            glBox(-3.5, -30.0, -26.2,  0.7, 0.7, 0.3, 0.05f, 0.05f, 0.05f, a)
            glBox(3.5, -30.0, -26.2,   0.7, 0.7, 0.3, 0.05f, 0.05f, 0.05f, a)

            // Ears (floppy — slightly darker)
            val er = (r * 0.6f); val eg = (g * 0.6f); val eb = (b * 0.6f)
            glBox(-6.0, -33.0, -15.0,  2.5, 5.0, 2.0, er, eg, eb, a)
            glBox(6.0, -33.0, -15.0,   2.5, 5.0, 2.0, er, eg, eb, a)

            // Front legs
            glBox(-6.0, -6.0, -12.0,  3.0, 6.0, 3.0, r, g, b, a)
            glBox(6.0, -6.0, -12.0,   3.0, 6.0, 3.0, r, g, b, a)
            // Back legs
            glBox(-6.0, -6.0, 12.0,   3.0, 6.0, 3.0, r, g, b, a)
            glBox(6.0, -6.0, 12.0,    3.0, 6.0, 3.0, r, g, b, a)
            // Paws (lighter)
            glBox(-6.0, -1.0, -12.0,  3.2, 1.0, 3.5, lr, lg, lb, a)
            glBox(6.0, -1.0, -12.0,   3.2, 1.0, 3.5, lr, lg, lb, a)
            glBox(-6.0, -1.0, 12.0,   3.2, 1.0, 3.5, lr, lg, lb, a)
            glBox(6.0, -1.0, 12.0,    3.2, 1.0, 3.5, lr, lg, lb, a)

            // Tail (wagging! separate sine)
            val wag = Math.sin(time * 0.008) * 20.0
            batch.flush(gl)  // flush before tail matrix
            gl.glPushMatrix()
            gl.glTranslated(0.0, -22.0, 16.0)
            gl.glRotated(-45.0, 1.0, 0.0, 0.0)  // tail angles up
            gl.glRotated(wag, 0.0, 0.0, 1.0)     // wag side to side
            glBox(0.0, -7.0, 0.0,  2.0, 7.0, 2.0, r, g, b, a)
            batch.flush(gl)  // flush tail geometry
            gl.glPopMatrix()
            batch.flush(gl)  // flush dog geometry

            gl.glPopMatrix()
        }

        // ── PoE Beam: tapered 3D beam with glowing orb at base ──
        if (beamEffects["poeBeam"] == true) {
            val aPulse = 0.65f + 0.35f * sin(time * 0.003).toFloat()
            batch.flush(gl)  // flush before additive blend
            gl.glEnable(GL2.GL_BLEND)
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE)  // additive

            val heightSegs = 48
            val circSegs = 8  // vertices around circumference
            val H = BEAM_WORLD_HEIGHT
            val baseW = BEAM_BASE_RADIUS * 3.5

            // Trumpet taper: wide bell base narrowing to needle
            fun trumpetR(f: Double): Double {
                val narrow = Math.pow(1.0 - f, 0.6)
                val flare  = 1.0 + 3.0 * Math.exp(-6.0 * f)
                return baseW * narrow * flare * 0.25
            }

            // Precompute circle unit vectors
            val cos = DoubleArray(circSegs + 1) { Math.cos(it * 2.0 * Math.PI / circSegs) }
            val sin2 = DoubleArray(circSegs + 1) { Math.sin(it * 2.0 * Math.PI / circSegs) }

            // Outer glow cylinder
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs
                val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 1.6; val r1 = trumpetR(f1) * 1.6
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.8) * aPulse * 0.18).toFloat()
                val a1 = ((1.0 - f1 * 0.8) * aPulse * 0.18).toFloat()
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(r, g, b, a0)
                    batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(r, g, b, a1)
                    batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }

            // Core beam cylinder (brighter, narrower)
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs
                val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 0.7; val r1 = trumpetR(f1) * 0.7
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.5) * aPulse * 0.7).toFloat()
                val a1 = ((1.0 - f1 * 0.5) * aPulse * 0.7).toFloat()
                val lr = r + (1f - r) * 0.35f; val lg = g + (1f - g) * 0.35f; val lb = b + (1f - b) * 0.35f
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(lr, lg, lb, a0)
                    batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(lr, lg, lb, a1)
                    batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }

            // Hot center cylinder (white, very narrow)
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs
                val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 0.15; val r1 = trumpetR(f1) * 0.15
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.4) * aPulse * 0.5).toFloat()
                val a1 = ((1.0 - f1 * 0.4) * aPulse * 0.5).toFloat()
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(1f, 1f, 1f, a0)
                    batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(1f, 1f, 1f, a1)
                    batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }

            // Glowing orb at base (proper sphere)
            val orbR = BEAM_BASE_RADIUS * 2.2
            val orbCY = dy - orbR * 0.7
            val orbP = 0.7f + 0.3f * sin(time * 0.004).toFloat()
            val latSegs = 6; val lonSegs = 8

            // Outer glow sphere
            for (i in 0 until latSegs) {
                val lat0 = i.toDouble() / latSegs * Math.PI
                val lat1 = (i + 1).toDouble() / latSegs * Math.PI
                val sr0 = orbR * Math.sin(lat0); val sr1 = orbR * Math.sin(lat1)
                val sy0 = orbCY - orbR * Math.cos(lat0); val sy1 = orbCY - orbR * Math.cos(lat1)
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..lonSegs) {
                    val lon = j.toDouble() / lonSegs * 2.0 * Math.PI
                    val cx = Math.cos(lon); val sz = Math.sin(lon)
                    batch.color4f(r, g, b, orbP * 0.25f)
                    batch.vertex3d(dx + cx * sr0, sy0, dz + sz * sr0)
                    batch.vertex3d(dx + cx * sr1, sy1, dz + sz * sr1)
                }
                batch.end()
            }
            // Bright core sphere
            val coreR = orbR * 0.45
            for (i in 0 until latSegs) {
                val lat0 = i.toDouble() / latSegs * Math.PI
                val lat1 = (i + 1).toDouble() / latSegs * Math.PI
                val sr0 = coreR * Math.sin(lat0); val sr1 = coreR * Math.sin(lat1)
                val sy0 = orbCY - coreR * Math.cos(lat0); val sy1 = orbCY - coreR * Math.cos(lat1)
                val lr = r + (1f - r) * 0.6f; val lg = g + (1f - g) * 0.6f; val lb = b + (1f - b) * 0.6f
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..lonSegs) {
                    val lon = j.toDouble() / lonSegs * 2.0 * Math.PI
                    val cx = Math.cos(lon); val sz = Math.sin(lon)
                    batch.color4f(lr, lg, lb, orbP * 0.65f)
                    batch.vertex3d(dx + cx * sr0, sy0, dz + sz * sr0)
                    batch.vertex3d(dx + cx * sr1, sy1, dz + sz * sr1)
                }
                batch.end()
            }

            // Ground pool (radial disc)
            val poolR = BEAM_BASE_RADIUS * 4.5
            batch.begin(GL2.GL_TRIANGLE_FAN)
            batch.color4f(r, g, b, orbP * 0.3f)
            batch.vertex3d(dx, dy, dz)
            for (j in 0..24) { val a = j * 2.0 * Math.PI / 24; batch.color4f(r, g, b, 0f); batch.vertex3d(dx + Math.cos(a) * poolR, dy, dz + Math.sin(a) * poolR) }
            batch.end()

            batch.flush(gl)  // flush additive geometry
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA)  // restore standard
        }

        // ── Helix Beam: 3D tapered beam with sparse double-helix diamonds ──
        if (beamEffects["helixBeam"] == true) {
            val aPulse = 0.6f + 0.4f * sin(time * 0.003).toFloat()
            batch.flush(gl)  // flush before additive blend
            gl.glEnable(GL2.GL_BLEND)
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE)

            val heightSegs = 48
            val circSegs = 8
            val H = BEAM_WORLD_HEIGHT
            val baseW = BEAM_BASE_RADIUS * 3.0

            fun trumpetR(f: Double): Double {
                val narrow = Math.pow(1.0 - f, 0.55)
                val flare  = 1.0 + 4.0 * Math.exp(-5.0 * f)
                return baseW * narrow * flare * 0.2
            }

            val cos = DoubleArray(circSegs + 1) { Math.cos(it * 2.0 * Math.PI / circSegs) }
            val sin2 = DoubleArray(circSegs + 1) { Math.sin(it * 2.0 * Math.PI / circSegs) }

            // Outer glow cylinder
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs; val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 1.5; val r1 = trumpetR(f1) * 1.5
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.85) * aPulse * 0.2).toFloat()
                val a1 = ((1.0 - f1 * 0.85) * aPulse * 0.2).toFloat()
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(r, g, b, a0); batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(r, g, b, a1); batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }
            // Core cylinder
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs; val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 0.6; val r1 = trumpetR(f1) * 0.6
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.4) * aPulse * 0.65).toFloat()
                val a1 = ((1.0 - f1 * 0.4) * aPulse * 0.65).toFloat()
                val lr = r + (1f - r) * 0.3f; val lg = g + (1f - g) * 0.3f; val lb = b + (1f - b) * 0.3f
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(lr, lg, lb, a0); batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(lr, lg, lb, a1); batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }
            // Bright center cylinder
            for (i in 0 until heightSegs) {
                val f0 = i.toDouble() / heightSegs; val f1 = (i + 1).toDouble() / heightSegs
                val r0 = trumpetR(f0) * 0.12; val r1 = trumpetR(f1) * 0.12
                val y0 = dy - f0 * H; val y1 = dy - f1 * H
                val a0 = ((1.0 - f0 * 0.3) * aPulse * 0.4).toFloat()
                val a1 = ((1.0 - f1 * 0.3) * aPulse * 0.4).toFloat()
                batch.begin(GL2.GL_TRIANGLE_STRIP)
                for (j in 0..circSegs) {
                    batch.color4f(1f, 1f, 1f, a0); batch.vertex3d(dx + cos[j] * r0, y0, dz + sin2[j] * r0)
                    batch.color4f(1f, 1f, 1f, a1); batch.vertex3d(dx + cos[j] * r1, y1, dz + sin2[j] * r1)
                }
                batch.end()
            }

            // ── Double helix: sparse diamonds on helix curves ──
            val helixSpeed = time * 0.0012
            val helixTurns = 2.5
            val diamondsPerStrand = 7

            for (strand in 0..1) {
                val offset = strand * Math.PI

                // Thin helix curve
                gl.glLineWidth(1.0f)
                batch.begin(GL2.GL_LINE_STRIP)
                for (i in 0..120) {
                    val f = i.toDouble() / 120
                    val angle = helixSpeed + f * helixTurns * 2.0 * Math.PI + offset
                    val orbitR = trumpetR(f) * 2.2 + 3.0
                    val hx = dx + Math.cos(angle) * orbitR
                    val hy = dy - f * H
                    val hz = dz + Math.sin(angle) * orbitR
                    batch.color4f(r * 0.7f, g * 0.7f, b * 0.7f, ((1.0 - f * 0.7) * aPulse * 0.25).toFloat())
                    batch.vertex3d(hx, hy, hz)
                }
                batch.end()

                // Diamonds along helix
                for (d in 0 until diamondsPerStrand) {
                    val f = (d + 0.5) / diamondsPerStrand.toDouble()
                    if (f > 0.92) continue
                    val angle = helixSpeed + f * helixTurns * 2.0 * Math.PI + offset
                    val orbitR = trumpetR(f) * 2.2 + 3.0
                    val ddx = dx + Math.cos(angle) * orbitR
                    val ddy = dy - f * H
                    val ddz = dz + Math.sin(angle) * orbitR
                    val alpha = ((1.0 - f * 0.5) * aPulse * 0.55).toFloat()
                    val sz = (6.0 - f * 3.0).coerceAtLeast(2.0)

                    // Face diamond toward beam center
                    val toCx = dx - ddx; val toCz = dz - ddz
                    val td = Math.sqrt(toCx * toCx + toCz * toCz).coerceAtLeast(0.01)
                    val nx = toCx / td; val nz = toCz / td
                    val tx = -nz; val tz = nx

                    // 3D diamond — 8 triangles forming an octahedron
                    val dh = sz * 1.6  // vertical half-extent
                    val dw = sz        // horizontal half-extent
                    val dd = sz * 0.4  // depth half-extent

                    // 6 vertices: top, bottom, +X, -X, +N, -N
                    val topY = ddy - dh; val botY = ddy + dh
                    val px = ddx + tx * dw; val pz = ddz + tz * dw   // +tangent
                    val mx = ddx - tx * dw; val mz = ddz - tz * dw   // -tangent
                    val fx = ddx + nx * dd; val fz = ddz + nz * dd   // +normal (toward beam)
                    val bkx = ddx - nx * dd; val bkz = ddz - nz * dd // -normal (away from beam)

                    // Outer diamond (8 faces)
                    batch.color4f(r * 0.5f, g * 0.5f, b * 0.5f, alpha * 0.4f)
                    batch.begin(GL2.GL_TRIANGLES)
                    // Top 4 faces
                    batch.vertex3d(ddx, topY, ddz); batch.vertex3d(px, ddy, pz); batch.vertex3d(fx, ddy, fz)
                    batch.vertex3d(ddx, topY, ddz); batch.vertex3d(fx, ddy, fz); batch.vertex3d(mx, ddy, mz)
                    batch.vertex3d(ddx, topY, ddz); batch.vertex3d(mx, ddy, mz); batch.vertex3d(bkx, ddy, bkz)
                    batch.vertex3d(ddx, topY, ddz); batch.vertex3d(bkx, ddy, bkz); batch.vertex3d(px, ddy, pz)
                    // Bottom 4 faces
                    batch.vertex3d(ddx, botY, ddz); batch.vertex3d(fx, ddy, fz); batch.vertex3d(px, ddy, pz)
                    batch.vertex3d(ddx, botY, ddz); batch.vertex3d(mx, ddy, mz); batch.vertex3d(fx, ddy, fz)
                    batch.vertex3d(ddx, botY, ddz); batch.vertex3d(bkx, ddy, bkz); batch.vertex3d(mx, ddy, mz)
                    batch.vertex3d(ddx, botY, ddz); batch.vertex3d(px, ddy, pz); batch.vertex3d(bkx, ddy, bkz)
                    batch.end()

                    // Inner bright octahedron (smaller)
                    val isz = 0.45
                    val idh = dh * isz; val idw = dw * isz; val idd = dd * isz
                    val ipx = ddx + tx * idw; val ipz = ddz + tz * idw
                    val imx = ddx - tx * idw; val imz = ddz - tz * idw
                    val ifx = ddx + nx * idd; val ifz = ddz + nz * idd
                    val ibx = ddx - nx * idd; val ibz = ddz - nz * idd
                    val itY = ddy - idh; val ibY = ddy + idh
                    val lr = r + (1f - r) * 0.5f; val lg = g + (1f - g) * 0.5f; val lb = b + (1f - b) * 0.5f
                    batch.color4f(lr, lg, lb, alpha * 0.85f)
                    batch.begin(GL2.GL_TRIANGLES)
                    batch.vertex3d(ddx, itY, ddz); batch.vertex3d(ipx, ddy, ipz); batch.vertex3d(ifx, ddy, ifz)
                    batch.vertex3d(ddx, itY, ddz); batch.vertex3d(ifx, ddy, ifz); batch.vertex3d(imx, ddy, imz)
                    batch.vertex3d(ddx, itY, ddz); batch.vertex3d(imx, ddy, imz); batch.vertex3d(ibx, ddy, ibz)
                    batch.vertex3d(ddx, itY, ddz); batch.vertex3d(ibx, ddy, ibz); batch.vertex3d(ipx, ddy, ipz)
                    batch.vertex3d(ddx, ibY, ddz); batch.vertex3d(ifx, ddy, ifz); batch.vertex3d(ipx, ddy, ipz)
                    batch.vertex3d(ddx, ibY, ddz); batch.vertex3d(imx, ddy, imz); batch.vertex3d(ifx, ddy, ifz)
                    batch.vertex3d(ddx, ibY, ddz); batch.vertex3d(ibx, ddy, ibz); batch.vertex3d(imx, ddy, imz)
                    batch.vertex3d(ddx, ibY, ddz); batch.vertex3d(ipx, ddy, ipz); batch.vertex3d(ibx, ddy, ibz)
                    batch.end()
                }
            }

            // Center diamond (prominent, near base) — proper octahedron
            val cdBob = Math.sin(time * 0.003) * 3.0
            val cdY = dy - H * 0.08 + cdBob
            val cdSz = 7.0; val cdD = 4.0
            val lr = r + (1f - r) * 0.6f; val lg = g + (1f - g) * 0.6f; val lb = b + (1f - b) * 0.6f
            batch.color4f(lr, lg, lb, aPulse * 0.8f)
            batch.begin(GL2.GL_TRIANGLES)
            // Top 4
            batch.vertex3d(dx, cdY - cdSz * 1.5, dz); batch.vertex3d(dx + cdSz, cdY, dz); batch.vertex3d(dx, cdY, dz + cdD)
            batch.vertex3d(dx, cdY - cdSz * 1.5, dz); batch.vertex3d(dx, cdY, dz + cdD); batch.vertex3d(dx - cdSz, cdY, dz)
            batch.vertex3d(dx, cdY - cdSz * 1.5, dz); batch.vertex3d(dx - cdSz, cdY, dz); batch.vertex3d(dx, cdY, dz - cdD)
            batch.vertex3d(dx, cdY - cdSz * 1.5, dz); batch.vertex3d(dx, cdY, dz - cdD); batch.vertex3d(dx + cdSz, cdY, dz)
            // Bottom 4
            batch.vertex3d(dx, cdY + cdSz * 1.5, dz); batch.vertex3d(dx, cdY, dz + cdD); batch.vertex3d(dx + cdSz, cdY, dz)
            batch.vertex3d(dx, cdY + cdSz * 1.5, dz); batch.vertex3d(dx - cdSz, cdY, dz); batch.vertex3d(dx, cdY, dz + cdD)
            batch.vertex3d(dx, cdY + cdSz * 1.5, dz); batch.vertex3d(dx, cdY, dz - cdD); batch.vertex3d(dx - cdSz, cdY, dz)
            batch.vertex3d(dx, cdY + cdSz * 1.5, dz); batch.vertex3d(dx + cdSz, cdY, dz); batch.vertex3d(dx, cdY, dz - cdD)
            batch.end()

            // Small floating side diamond
            val sdAngle = helixSpeed * 0.8
            val sdDist = trumpetR(0.0) * 2.5 + 8.0
            val sdx = dx + Math.cos(sdAngle) * sdDist
            val sdz = dz + Math.sin(sdAngle) * sdDist
            val sdBob = Math.sin(time * 0.0035 + 1.0) * 2.0
            val sdSz = 3.5; val sdD = 2.0
            val sdCY = dy + sdBob
            batch.color4f(lr, lg, lb, aPulse * 0.6f)
            batch.begin(GL2.GL_TRIANGLES)
            batch.vertex3d(sdx, sdCY - sdSz * 1.5, sdz); batch.vertex3d(sdx + sdSz, sdCY, sdz); batch.vertex3d(sdx, sdCY, sdz + sdD)
            batch.vertex3d(sdx, sdCY - sdSz * 1.5, sdz); batch.vertex3d(sdx, sdCY, sdz + sdD); batch.vertex3d(sdx - sdSz, sdCY, sdz)
            batch.vertex3d(sdx, sdCY - sdSz * 1.5, sdz); batch.vertex3d(sdx - sdSz, sdCY, sdz); batch.vertex3d(sdx, sdCY, sdz - sdD)
            batch.vertex3d(sdx, sdCY - sdSz * 1.5, sdz); batch.vertex3d(sdx, sdCY, sdz - sdD); batch.vertex3d(sdx + sdSz, sdCY, sdz)
            batch.vertex3d(sdx, sdCY + sdSz * 1.5, sdz); batch.vertex3d(sdx, sdCY, sdz + sdD); batch.vertex3d(sdx + sdSz, sdCY, sdz)
            batch.vertex3d(sdx, sdCY + sdSz * 1.5, sdz); batch.vertex3d(sdx - sdSz, sdCY, sdz); batch.vertex3d(sdx, sdCY, sdz + sdD)
            batch.vertex3d(sdx, sdCY + sdSz * 1.5, sdz); batch.vertex3d(sdx, sdCY, sdz - sdD); batch.vertex3d(sdx - sdSz, sdCY, sdz)
            batch.vertex3d(sdx, sdCY + sdSz * 1.5, sdz); batch.vertex3d(sdx + sdSz, sdCY, sdz); batch.vertex3d(sdx, sdCY, sdz - sdD)
            batch.end()

            // Base pool
            val poolR = trumpetR(0.0) * 3.0
            batch.begin(GL2.GL_TRIANGLE_FAN)
            batch.color4f(r, g, b, aPulse * 0.22f)
            batch.vertex3d(dx, dy, dz)
            for (j in 0..28) { val a = j * 2.0 * Math.PI / 28; batch.color4f(r, g, b, 0f); batch.vertex3d(dx + Math.cos(a) * poolR, dy, dz + Math.sin(a) * poolR) }
            batch.end()
            val ipR = poolR * 0.4
            batch.begin(GL2.GL_TRIANGLE_FAN)
            batch.color4f(r + (1f - r) * 0.3f, g + (1f - g) * 0.3f, b + (1f - b) * 0.3f, aPulse * 0.35f)
            batch.vertex3d(dx, dy, dz)
            for (j in 0..20) { val a = j * 2.0 * Math.PI / 20; batch.color4f(r, g, b, 0f); batch.vertex3d(dx + Math.cos(a) * ipR, dy, dz + Math.sin(a) * ipR) }
            batch.end()

            batch.flush(gl)  // flush additive geometry
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA)  // restore standard
        }

    }

    // ─── Depth-based occlusion for 2D effects ──────────────────────────────────
    // GL beams use the depth buffer for occlusion, but 2D effects (pool, runes,
    // sparkles etc.) are software-rasterized and bypass it entirely. This reads
    // the depth buffer at the tile center to determine if geometry is blocking.

    private fun isTileOccludedByDepth(screenX: Int, screenY: Int, tileX: Int, tileZ: Int): Boolean {
        if (!beamOcclusion || !API.IsHD()) return false
        val gl = GlRenderer.gl ?: return false
        if (screenX < 0 || screenY < 0) return true

        try {
            val canvasW = GameShell.canvasWidth
            val canvasH = GameShell.canvasHeight
            if (screenX >= canvasW || screenY >= canvasH) return true

            // Read depth buffer at tile center (GL Y is bottom-up)
            val depthBuf = java.nio.ByteBuffer.allocateDirect(4)
                .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            gl.glReadPixels(screenX, canvasH - screenY - 1, 1, 1,
                GL2.GL_DEPTH_COMPONENT, GL2.GL_FLOAT, depthBuf)
            val storedDepth = depthBuf.get(0)

            // Far plane (1.0) = sky/nothing blocking
            if (storedDepth >= 0.999f) return false

            // Compute eye-space depth of tile center via modelview rotation
            val camX = Camera.renderX.toDouble()
            val camY = Camera.renderY.toDouble()
            val camZ = Camera.renderZ.toDouble()
            val worldX = ((tileX shl 7) + 64).toDouble()
            val worldZ = ((tileZ shl 7) + 64).toDouble()
            val xFine = (tileX shl 7) + 64
            val zFine = (tileZ shl 7) + 64
            val groundY = SceneGraph.getTileHeight(Player.plane, xFine, zFine).toDouble()

            val dx = worldX - camX
            val dy = groundY - camY
            val dz = worldZ - camZ

            // Replicate modelview: RotateX(180+pitch) * RotateY(yaw)
            val totalPitchRad = (180.0 + Camera.cameraPitch * 0.17578125) * Math.PI / 180.0
            val yawRad = Camera.cameraYaw * 0.17578125 * Math.PI / 180.0

            // RotateY(yaw) applied first (rightmost)
            val z1 = -dx * Math.sin(yawRad) + dz * Math.cos(yawRad)
            // RotateX(totalPitch) — only need Z component
            val eyeZ = dy * Math.sin(totalPitchRad) + z1 * Math.cos(totalPitchRad)

            val dist = -eyeZ  // positive = in front of camera
            if (dist <= 50.0) return false  // closer than near plane

            // Expected depth buffer value for this camera→tile distance
            val near = 50.0
            val far = 3_000_000.0
            val expectedDepth = (far * (dist - near)) / (dist * (far - near))

            // Occluded if stored depth is significantly closer than expected
            return storedDepth < expectedDepth - 0.001
        } catch (_: Exception) {
            return false
        }
    }

    // ─── Loot Beam (2D software mode) ──────────────────────────────────────────

    private fun drawLootBeam3D(tileX: Int, tileY: Int, color: Int, itemId: Int, qty: Int) {
        val cx = (tileX shl 7) + 64
        val cz = (tileY shl 7) + 64

        val time  = System.currentTimeMillis()
        val pulse = 0.75f + 0.25f * sin(time * 0.003).toFloat()

        val r = (color shr 16) and 0xFF
        val g = (color shr 8)  and 0xFF
        val b =  color         and 0xFF
        val glowColor = (
                ((r + (255 - r) * 0.5f).toInt().coerceAtMost(255) shl 16) or
                        ((g + (255 - g) * 0.5f).toInt().coerceAtMost(255) shl 8) or
                        (b + (255 - b) * 0.5f).toInt().coerceAtMost(255)
                )

        // 2D beam column — gated by beamColumnEnabled
        if (beamColumnEnabled) {
            val SIDES  = 10
            val steps  = 30
            val worldH = BEAM_WORLD_HEIGHT
            val baseR  = BEAM_BASE_RADIUS.toFloat()

            for (i in 0 until steps) {
                val botFrac = i.toFloat() / steps
                val topFrac = (i + 1).toFloat() / steps

                val botR  = (baseR * Math.pow(1.0 - botFrac, 8.0)).toFloat().coerceAtMost(64f)
                val topR  = (baseR * Math.pow(1.0 - topFrac, 8.0)).toFloat().coerceAtMost(64f)
                val botRg = (botR * 1.8f).coerceAtMost(64f)
                val topRg = (topR * 1.8f).coerceAtMost(64f)
                if (topR < 0.3f) continue

                val botY = (botFrac * worldH).toInt()
                val topY = (topFrac * worldH).toInt()

                val coreAlpha = ((1f - botFrac * 0.35f) * pulse * 230f).toInt().coerceIn(0, 230)
                val glowAlpha = (coreAlpha * 0.18f).toInt()

                val botCore = Array(SIDES) { s ->
                    val a = s * 2.0 * Math.PI / SIDES
                    API.CalculateSceneGraphScreenPosition(
                        cx + (Math.cos(a) * botR).toInt(),
                        cz + (Math.sin(a) * botR).toInt(), botY)
                }
                val topCore = Array(SIDES) { s ->
                    val a = s * 2.0 * Math.PI / SIDES
                    API.CalculateSceneGraphScreenPosition(
                        cx + (Math.cos(a) * topR).toInt(),
                        cz + (Math.sin(a) * topR).toInt(), topY)
                }
                val botGlow = Array(SIDES) { s ->
                    val a = s * 2.0 * Math.PI / SIDES
                    API.CalculateSceneGraphScreenPosition(
                        cx + (Math.cos(a) * botRg).toInt(),
                        cz + (Math.sin(a) * botRg).toInt(), botY)
                }
                val topGlow = Array(SIDES) { s ->
                    val a = s * 2.0 * Math.PI / SIDES
                    API.CalculateSceneGraphScreenPosition(
                        cx + (Math.cos(a) * topRg).toInt(),
                        cz + (Math.sin(a) * topRg).toInt(), topY)
                }

                for (s in 0 until SIDES) {
                    val s2 = (s + 1) % SIDES
                    if (glowAlpha > 0)
                        fillQuad(botGlow[s][0], botGlow[s][1], botGlow[s2][0], botGlow[s2][1],
                            topGlow[s2][0], topGlow[s2][1], topGlow[s][0], topGlow[s][1],
                            glowColor, glowAlpha)
                    if (coreAlpha > 0)
                        fillQuad(botCore[s][0], botCore[s][1], botCore[s2][0], botCore[s2][1],
                            topCore[s2][0], topCore[s2][1], topCore[s][0], topCore[s][1],
                            color, coreAlpha)
                }
            }
        }

        // ── 2D beam effects — software mode fallback only ──
        // In HD mode, drawTileEffectsGL handles these in GL with proper depth occlusion.
        // In software mode, draw the 2D versions here.
        if (!API.IsHD()) {
            val tileKey = "$tileX,$tileY"

            if (beamEffects["pool"] == true) drawBeamGroundPool(cx, cz, color, glowColor, pulse)
            if (beamEffects["invertCone"] == true) drawInvertedCone(cx, cz, color, glowColor, pulse)
            if (beamEffects["runes"] == true) drawOrbitRunes(cx, cz, color)
            if (beamEffects["pulseRings"] == true) drawPulseRings(cx, cz, color, tileKey)
            if (beamEffects["tileHighlight"] == true) drawTileHighlight(tileX, tileY, color)
            if (beamEffects["pentagram"] == true) drawPentagram(cx, cz, color)
            if (beamEffects["sparkles"] == true) drawWorldSparkles(cx, cz, color, tileKey)

            val center = API.CalculateSceneGraphScreenPosition(cx, cz, 0)
            if (center[0] >= 0 && center[1] >= 0) {
                if (beamEffects["arrows"] == true) drawChevronArrow(cx, cz, color)

                if (beamEffects["icon"] == true) {
                    val sprite = API.GetObjSprite(itemId, qty, false, 0, 180)
                    if (sprite != null) {
                        val bobOffset = (Math.sin(time * 0.0018) * 6).toInt()
                        val iconX = center[0] - sprite.width / 2
                        val iconY = center[1] - (BEAM_BASE_RADIUS * 3).toInt() - sprite.height + bobOffset
                        sprite.renderAlpha(iconX, iconY, 200)
                    }
                }
            }
        }
    }

    // Fills a screen-space polygon using horizontal scanlines
    private fun fillPoly(xs: IntArray, ys: IntArray, color: Int, alpha: Int) {
        if (alpha <= 0) return
        val n    = xs.size
        val minY = ys.minOrNull() ?: return
        val maxY = ys.maxOrNull() ?: return
        for (y in minY..maxY) {
            var minX = Int.MAX_VALUE
            var maxX = Int.MIN_VALUE
            for (i in 0 until n) {
                val j  = (i + 1) % n
                val ay = ys[i]; val by = ys[j]
                if ((ay <= y && by >= y) || (by <= y && ay >= y)) {
                    if (ay == by) {
                        minX = minOf(minX, xs[i], xs[j])
                        maxX = maxOf(maxX, xs[i], xs[j])
                    } else {
                        val x = xs[i] + (y - ay).toLong() * (xs[j] - xs[i]) / (by - ay)
                        minX = minOf(minX, x.toInt())
                        maxX = maxOf(maxX, x.toInt())
                    }
                }
            }
            if (minX <= maxX) FillRect(minX, y, (maxX - minX + 1).coerceAtLeast(1), 1, color, alpha)
        }
    }

    private fun fillQuad(
        x0: Int, y0: Int, x1: Int, y1: Int,
        x2: Int, y2: Int, x3: Int, y3: Int,
        color: Int, alpha: Int
    ) = fillPoly(intArrayOf(x0, x1, x2, x3), intArrayOf(y0, y1, y2, y3), color, alpha)

    private fun drawBeamGroundPool(cx: Int, cz: Int, color: Int, glowColor: Int, pulse: Float) {
        val SIDES = 16
        fun ring(r: Int, col: Int, alpha: Int) {
            if (alpha <= 0) return
            val pts = Array(SIDES) { s ->
                val a = s * 2.0 * Math.PI / SIDES
                API.CalculateSceneGraphScreenPosition(
                    cx + (Math.cos(a) * r).toInt(),
                    cz + (Math.sin(a) * r).toInt(), 0)
            }
            if (pts.any { it[0] < 0 }) return
            fillPoly(IntArray(SIDES) { pts[it][0] }, IntArray(SIDES) { pts[it][1] }, col, alpha)
        }
        ring(minOf((BEAM_BASE_RADIUS * 4.0).toInt(), 60), glowColor, (pulse * 40f).toInt())
        ring(minOf((BEAM_BASE_RADIUS * 2.8).toInt(), 52), glowColor, (pulse * 70f).toInt())
        ring(minOf((BEAM_BASE_RADIUS * 1.5).toInt(), 40), color,     (pulse * 150f).toInt())
        ring(minOf((BEAM_BASE_RADIUS * 0.6).toInt(), 16), 0xFFFFFF,  (pulse * 110f).toInt())
    }

    // ─── Beam effect renderers ────────────────────────────────────────────────

    private fun drawInvertedCone(cx: Int, cz: Int, color: Int, glowColor: Int, pulse: Float) {
        val SIDES  = 10
        val steps  = 20
        val height = BEAM_WORLD_HEIGHT.toInt()
        val maxR   = (BEAM_BASE_RADIUS * 1.6f).toFloat()  // wider than main beam
        for (i in 0 until steps) {
            val botFrac = i.toFloat() / steps
            val topFrac = (i + 1).toFloat() / steps
            // Radius grows with height — narrow at ground, wide at top (funnel shape)
            val botR = (maxR * Math.pow(botFrac.toDouble(), 2.0)).toFloat().coerceAtMost(64f)
            val topR = (maxR * Math.pow(topFrac.toDouble(), 2.0)).toFloat().coerceAtMost(64f)
            if (botR < 0.3f && topR < 0.3f) continue
            // Y goes upward — 0 at ground, height at top
            val botY = (botFrac * height).toInt()
            val topY = (topFrac * height).toInt()
            // Brighter at the wide top, fading toward the narrow point
            val alpha = (botFrac * pulse * 220f).toInt().coerceIn(0, 220)
            val gAlpha = (alpha * 0.18f).toInt()
            val botCore = Array(SIDES) { s ->
                val a = s * 2.0 * Math.PI / SIDES
                API.CalculateSceneGraphScreenPosition(cx + (Math.cos(a) * botR).toInt(), cz + (Math.sin(a) * botR).toInt(), botY)
            }
            val topCore = Array(SIDES) { s ->
                val a = s * 2.0 * Math.PI / SIDES
                API.CalculateSceneGraphScreenPosition(cx + (Math.cos(a) * topR).toInt(), cz + (Math.sin(a) * topR).toInt(), topY)
            }
            for (s in 0 until SIDES) {
                val s2 = (s + 1) % SIDES
                if (gAlpha > 0) fillQuad(
                    botCore[s][0], botCore[s][1], botCore[s2][0], botCore[s2][1],
                    topCore[s2][0], topCore[s2][1], topCore[s][0], topCore[s][1], glowColor, gAlpha)
                if (alpha > 0) fillQuad(
                    botCore[s][0], botCore[s][1], botCore[s2][0], botCore[s2][1],
                    topCore[s2][0], topCore[s2][1], topCore[s][0], topCore[s][1], color, alpha)
            }
        }
    }

    private fun drawOrbitRunes(cx: Int, cz: Int, color: Int) {
        val time   = System.currentTimeMillis()
        val spin   = (time % 6000L).toFloat() / 6000f * 2.0 * Math.PI
        val r      = (BEAM_BASE_RADIUS * 2.8).toInt().coerceAtMost(55)
        val count  = 5
        val pulse  = 0.6f + 0.4f * sin(time * 0.004).toFloat()
        val alpha  = (pulse * 200f).toInt()
        val glowA  = (pulse * 80f).toInt()
        val glow   = blendColor(color, 0xFFFFFF, 0.5f)
        for (i in 0 until count) {
            val angle  = spin + i * 2.0 * Math.PI / count
            val wx     = cx + (Math.cos(angle) * r).toInt()
            val wz     = cz + (Math.sin(angle) * r).toInt()
            val sz = 5
            val top = API.CalculateSceneGraphScreenPosition(wx,      wz - sz, 0)
            val bot = API.CalculateSceneGraphScreenPosition(wx,      wz + sz, 0)
            val lft = API.CalculateSceneGraphScreenPosition(wx - sz, wz,      0)
            val rgt = API.CalculateSceneGraphScreenPosition(wx + sz, wz,      0)
            if (top[0] < 0 || bot[0] < 0 || lft[0] < 0 || rgt[0] < 0) continue
            fillQuad(top[0], top[1], rgt[0], rgt[1], bot[0], bot[1], lft[0], lft[1], glow, glowA)
            val mx = (top[0] + bot[0] + lft[0] + rgt[0]) / 4
            val my = (top[1] + bot[1] + lft[1] + rgt[1]) / 4
            fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).toInt()
            val t2 = intArrayOf(lerp(mx, top[0], 0.5f), lerp(my, top[1], 0.5f))
            val b2 = intArrayOf(lerp(mx, bot[0], 0.5f), lerp(my, bot[1], 0.5f))
            val l2 = intArrayOf(lerp(mx, lft[0], 0.5f), lerp(my, lft[1], 0.5f))
            val r2 = intArrayOf(lerp(mx, rgt[0], 0.5f), lerp(my, rgt[1], 0.5f))
            fillQuad(t2[0], t2[1], r2[0], r2[1], b2[0], b2[1], l2[0], l2[1], 0xFFFFFF, alpha)
        }
    }

    private fun drawPulseRings(cx: Int, cz: Int, color: Int, tileKey: String) {
        val now = System.currentTimeMillis()
        if ((now - (ringLastSpawn[tileKey] ?: 0L)) > 800L) {
            pulseRings.add(PulseRing(tileKey, cx, cz, color, now))
            ringLastSpawn[tileKey] = now
        }
        val dead = mutableListOf<PulseRing>()
        val ringDuration = 1600L
        for (ring in pulseRings) {
            if (ring.tileKey != tileKey) continue
            val age   = now - ring.spawnTime
            if (age > ringDuration) { dead.add(ring); continue }
            val frac  = age.toFloat() / ringDuration
            val r     = (frac * BEAM_BASE_RADIUS * 6.0).toInt().coerceAtMost(62)
            val alpha = ((1f - frac) * (1f - frac) * 180f).toInt().coerceIn(0, 180)
            if (alpha <= 0) continue
            val SIDES = 16
            val outerR = r
            val innerR = (r * 0.82f).toInt()
            val outerPts = Array(SIDES) { s ->
                val a = s * 2.0 * Math.PI / SIDES
                API.CalculateSceneGraphScreenPosition(ring.cx + (Math.cos(a) * outerR).toInt(), ring.cz + (Math.sin(a) * outerR).toInt(), 0)
            }
            val innerPts = Array(SIDES) { s ->
                val a = s * 2.0 * Math.PI / SIDES
                API.CalculateSceneGraphScreenPosition(ring.cx + (Math.cos(a) * innerR).toInt(), ring.cz + (Math.sin(a) * innerR).toInt(), 0)
            }
            if (outerPts.any { it[0] < 0 } || innerPts.any { it[0] < 0 }) continue
            for (s in 0 until SIDES) {
                val s2 = (s + 1) % SIDES
                fillQuad(outerPts[s][0], outerPts[s][1], outerPts[s2][0], outerPts[s2][1],
                    innerPts[s2][0], innerPts[s2][1], innerPts[s][0], innerPts[s][1],
                    color, alpha)
            }
        }
        pulseRings.removeAll(dead)
    }

    private fun drawTileHighlight(tileX: Int, tileY: Int, color: Int) {
        val pulse = 0.4f + 0.6f * sin(System.currentTimeMillis() * 0.002).toFloat().let { it * it }
        val alpha = (pulse * 90f).toInt().coerceIn(0, 90)
        val tx = tileX shl 7; val tz = tileY shl 7
        val sw = API.CalculateSceneGraphScreenPosition(tx,         tz,         0)
        val se = API.CalculateSceneGraphScreenPosition(tx + 128,   tz,         0)
        val ne = API.CalculateSceneGraphScreenPosition(tx + 128,   tz + 128,   0)
        val nw = API.CalculateSceneGraphScreenPosition(tx,         tz + 128,   0)
        if (sw[0] < 0 || se[0] < 0 || ne[0] < 0 || nw[0] < 0) return
        fillQuad(sw[0], sw[1], se[0], se[1], ne[0], ne[1], nw[0], nw[1], color, alpha)
        val bord = 4
        val glow = blendColor(color, 0xFFFFFF, 0.4f)
        for (s in 0 until 4) {
            val (ax, ay) = when(s) { 0 -> sw; 1 -> se; 2 -> ne; else -> nw }
            val (bx, by) = when(s) { 0 -> se; 1 -> ne; 2 -> nw; else -> sw }
            val dx = bx - ax; val dy = by - ay
            val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(1f)
            val nx = (-dy / len * bord).toInt(); val ny = (dx / len * bord).toInt()
            fillQuad(ax, ay, bx, by, bx + nx, by + ny, ax + nx, ay + ny, glow, (pulse * 180f).toInt().coerceIn(0, 180))
        }
    }

    private fun drawPentagram(cx: Int, cz: Int, color: Int) {
        val time  = System.currentTimeMillis()
        val spin  = (time % 12000L).toFloat() / 12000f * 2.0 * Math.PI
        val pulse = 0.5f + 0.5f * sin(time * 0.002).toFloat()
        val alpha = (pulse * 160f).toInt()
        val gAlpha = (pulse * 60f).toInt()
        val glow  = blendColor(color, 0xFFFFFF, 0.5f)
        val r     = (BEAM_BASE_RADIUS * 2.5).toInt().coerceAtMost(55)
        val innerR = (r * 0.38f).toInt()
        val pts = Array(10) { i ->
            val a   = spin + i * Math.PI / 5.0
            val rad = if (i % 2 == 0) r else innerR
            API.CalculateSceneGraphScreenPosition(
                cx + (Math.cos(a) * rad).toInt(),
                cz + (Math.sin(a) * rad).toInt(), 0)
        }
        if (pts.any { it[0] < 0 }) return
        fillPoly(IntArray(10) { pts[it][0] }, IntArray(10) { pts[it][1] }, color, alpha / 3)
        for (i in 0 until 10) {
            val j  = (i + 1) % 10
            val ax = pts[i][0]; val ay = pts[i][1]
            val bx = pts[j][0]; val by = pts[j][1]
            val dx = bx - ax; val dy = by - ay
            val len = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat().coerceAtLeast(1f)
            val nx = (-dy / len * 1.5f).toInt(); val ny = (dx / len * 1.5f).toInt()
            fillQuad(ax - nx, ay - ny, bx - nx, by - ny, bx + nx, by + ny, ax + nx, ay + ny, glow, gAlpha)
            fillQuad(ax, ay, bx, by, bx + nx, by + ny, ax + nx, ay + ny, 0xFFFFFF, (alpha * 0.3f).toInt())
        }
    }

    private fun drawWorldSparkles(cx: Int, cz: Int, color: Int, tileKey: String) {
        val now = System.currentTimeMillis()
        if ((now - (sparkleLastSpawn[tileKey] ?: 0L)) > 120L) {
            val glow = blendColor(color, 0xFFFFFF, 0.6f)
            repeat(3) {
                val angle = Math.random() * 2.0 * Math.PI
                val dist  = (Math.random() * BEAM_BASE_RADIUS * 1.5).toInt().coerceAtMost(48)
                worldSparkles.add(WorldSparkle(
                    tileKey,
                    cx + (Math.cos(angle) * dist).toInt(),
                    cz + (Math.sin(angle) * dist).toInt(),
                    0f,
                    (18f + Math.random().toFloat() * 24f),
                    if (Math.random() > 0.4f) color else glow,
                    (1.2f + Math.random().toFloat() * 1.2f),
                    (1.2f + Math.random().toFloat() * 1.2f)
                ))
            }
            sparkleLastSpawn[tileKey] = now
        }
        val dt   = 0.016f
        val dead = mutableListOf<WorldSparkle>()
        for (sp in worldSparkles) {
            if (sp.tileKey != tileKey) continue
            sp.life -= dt
            if (sp.life <= 0f) { dead.add(sp); continue }
            sp.wy += sp.speed * dt * 60f
            val pos = API.CalculateSceneGraphScreenPosition(sp.wx, sp.wz, sp.wy.toInt())
            if (pos[0] < 0) continue
            val frac  = (sp.life / sp.maxLife).coerceIn(0f, 1f)
            val alpha = (frac * frac * 220f).toInt().coerceIn(0, 220)
            val size  = (frac * 3f).toInt().coerceAtLeast(1)
            FillRect(pos[0] - size,     pos[1] - size,     size * 2, size * 2, sp.color,  alpha)
            FillRect(pos[0] - size / 2, pos[1] - size / 2, size,     size,     0xFFFFFF, (alpha * 0.5f).toInt())
        }
        worldSparkles.removeAll(dead)
        if (worldSparkles.size > 300) worldSparkles.removeAll { it.tileKey != tileKey }
    }

    private fun drawChevronArrow(cx: Int, cz: Int, color: Int) {
        val time    = System.currentTimeMillis()
        val bob     = (Math.sin(time * 0.003) * 8).toInt()
        val arrowY  = (BEAM_WORLD_HEIGHT * 0.55).toInt() + bob
        val glow    = blendColor(color, 0xFFFFFF, 0.4f)
        val pulse   = 0.7f + 0.3f * sin(time * 0.004).toFloat()
        val alpha   = (pulse * 210f).toInt()
        val w       = (BEAM_BASE_RADIUS * 1.6).toInt().coerceAtMost(28)
        val h       = (w * 0.6f).toInt()
        repeat(2) { layer ->
            val yOff = arrowY - layer * (h + 4)
            val tl  = API.CalculateSceneGraphScreenPosition(cx - w,     cz,      yOff + h)
            val tip = API.CalculateSceneGraphScreenPosition(cx,         cz,      yOff)
            val tr  = API.CalculateSceneGraphScreenPosition(cx + w,     cz,      yOff + h)
            val ilf = API.CalculateSceneGraphScreenPosition(cx - w + 4, cz,      yOff + h)
            val irt = API.CalculateSceneGraphScreenPosition(cx + w - 4, cz,      yOff + h)
            val itp = API.CalculateSceneGraphScreenPosition(cx,         cz,      yOff + 5)
            if (tl[0] < 0 || tip[0] < 0 || tr[0] < 0) return
            val a = if (layer == 0) alpha else (alpha * 0.5f).toInt()
            fillQuad(tl[0], tl[1], tip[0], tip[1], itp[0], itp[1], ilf[0], ilf[1], glow,    (a * 0.4f).toInt())
            fillQuad(tr[0], tr[1], tip[0], tip[1], itp[0], itp[1], irt[0], irt[1], glow,    (a * 0.4f).toInt())
            fillQuad(tl[0], tl[1], tip[0], tip[1], itp[0], itp[1], ilf[0], ilf[1], color,   a)
            fillQuad(tr[0], tr[1], tip[0], tip[1], itp[0], itp[1], irt[0], irt[1], color,   a)
            fillQuad(tl[0], tl[1], tip[0], tip[1], itp[0], itp[1], ilf[0], ilf[1], 0xFFFFFF, (a * 0.15f).toInt())
            fillQuad(tr[0], tr[1], tip[0], tip[1], itp[0], itp[1], irt[0], irt[1], 0xFFFFFF, (a * 0.15f).toInt())
        }
    }

    // ─── Broadcast chat polling ──────────────────────────────────────────────────

    private fun pollBroadcastChat() {
        try {
            val size = rt4.Chat.size
            if (size <= 0) return
            val newestSlot = (rt4.Chat.messageCounter + 99) % 100
            val newestMsg = rt4.Chat.messages[newestSlot]?.toString() ?: return
            val newestSender = rt4.Chat.names[newestSlot]?.toString()?.trim() ?: ""
            val newestKey = "$newestSlot:$newestSender:$newestMsg"
            if (newestKey == lastProcessedMessageId) return
            lastProcessedMessageId = newestKey

            val cleanNew = newestMsg.replace(Regex("<[^>]+>"), "").trim()

            if (cleanNew.equals("droptest", ignoreCase = true)) {
                if (newestSender.isNotBlank()) {
                    broadcastIcons.remove(newestSender)
                    val itemId = findItemIdByName("Elysian spirit shield").takeIf { it >= 0 } ?: 12817
                    broadcastIcons[newestSender] = BroadcastEntry(itemId, 1, System.currentTimeMillis(), 0x88CCFF)
                }
                return
            }

            for (i in 0 until minOf(size, 10)) {
                val scanRaw = rt4.Chat.messages[i]?.toString() ?: continue
                val clean = scanRaw.replace(Regex("<[^>]+>"), "").trim()
                if (!clean.startsWith(BROADCAST_PREFIX)) continue
                val afterNews  = clean.removePrefix(BROADCAST_PREFIX)
                val midIdx     = afterNews.indexOf(BROADCAST_MID)
                if (midIdx < 0) continue
                val playerName = afterNews.substring(0, midIdx).trim()
                val rest       = afterNews.substring(midIdx + BROADCAST_MID.length).trim()
                val xIdx       = rest.indexOf(" x ")
                if (xIdx < 0) continue
                val qty        = rest.substring(0, xIdx).trim().toIntOrNull() ?: 1
                val itemName   = rest.substring(xIdx + 3).trim()
                val itemId     = findItemIdByName(itemName)
                if (itemId < 0) continue
                if (!broadcastIcons.containsKey(playerName)) {
                    broadcastIcons[playerName] = BroadcastEntry(itemId, qty, System.currentTimeMillis(), 0xFF00FF)
                }
            }
        } catch (e: Exception) { /* non-critical */ }
    }

    private fun findItemIdByName(name: String): Int {
        try {
            val cap = rt4.ObjTypeList.capacity
            for (id in 0 until cap) {
                val obj = rt4.ObjTypeList.get(id) ?: continue
                if (obj.name?.toString()?.equals(name, ignoreCase = true) == true) return id
            }
        } catch (e: Exception) { /* ignore */ }
        return -1
    }

    // ─── Active icon + particle renderer ────────────────────────────────────────

    private fun drawActiveIcons() {
        val now = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()

        for ((key, entry) in beamIcons) {
            val age  = now - entry.spawnTime
            if (age > ICON_DURATION_MS) { toRemove.add(key); continue }

            val pos = API.CalculateSceneGraphScreenPosition(
                (entry.tileX shl 7) + 64, (entry.tileY shl 7) + 64, 0)
            if (pos[0] < 0 || pos[1] < 0) continue

            val fadeProgress = ((age - (ICON_DURATION_MS - ICON_FADE_MS)).toFloat() / ICON_FADE_MS).coerceIn(0f, 1f)
            val iconAlpha    = ((1f - fadeProgress) * 220f).toInt().coerceIn(0, 220)
            val pulse        = 0.65f + 0.35f * sin(now * 0.0025).toFloat()

            val sprite = API.GetObjSprite(entry.itemId, entry.qty, false, 0, 160)
            if (sprite != null) {
                val scaledW = sprite.width  * 4
                val scaledH = sprite.height * 4

                if (!entry.particlesSpawned && age > 150L) {
                    val colors = extractParticleColors(sprite, entry.beamColor)
                    spawnParticles(entry, pos[0], pos[1], scaledW, scaledH, colors)
                    entry.particlesSpawned = true
                }

                val bobOffset = (Math.sin(now * 0.0018) * 8).toInt()
                val floatH    = (scaledH * 2.2).toInt()
                val iconX     = pos[0] - scaledW / 2
                val iconY     = pos[1] - floatH + bobOffset

                if (iconAlpha > 0) {
                    if (fadeProgress <= 0f) {
                        sprite.renderResized(iconX, iconY, scaledW, scaledH)
                    } else {
                        val cx = pos[0] - sprite.width  / 2
                        val cy = pos[1] - (sprite.height * 2.2).toInt() + bobOffset
                        sprite.renderAlpha(cx, cy, iconAlpha)
                    }
                }
            }

            val dt = ((now - entry.lastMs).toFloat() / 1000f).coerceAtMost(0.05f)
            entry.lastMs = now
            val deadParticles = mutableListOf<BeamParticle>()
            for (p in entry.particles) {
                p.life -= dt
                if (p.life <= 0f) { deadParticles.add(p); continue }
                p.vy  += 120f * dt
                p.sx  += p.vx * dt
                p.sy  += p.vy * dt
                val pFrac  = (p.life / p.maxLife).coerceIn(0f, 1f)
                val pAlpha = (pFrac * pFrac * 200f * (1f - fadeProgress)).toInt().coerceIn(0, 200)
                val size   = (pFrac * 4f).toInt().coerceAtLeast(1)
                FillRect(p.sx.toInt() - size,     p.sy.toInt() - size,     size * 2,     size * 2,     p.color, pAlpha)
                FillRect(p.sx.toInt() - size / 2, p.sy.toInt() - size / 2, size,         size,         0xFFFFFF, (pAlpha * 0.4f).toInt())
            }
            entry.particles.removeAll(deadParticles)
        }

        toRemove.forEach { beamIcons.remove(it) }
    }

    private fun spawnParticles(
        entry: BeamIconEntry,
        cx: Int, cy: Int,
        iconW: Int, iconH: Int,
        colors: List<Int>
    ) {
        spawnParticles(entry.particles, cx, cy - (iconH * 2.2).toInt(), iconW, iconH, colors)
    }

    private fun spawnParticles(
        list: MutableList<BeamParticle>,
        cx: Int, originY: Int,
        iconW: Int, iconH: Int,
        colors: List<Int>
    ) {
        val count = 22
        val speed = iconW * 0.6f
        for (i in 0 until count) {
            val angle = Math.random() * 2.0 * Math.PI
            val mag   = (0.4f + Math.random().toFloat() * 0.6f) * speed
            val vx    = (Math.cos(angle) * mag).toFloat()
            val vy    = (Math.sin(angle) * mag * 0.5f - speed * 0.5f).toFloat()
            val color = colors[i % colors.size]
            val life  = (1.2f + Math.random().toFloat() * 1.0f)
            list.add(BeamParticle(cx.toFloat(), originY.toFloat(), vx, vy, color, life, life))
        }
    }

    private fun extractParticleColors(sprite: rt4.Sprite, beamColor: Int): List<Int> {
        try {
            if (sprite is rt4.SoftwareSprite) {
                val pixels = sprite.pixels ?: return fallbackColors(beamColor)
                val buckets = mutableMapOf<Int, Int>()
                for (px in pixels) {
                    if (px == 0) continue
                    val key = ((px shr 18) and 0x3F shl 12) or
                            ((px shr 10) and 0x3F shl 6) or
                            ((px shr 2)  and 0x3F)
                    buckets[key] = (buckets[key] ?: 0) + 1
                }
                if (buckets.isEmpty()) return fallbackColors(beamColor)

                val sorted = buckets.entries.sortedByDescending { it.value }
                val total  = sorted.sumOf { it.value }.toFloat()
                val result = mutableListOf<Int>()

                for (entry in sorted.take(5)) {
                    val weight = entry.value / total
                    if (weight < 0.03f) continue
                    val rq = (entry.key shr 12) and 0x3F
                    val gq = (entry.key shr 6)  and 0x3F
                    val bq =  entry.key         and 0x3F
                    val rgb = ((rq shl 2) shl 16) or ((gq shl 2) shl 8) or (bq shl 2)
                    val copies = (weight * 10).toInt().coerceAtLeast(1)
                    repeat(copies) { result.add(rgb) }
                }

                if (result.isNotEmpty()) return result
            }
        } catch (e: Exception) { /* fall through */ }
        return fallbackColors(beamColor)
    }

    private fun fallbackColors(beamColor: Int): List<Int> {
        val r = (beamColor shr 16) and 0xFF
        val g = (beamColor shr 8)  and 0xFF
        val b =  beamColor         and 0xFF
        return listOf(
            beamColor,
            ((r * 1.3f).toInt().coerceAtMost(255) shl 16) or ((g * 1.3f).toInt().coerceAtMost(255) shl 8) or (b * 1.3f).toInt().coerceAtMost(255),
            ((r * 0.7f).toInt() shl 16) or ((g * 0.7f).toInt() shl 8) or (b * 0.7f).toInt(),
            beamColor, 0xFFFFFF
        )
    }

    // ─── 2D beam fallback (software mode) ────────────────────────────────────────

    private fun drawLootBeam2D(screenX: Int, screenY: Int, color: Int) {
        val t     = System.currentTimeMillis()
        val pulse = 0.55f + 0.45f * ((sin(t * 0.0025) + 1.0) / 2.0).toFloat()
        val highlightPos = ((t % 1200L).toFloat() / 1200f) * 50

        val r = (color shr 16) and 0xFF
        val g = (color shr 8)  and 0xFF
        val b =  color         and 0xFF
        val beamH    = 200; val maxW = 22; val slices = 50; val sliceH = beamH / slices

        for (i in 0 until slices) {
            val frac  = 1f - i.toFloat() / slices
            val width = (maxW * Math.pow(frac.toDouble(), 0.65)).toInt().coerceAtLeast(1)
            val sliceY = screenY - i * sliceH
            val coreA  = (frac * pulse * 130f).toInt().coerceIn(0, 170)
            val dist   = Math.abs(i - highlightPos)
            val hlA    = if (dist < 6f) ((1f - dist / 6f) * 90f).toInt() else 0
            val alpha  = (coreA + hlA).coerceIn(0, 210)
            val coreW  = (width * 0.45f).toInt().coerceAtLeast(1)
            FillRect(screenX - coreW / 2, sliceY - sliceH, coreW, sliceH, color, alpha)
            val glowR = (r + (255 - r) * 0.25f).toInt()
            val glowG = (g + (255 - g) * 0.25f).toInt()
            val glowB = (b + (255 - b) * 0.25f).toInt()
            val glow  = (glowR shl 16) or (glowG shl 8) or glowB
            val outerW = width - coreW
            if (outerW > 0) {
                FillRect(screenX - width / 2, sliceY - sliceH, outerW / 2, sliceH, glow, (alpha * 0.4f).toInt())
                FillRect(screenX + coreW / 2, sliceY - sliceH, outerW / 2, sliceH, glow, (alpha * 0.4f).toInt())
            }
        }
        val baseAlpha = (pulse * 40f).toInt()
        FillRect(screenX - maxW,     screenY - 4, maxW * 2, 4, color, baseAlpha)
        FillRect(screenX - maxW / 2, screenY - 2, maxW,     2, color, baseAlpha + 25)
    }

    // ─── Settings panel draw ──────────────────────────────────────────────────

    private fun drawSettingsPanel() {
        var screenW = 765; var screenH = 503
        try { screenW = GameShell.canvasWidth; screenH = GameShell.canvasHeight } catch (_: Exception) {}
        if (screenW < 100 || screenH < 100) return

        // In standard mode the 3D viewport is the upper-left ~512×334 area.
        // The rest of the canvas is minimap, inventory, chatbox.
        // In HD the viewport fills the full canvas.
        val vpW: Int; val vpH: Int
        if (API.IsHD()) { vpW = screenW; vpH = screenH }
        else { vpW = 512; vpH = 334 }

        PX = ((vpW - PW) / 2).coerceAtLeast(4) - 2
        PY = ((vpH - PH) / 2).coerceAtLeast(4) - 1

        // In HD, shift up and left so we don't cover the player character
        if (API.IsHD()) { PX -= 400; PY -= 200 }
        PX = PX.coerceAtLeast(4); PY = PY.coerceAtLeast(4)

        // If slide panel is open, shift left so both fit in viewport
        if (beamSlideOpen && PX + PW + SLIDE_W + 4 > vpW) {
            PX = (vpW - PW - SLIDE_W - 4).coerceAtLeast(2)
        }

        FillRect(PX, PY, PW, PH, 0x1A1A1A, 248)
        DrawRect(PX, PY, PW, PH, 0x555555)

        // ── Title bar ──
        FillRect(PX, PY, PW, TITLE_H, 0x2A2A2A, 255)
        DrawRect(PX, PY, PW, TITLE_H, 0x555555)
        drawSmallText("PoE Loot Filter Settings", PX + 8, PY + 14, 0xFFFFFF)
        drawSmallText("[X]", PX + PW - 28, PY + 14, 0xFF4444)
        // Slide panel arrow
        val arrowText = if (beamSlideOpen) "Beams <" else "Beams >"
        val arrowX = PX + PW - 100
        drawSmallText(arrowText, arrowX, PY + 14, 0x88CCFF)

        // ── Thresholds (2 per row) ──
        drawSmallText("THRESHOLDS", PX + 8, PY + TITLE_H + 2 + 10, 0x999999)
        val threshLabels = listOf("Low", "Medium", "High", "Insane", "Hide Below", "Alert (dB)")
        val threshValues = listOf(lowValue, mediumValue, highValue, insaneValue, hideBelowValue, alertVolume)
        for (i in 0..5) {
            val row = i / 2; val col = i % 2
            val fy  = threshStartY + row * THRESH_ROW_H
            val colOff = col * (PW / 2)
            val active = activeField == i
            val value  = if (active) "$fieldBuffer|" else threshValues[i].toString()
            drawSmallText(threshLabels[i], PX + 8 + colOff, fy + 12, 0xCCCCCC)
            val fx = PX + 80 + colOff; val fw = PW / 2 - 90
            FillRect(fx, fy + 1, fw, 15, if (active) 0x1A2A3A else 0x111111, 255)
            DrawRect(fx, fy + 1, fw, 15, if (active) 0x55AAFF else 0x444444)
            drawSmallText(value, fx + 3, fy + 12, 0xFFFFFF)
        }

        // ── Tier settings (colors + audio merged) ──
        val tLabelY = tierStartY - SEC_LABEL_H
        drawSmallText("TIER SETTINGS", PX + 8, tLabelY + 10, 0x999999)
        drawSmallText("Accent", PX + 148, tLabelY + 10, 0x777777)
        drawSmallText("BG",     PX + 218, tLabelY + 10, 0x777777)
        drawSmallText("Beam",   PX + 268, tLabelY + 10, 0x777777)
        drawSmallText("Audio",  PX + 320, tLabelY + 10, 0x777777)

        for ((idx, tier) in tierOrder.withIndex()) {
            val ry    = tierStartY + idx * TIER_ROW_H
            val style = currentStyles[tier]!!
            drawSmallText(tier.capitalize(), PX + 8, ry + 14, style.textColor)

            // Accent swatch
            val ax = PX + 140; val ay = ry + 3
            FillRect(ax, ay, 40, 16, style.borderColor, 255)
            DrawRect(ax, ay, 40, 16, if (pickerOpen && pickerTier == tier && pickerIsBorder)  0xFFFFFF else 0x555555)

            // BG swatch
            val bx = PX + 200; val by = ry + 3
            FillRect(bx, by, 40, 16, style.bgColor, 255)
            DrawRect(bx, by, 40, 16, if (pickerOpen && pickerTier == tier && !pickerIsBorder) 0xFFFFFF else 0x555555)

            // Beam checkbox
            val beamOn = tierBeamEnabled[tier] == true
            val cbx = PX + 270; val cby = ry + 3
            FillRect(cbx, cby, 15, 15, if (beamOn) 0x1A2A1A else 0x111111, 255)
            DrawRect(cbx, cby, 15, 15, if (beamOn) 0x55FF55 else 0x444444)
            if (beamOn) drawSmallText("v", cbx + 3, cby + 11, style.borderColor)

            // Audio browse button + filename
            val path     = tierAudioPaths[tier] ?: ""
            val fileName = if (path.isNotEmpty()) File(path).name else "none"
            val display  = if (fileName.length > 16) fileName.take(13) + "..." else fileName
            drawSmallText(display, PX + 310, ry + 14, if (path.isNotEmpty()) 0xBBBBBB else 0x555555)
            val bbx = PX + PW - 62
            FillRect(bbx, ry + 2, 52, 16, 0x2E2E2E, 255)
            DrawRect(bbx, ry + 2, 52, 16, 0x666666)
            drawSmallText("Browse", bbx + 26, ry + 13, 0xCCCCCC, center = true)
        }

        // ── Options ──
        FillRect(PX, optStartY, PW, 1, 0x3A3A3A, 255)
        drawSmallText("${if (displayPrices)   "[x]" else "[ ]"} Show prices",    PX + 10,  optStartY + 13, 0xCCCCCC)
        drawSmallText("${if (useLiveGEPrices) "[x]" else "[ ]"} Live GE prices", PX + 165, optStartY + 13, 0xCCCCCC)

        // ── Buttons ──
        FillRect(PX, btnStartY - 2, PW, 1, 0x3A3A3A, 255)
        val bw = 80; val gap = (PW - 3 * bw) / 4
        drawPanelButton("Defaults", PX + gap,             btnStartY)
        drawPanelButton("Save",     PX + gap * 2 + bw,    btnStartY)
        drawPanelButton("Close",    PX + gap * 3 + bw * 2, btnStartY)

        // ── Slide panel ──
        if (beamSlideOpen) drawBeamSlidePanel()
        if (pickerOpen) drawColorPicker()
    }

    private fun drawBeamSlidePanel() {
        FillRect(slideX, slideY, SLIDE_W, slideH, 0x1A1A1A, 248)
        DrawRect(slideX, slideY, SLIDE_W, slideH, 0x555555)

        // Title
        FillRect(slideX, slideY, SLIDE_W, TITLE_H, 0x2A2A2A, 255)
        DrawRect(slideX, slideY, SLIDE_W, TITLE_H, 0x555555)
        drawSmallText("Beam Settings", slideX + 8, slideY + 14, 0xFFFFFF)
        drawSmallText("[X]", slideX + SLIDE_W - 28, slideY + 14, 0xFF4444)

        var sy = slideY + TITLE_H + 2

        // Beam column toggle
        val colOn = beamColumnEnabled
        FillRect(slideX + 8, sy + 1, 15, 15, if (colOn) 0x1A2A1A else 0x111111, 255)
        DrawRect(slideX + 8, sy + 1, 15, 15, if (colOn) 0x55FF55 else 0x444444)
        if (colOn) drawSmallText("v", slideX + 11, sy + 12, 0x55FF55)
        drawSmallText("Beam Column", slideX + 28, sy + 12, 0xCCCCCC)
        sy += SLIDE_EFF_ROW

        // Beam occlusion toggle
        val occOn = beamOcclusion
        FillRect(slideX + 8, sy + 1, 15, 15, if (occOn) 0x1A2A1A else 0x111111, 255)
        DrawRect(slideX + 8, sy + 1, 15, 15, if (occOn) 0x55FF55 else 0x444444)
        if (occOn) drawSmallText("v", slideX + 11, sy + 12, 0x55FF55)
        drawSmallText("Occlusion", slideX + 28, sy + 12, 0xCCCCCC)
        sy += SLIDE_EFF_ROW

        // Divider
        FillRect(slideX, sy + DIVIDER_H / 2, SLIDE_W, 1, 0x3A3A3A, 255)
        sy += DIVIDER_H

        // Effects label
        drawSmallText("EFFECTS", slideX + 8, sy + 10, 0x999999)
        sy += SEC_LABEL_H

        // Effect toggles
        for (i in beamEffectKeys.indices) {
            val key = beamEffectKeys[i]
            val on  = beamEffects[key] == true
            FillRect(slideX + 8, sy + 1, 15, 15, if (on) 0x1A2A1A else 0x111111, 255)
            DrawRect(slideX + 8, sy + 1, 15, 15, if (on) 0x55FF55 else 0x444444)
            if (on) drawSmallText("v", slideX + 11, sy + 12, 0x55FF55)
            drawSmallText(beamEffectLabels[i], slideX + 28, sy + 12, 0xCCCCCC)

            // Box occlusion toggle on the poeBox row
            if (key == "poeBox") {
                val boxOn = poeBoxOcclusion
                FillRect(slideX + 118, sy + 1, 15, 15, if (boxOn) 0x1A2A1A else 0x111111, 255)
                DrawRect(slideX + 118, sy + 1, 15, 15, if (boxOn) 0x55FF55 else 0x444444)
                if (boxOn) drawSmallText("v", slideX + 121, sy + 12, 0x55FF55)
                drawSmallText("Occ", slideX + 138, sy + 12, 0x888888)
            }
            sy += SLIDE_EFF_ROW
        }
    }

    private fun drawColorPicker() {
        FillRect(CPX, CPY, CPW, CPH, 0x1A1A1A, 250)
        DrawRect(CPX, CPY, CPW, CPH, 0x777777)
        FillRect(CPX, CPY, CPW, TITLE_H, 0x2A2A2A, 255)
        drawSmallText("${pickerTier.capitalize()}  ${if (pickerIsBorder) "Accent" else "Background"}", CPX + 8, CPY + 15, 0xFFFFFF)

        val currentColor = if (pickerIsBorder) currentStyles[pickerTier]!!.borderColor
        else                currentStyles[pickerTier]!!.bgColor
        for (i in colorPresets.indices) {
            val col = i % SCOLS; val row = i / SCOLS
            val sx = CPX + 8 + col * (SW + 4)
            val sy = CPY + TITLE_H + 4 + row * (SH + 4)
            FillRect(sx, sy, SW, SH, colorPresets[i], 255)
            DrawRect(sx, sy, SW, SH, if (colorPresets[i] == currentColor) 0xFFFFFF else 0x333333)
        }

        var py = gridEndY + 2
        drawSmallText("Hex #", CPX + 8, py + 12, 0x999999)
        val hfx = CPX + 52; val hfw = CPW - 60
        FillRect(hfx, py, hfw, 17, if (hexFieldActive) 0x1A2A3A else 0x111111, 255)
        DrawRect(hfx, py, hfw, 17, if (hexFieldActive) 0x55AAFF else 0x444444)
        drawSmallText("${hexBuffer}${if (hexFieldActive) "|" else ""}", hfx + 4, py + 12, 0xFFFFFF)
        py += 22

        drawSmallText("Preview", CPX + 8, py + 12, 0x999999)
        FillRect(CPX + 66, py, 60, 17, currentColor, 255)
        DrawRect(CPX + 66, py, 60, 17, 0x666666)
        py += 24

        drawPanelButton("Apply",  CPX + 8,        py)
        drawPanelButton("Cancel", CPX + CPW - 98, py)
    }

    private fun drawPanelButton(label: String, x: Int, y: Int) {
        FillRect(x, y, 90, BTN_H, 0x2E2E2E, 255)
        DrawRect(x, y, 90, BTN_H, 0x666666)
        drawSmallText(label, x + 45, y + 14, 0xFFFFFF, center = true)
    }

    private fun drawSmallText(text: String, x: Int, y: Int, color: Int, center: Boolean = false) {
        DrawText(FontType.SMALL, fromColor(Color(color)), if (center) TextModifier.CENTER else TextModifier.LEFT, text, x, y)
    }

    // ─── Panel click handler ──────────────────────────────────────────────────

    private fun handlePanelClick(cx: Int, cy: Int) {
        val inPicker = pickerOpen && cx in CPX until CPX + CPW && cy in CPY until CPY + CPH
        if (inPicker) { handlePickerClick(cx, cy); return }

        // Slide panel clicks
        if (beamSlideOpen && cx in slideX until slideX + SLIDE_W && cy in slideY until slideY + slideH) {
            handleSlideClick(cx, cy); return
        }

        // [X] close button
        if (cx in (PX + PW - 28)..(PX + PW) && cy in PY..(PY + TITLE_H)) {
            settingsOpen = false; StoreData("poe-settings-open", false); pickerOpen = false; activeField = -1; beamSlideOpen = false; return
        }

        // "Beams >" / "Beams <" arrow toggle
        val arrowX = PX + PW - 100
        if (cx in arrowX..(arrowX + 68) && cy in PY..(PY + TITLE_H)) {
            beamSlideOpen = !beamSlideOpen; return
        }

        // Threshold fields (2 per row)
        for (i in 0..5) {
            val row = i / 2; val col = i % 2
            val fy  = threshStartY + row * THRESH_ROW_H
            val colOff = col * (PW / 2)
            val fx = PX + 80 + colOff; val fw = PW / 2 - 90
            if (cx in fx..(fx + fw) && cy in (fy + 1)..(fy + 16)) {
                commitActiveField(); activeField = i
                fieldBuffer = listOf(lowValue, mediumValue, highValue, insaneValue, hideBelowValue, alertVolume)[i].toString()
                hexFieldActive = false; return
            }
        }

        // Tier rows (merged color + audio)
        for ((idx, tier) in tierOrder.withIndex()) {
            val ry = tierStartY + idx * TIER_ROW_H
            // Accent swatch
            val ax = PX + 140
            if (cx in ax..(ax + 40) && cy in (ry + 3)..(ry + 19)) { openPicker(tier, true);  return }
            // BG swatch
            val bx = PX + 200
            if (cx in bx..(bx + 40) && cy in (ry + 3)..(ry + 19)) { openPicker(tier, false); return }
            // Beam checkbox
            val cbx = PX + 270
            if (cx in cbx..(cbx + 15) && cy in (ry + 3)..(ry + 18)) {
                tierBeamEnabled[tier] = !(tierBeamEnabled[tier] ?: false); return
            }
            // Browse button
            val bbx = PX + PW - 62
            if (cx in bbx..(bbx + 52) && cy in (ry + 2)..(ry + 18)) { openAudioFilePicker(tier); return }
        }

        // Options row
        if (cy in optStartY..(optStartY + OPT_H)) {
            if (cx in (PX + 10)..(PX + 155))  { displayPrices   = !displayPrices;   return }
            if (cx in (PX + 165)..(PX + 320)) { useLiveGEPrices = !useLiveGEPrices; return }
        }

        // Buttons
        if (cy in btnStartY..(btnStartY + BTN_H)) {
            val bw = 80; val gap = (PW - 3 * bw) / 4
            if (cx in (PX + gap)..(PX + gap + 90))                        { applyDefaults(); return }
            if (cx in (PX + gap * 2 + bw)..(PX + gap * 2 + bw + 90))     { saveSettings(); return }
            if (cx in (PX + gap * 3 + bw * 2)..(PX + gap * 3 + bw * 2 + 90)) {
                settingsOpen = false; StoreData("poe-settings-open", false); pickerOpen = false; activeField = -1; beamSlideOpen = false; return
            }
        }

        commitActiveField(); activeField = -1; hexFieldActive = false
    }

    private fun handleSlideClick(cx: Int, cy: Int) {
        // [X] close button in slide panel title bar
        if (cy in slideY..(slideY + TITLE_H) && cx in (slideX + SLIDE_W - 28)..(slideX + SLIDE_W)) {
            beamSlideOpen = false; return
        }

        var sy = slideY + TITLE_H + 2

        // Beam column toggle
        if (cy in sy..(sy + SLIDE_EFF_ROW) && cx in slideX..(slideX + SLIDE_W)) {
            beamColumnEnabled = !beamColumnEnabled; return
        }
        sy += SLIDE_EFF_ROW

        // Beam occlusion toggle
        if (cy in sy..(sy + SLIDE_EFF_ROW) && cx in slideX..(slideX + SLIDE_W)) {
            beamOcclusion = !beamOcclusion; return
        }
        sy += SLIDE_EFF_ROW + DIVIDER_H + SEC_LABEL_H

        // Effect toggles
        for (i in beamEffectKeys.indices) {
            val key = beamEffectKeys[i]
            if (cy in sy..(sy + SLIDE_EFF_ROW)) {
                // Box occlusion sub-toggle on poeBox row
                if (key == "poeBox" && cx in (slideX + 118)..(slideX + 190)) {
                    poeBoxOcclusion = !poeBoxOcclusion; return
                }
                // Effect toggle (left side)
                if (cx in slideX..(slideX + 115)) {
                    beamEffects[key] = !(beamEffects[key] ?: false); return
                }
            }
            sy += SLIDE_EFF_ROW
        }
    }

    private fun openAudioFilePicker(tier: String) {
        Thread {
            try {
                if (!resFolder.exists()) resFolder.mkdirs()
                val chooser = JFileChooser(resFolder)
                chooser.dialogTitle = "Select audio for $tier tier"
                chooser.fileFilter  = FileNameExtensionFilter("WAV Audio Files (*.wav)", "wav")
                chooser.isAcceptAllFileFilterUsed = false
                if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
                    tierAudioPaths[tier] = chooser.selectedFile.absolutePath
            } catch (_: Exception) { }
        }.also { it.isDaemon = true }.start()
    }

    private fun handlePickerClick(cx: Int, cy: Int) {
        for (i in colorPresets.indices) {
            val col = i % SCOLS; val row = i / SCOLS
            val sx = CPX + 8 + col * (SW + 4); val sy = CPY + TITLE_H + 4 + row * (SH + 4)
            if (cx in sx..(sx + SW) && cy in sy..(sy + SH)) {
                applyPickerColor(colorPresets[i]); hexBuffer = "%06X".format(colorPresets[i]); hexFieldActive = false; return
            }
        }
        var py = gridEndY + 2
        val hfx = CPX + 52; val hfw = CPW - 60
        if (cx in hfx..(hfx + hfw) && cy in py..(py + 17)) {
            hexFieldActive = true
            if (hexBuffer.isEmpty()) hexBuffer = "%06X".format(
                if (pickerIsBorder) currentStyles[pickerTier]!!.borderColor else currentStyles[pickerTier]!!.bgColor)
            return
        }
        py += 22; py += 24
        if (cy in py..(py + BTN_H)) {
            when {
                cx in (CPX + 8)..(CPX + 98)             -> { pickerOpen = false; hexFieldActive = false }
                cx in (CPX + CPW - 98)..(CPX + CPW - 8) -> { applyPickerColor(pickerOrigColor); pickerOpen = false; hexFieldActive = false }
            }
        }
    }

    private fun openPicker(tier: String, isBorder: Boolean) {
        pickerTier = tier; pickerIsBorder = isBorder; pickerOpen = true; hexFieldActive = false
        pickerOrigColor = if (isBorder) currentStyles[tier]!!.borderColor else currentStyles[tier]!!.bgColor
        hexBuffer = "%06X".format(pickerOrigColor); activeField = -1
    }

    private fun applyPickerColor(color: Int) {
        val s = currentStyles[pickerTier]!!
        currentStyles[pickerTier] = if (pickerIsBorder) s.copy(
            borderColor = color, textColor = color, priceColor = blendColor(color, 0x000000, 0.45f)
        ) else s.copy(bgColor = color)
    }

    private fun commitHexField() {
        try { applyPickerColor(hexBuffer.toLong(16).toInt()) } catch (_: Exception) {}
        hexFieldActive = false
    }

    private fun commitActiveField() {
        if (activeField == -1) return
        val v = fieldBuffer.toIntOrNull() ?: return
        when (activeField) {
            0 -> lowValue       = v
            1 -> mediumValue    = v
            2 -> highValue      = v
            3 -> insaneValue    = v
            4 -> hideBelowValue = v
            5 -> alertVolume    = v.coerceIn(-80, 0)
        }
        activeField = -1
    }

    private fun applyDefaults() {
        lowValue = 5000; mediumValue = 20000; highValue = 50000
        insaneValue = 100000; hideBelowValue = 0; alertVolume = -20
        displayPrices = true; useLiveGEPrices = true
        for ((k, v) in defaultStyles) currentStyles[k] = v
        for (tier in tierOrder) { tierAudioPaths[tier] = ""; tierBeamEnabled[tier] = false }
        beamOcclusion = true; poeBoxOcclusion = true; beamColumnEnabled = true
        for (k in beamEffectKeys) beamEffects[k] = (k == "pool" || k == "icon" || k == "poeBox")
        pickerOpen = false; activeField = -1; hexFieldActive = false
    }

    private fun saveSettings() {
        commitActiveField()
        StoreData("low-value",         lowValue);  StoreData("medium-value",      mediumValue)
        StoreData("high-value",        highValue); StoreData("insane-value",      insaneValue)
        StoreData("hide-below-value",  hideBelowValue)
        StoreData("poe-alert-volume",  alertVolume)
        StoreData("poe-display-price", displayPrices)
        StoreData("poe-use-remote",    useLiveGEPrices)
        saveColorOverrides(); saveAudioPaths(); saveBeamSettings(); saveBeamEffects()
        refreshGEPrices()
        SendMessage("PoE Filter: settings saved.")
        settingsOpen = false; StoreData("poe-settings-open", false); pickerOpen = false; activeField = -1; beamSlideOpen = false
    }

    // ─── Color helpers ────────────────────────────────────────────────────────

    private fun loadColorOverrides() {
        for (tier in tierOrder) {
            val def    = defaultStyles[tier]!!
            val accent = GetData("poe-accent-$tier") as? Int ?: def.borderColor
            val bg     = GetData("poe-bg-$tier")     as? Int ?: def.bgColor
            currentStyles[tier] = def.copy(
                borderColor = accent, textColor = accent,
                priceColor  = blendColor(accent, 0x000000, 0.45f), bgColor = bg
            )
        }
    }

    private fun saveColorOverrides() {
        for (tier in tierOrder) {
            val s = currentStyles[tier]!!
            StoreData("poe-accent-$tier", s.borderColor)
            StoreData("poe-bg-$tier",     s.bgColor)
        }
    }

    private fun blendColor(a: Int, b: Int, t: Float): Int {
        val ar = (a shr 16) and 0xFF; val ag = (a shr 8) and 0xFF; val ab = a and 0xFF
        val br = (b shr 16) and 0xFF; val bg = (b shr 8) and 0xFF; val bb = b and 0xFF
        return ((ar + ((br - ar) * t).toInt()) shl 16) or ((ag + ((bg - ag) * t).toInt()) shl 8) or (ab + ((bb - ab) * t).toInt())
    }

    // ─── Audio ────────────────────────────────────────────────────────────────

    private fun playTierAlert(tier: String) {
        try {
            val path = tierAudioPaths[tier] ?: ""; if (path.isEmpty()) return
            val file = File(path); if (!file.exists()) return
            val clip: Clip = AudioSystem.getClip()
            clip.open(AudioSystem.getAudioInputStream(file))
            val gain = clip.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN) as javax.sound.sampled.FloatControl
            gain.value = alertVolume.toFloat()
            clip.start()
        } catch (_: Exception) { }
    }

    // ─── Ground item rendering ────────────────────────────────────────────────

    private fun renderGroundItemNames() {
        for (x in 0..103) {
            for (y in 0..103) {
                try {
                    val stack = SceneGraph.objStacks[Player.plane][x][y] ?: continue

                    val heights = mutableListOf<Int>()
                    var calc = stack.head() as? ObjStackNode
                    while (calc != null) {
                        if (shouldDisplayItem(calc)) {
                            val style = getStyle(calc)
                            heights.add(FONT_H + (if (displayPrices) PRICE_H + 5 else 0) + style.padV * 2)
                        }
                        calc = stack.next() as? ObjStackNode
                    }
                    if (heights.isEmpty()) continue

                    val screenPos = API.CalculateSceneGraphScreenPosition((x shl 7) + 64, (y shl 7) + 64, 64)
                    if (screenPos[0] < 0 || screenPos[1] < 0) continue

                    // Beam column + 2D effects (software mode only).
                    // In HD mode, GL handles beam column and effects via drawBeamsGL.
                    if (!API.IsHD()) {
                        var beamItem = stack.head() as? ObjStackNode
                        while (beamItem != null) {
                            if (shouldDisplayItem(beamItem)) {
                                val style = getStyle(beamItem)
                                if (tierBeamEnabled[style.key] == true) {
                                    drawLootBeam3D(x, y, style.borderColor, beamItem!!.value.type, beamItem!!.value.amount)
                                    break
                                }
                            }
                            beamItem = stack.next() as? ObjStackNode
                        }
                    }

                    var totalH = heights.sum() + heights.size * 2
                    var item   = stack.head() as? ObjStackNode
                    var hi     = 0

                    while (item != null) {
                        if (shouldDisplayItem(item)) {
                            val style = getStyle(item)
                            val key   = "${item.value?.type}_${x}_${y}"
                            if (style.key != "default" && (tierAudioPaths[style.key] ?: "").isNotEmpty()) {
                                if (!alertedItems.contains(key)) { alertedItems.add(key); playTierAlert(style.key) }
                            }
                            val boxH   = heights[hi]
                            val boxTop = screenPos[1] - totalH
                            if (beamEffects["poeBox"] == true) {
                                val boxOccluded = poeBoxOcclusion && isTileOccludedByDepth(screenPos[0], screenPos[1], x, y)
                                if (!boxOccluded) drawItem(item, screenPos[0], boxTop, boxH)
                            }
                            totalH -= (boxH + 2); hi++
                        }
                        item = stack.next() as? ObjStackNode
                    }
                } catch (_: Exception) { }
            }
        }

        alertedItems.retainAll { key ->
            var found = false
            outer@ for (x in 0..103) {
                for (y in 0..103) {
                    val stack = SceneGraph.objStacks[Player.plane][x][y] ?: continue
                    var item = stack.head() as? ObjStackNode
                    while (item != null) {
                        val current = item
                        if ("${current?.value?.type}_${x}_${y}" == key) { found = true; break@outer }
                        item = stack.next() as? ObjStackNode
                    }
                }
            }
            found
        }
    }

    private fun getStyle(item: ObjStackNode): TierStyle {
        return try {
            val value   = item.value ?: return currentStyles["default"]!!
            val itemDef = ObjTypeList.get(value.type) ?: return currentStyles["default"]!!
            val haValue = if (itemDef.id == coinId) value.amount else (itemDef.cost * 0.6 * value.amount).roundToInt()
            val geValue = (gePriceMap[itemDef.id.toString()]?.toInt() ?: 0) * value.amount
            val highest = maxOf(haValue, geValue)
            currentStyles[when {
                isTagged(itemDef.id)   -> "tagged"
                highest >= insaneValue -> "insane"
                highest >= highValue   -> "high"
                highest >= mediumValue -> "medium"
                highest >= lowValue    -> "low"
                else                   -> "default"
            }]!!
        } catch (_: Exception) { currentStyles["default"]!! }
    }

    private fun drawItem(item: ObjStackNode, cx: Int, boxTop: Int, boxH: Int) {
        try {
            val value   = item.value ?: return
            val itemDef = ObjTypeList.get(value.type) ?: return
            val haValue = if (itemDef.id == coinId) value.amount else (itemDef.cost * 0.6 * value.amount).roundToInt()
            val geValue = (gePriceMap[itemDef.id.toString()]?.toInt() ?: 0) * value.amount
            val style   = getStyle(item)

            val amountSuffix = if (value.amount > 1) " (${formatValue(value.amount)})" else ""
            val nameLine     = "${itemDef.name}$amountSuffix"
            val priceLine    = if (displayPrices) {
                if (geValue > 0) "GE: ${formatValue(geValue)}  HA: ${formatValue(haValue)}"
                else "HA: ${formatValue(haValue)}"
            } else null

            val nameW     = nameLine.length * 8
            val priceW    = (priceLine?.length ?: 0) * 6
            val textAreaW = maxOf(nameW, priceW) + style.padH * 2
            val boxW      = stripeW + textAreaW
            val boxX      = cx - boxW / 2

            FillRect(boxX + 2, boxTop + 2, boxW, boxH, 0x000000, 150)
            FillRect(boxX + stripeW, boxTop, textAreaW, boxH, style.bgColor, style.bgAlpha)
            FillRect(boxX, boxTop, stripeW, boxH, style.borderColor, style.stripeAlpha)
            DrawRect(boxX, boxTop, boxW, boxH, style.borderColor)

            try {
                val iconSize = stripeW - 6
                GetObjSprite(itemDef.id, value.amount, false, 0, 0)?.renderResized(
                    boxX + 3, boxTop + (boxH - iconSize) / 2, iconSize, iconSize
                )
            } catch (_: Exception) { }

            val textX  = boxX + stripeW + style.padH
            val nameY  = boxTop + style.padV + FONT_H
            val priceY = nameY + PRICE_H + 5

            val nameJs  = JagString.of(nameLine)
            val offsets = listOf(
                -2 to 0, 2 to 0, 0 to -2, 0 to 2,
                -2 to -2, 2 to -2, -2 to 2, 2 to 2,
                -2 to -1, 2 to -1, -2 to 1, 2 to 1,
                -1 to -2, 1 to -2, -1 to 2, 1 to 2,
                -1 to -1, 1 to -1, -1 to 1, 1 to 1,
                -1 to 0,  1 to 0,  0 to -1, 0 to 1
            )
            for ((ox, oy) in offsets) Fonts.b12Full.renderLeft(nameJs, textX + ox, nameY + oy, 0x000000, -1)
            Fonts.b12Full.renderLeft(nameJs, textX, nameY, style.textColor, -1)

            if (priceLine != null)
                DrawText(FontType.SMALL, fromColor(Color(style.priceColor)), TextModifier.LEFT, priceLine, textX, priceY)
        } catch (_: Exception) { }
    }

    private fun shouldDisplayItem(item: ObjStackNode): Boolean {
        return try {
            val value   = item.value ?: return false
            val itemDef = ObjTypeList.get(value.type) ?: return false
            val haValue = if (itemDef.id == coinId) value.amount else (itemDef.cost * 0.6 * value.amount).roundToInt()
            val geValue = (gePriceMap[itemDef.id.toString()]?.toInt() ?: 0) * value.amount
            !((maxOf(haValue, geValue) < hideBelowValue || isHidden(itemDef.id)) && !isTagged(itemDef.id))
        } catch (_: Exception) { false }
    }

    // ─── MiniMenu ─────────────────────────────────────────────────────────────

    override fun NPCOverheadDraw(npc: rt4.Npc, screenX: Int, screenY: Int) {
    }

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        if (currentEntries != null && IsKeyPressed(Keyboard.KEY_CTRL)) {
            for ((index, entry) in currentEntries.withIndex()) {
                if (entry.type == MiniMenuType.OBJ && index == currentEntries.size - 1) {
                    val itemDef = ObjTypeList.get(entry.subjectIndex.toInt())
                    InsertMiniMenuEntry("Ignore/Unignore", itemDef.name.toString(), ignoreItem(itemDef.id))
                    InsertMiniMenuEntry("Tag/Untag",       itemDef.name.toString(), tagItem(itemDef.id))
                }
            }
        }
    }

    private fun ignoreItem(itemId: Int) = Runnable {
        val list = ignoredItems.toMutableList()
        if (list.contains(itemId)) list.remove(itemId) else list.add(itemId)
        ignoredItems = list; StoreData("poe-ignore", list.joinToString(","))
    }

    private fun tagItem(itemId: Int) = Runnable {
        val list = taggedItems.toMutableList()
        if (list.contains(itemId)) list.remove(itemId) else list.add(itemId)
        taggedItems = list; StoreData("poe-tags", list.joinToString(","))
    }

    // ─── Commands ─────────────────────────────────────────────────────────────

    override fun ProcessCommand(commandStr: String, args: Array<out String>?) {
        when (commandStr.toLowerCase()) {
            "::poesettings"     -> { settingsOpen = !settingsOpen; StoreData("poe-settings-open", settingsOpen) }
            "::poeconfig-reset" -> resetConfig().also { Init() }
            "::poeconfig"       -> displayRanges()
            "::poeignore"       -> args?.get(0)?.toInt()?.let { ignoreItem(it).run() }
            "::poetag"          -> args?.get(0)?.toInt()?.let { tagItem(it).run() }
            "::poetoggleprices" -> { displayPrices = !displayPrices; SendMessage("Display Prices: $displayPrices") }

            "::droptestforce" -> {
                val target = args?.joinToString(" ")?.takeIf { it.isNotBlank() } ?: "Papasmurf2"
                val itemId = findItemIdByName("Elysian spirit shield").takeIf { it >= 0 } ?: 12817
                broadcastIcons[target] = BroadcastEntry(itemId, 1, System.currentTimeMillis(), 0x88CCFF)
            }
            else -> commandMap[commandStr]?.let { key ->
                args?.get(0)?.toInt()?.let { v -> if (v >= 0) StoreData(key, v).also { Init() } }
            }
        }
    }

    // ─── KondoKit ─────────────────────────────────────────────────────────────

    fun OnKondoValueUpdated() {
        StoreData("poe-tags",          taggedItems.joinToString(","))
        StoreData("poe-ignore",        ignoredItems.joinToString(","))
        StoreData("low-value",         lowValue);   StoreData("medium-value",      mediumValue)
        StoreData("high-value",        highValue);  StoreData("insane-value",      insaneValue)
        StoreData("hide-below-value",  hideBelowValue)
        StoreData("poe-alert-volume",  alertVolume)
        StoreData("poe-display-price", displayPrices)
        StoreData("poe-use-remote",    useLiveGEPrices)
        refreshGEPrices()
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun resetConfig() {
        lowValue = 5000; mediumValue = 20000; highValue = 50000
        insaneValue = 100000; hideBelowValue = 0; alertVolume = -20
        useLiveGEPrices = true; displayPrices = true
        StoreData("poe-tags", ""); StoreData("poe-ignore", "")
        for ((k, v) in defaultStyles) { currentStyles[k] = v; StoreData("poe-accent-$k", v.borderColor); StoreData("poe-bg-$k", v.bgColor) }
        for (tier in tierOrder) { tierAudioPaths[tier] = ""; tierBeamEnabled[tier] = false; StoreData("poe-audio-$tier", ""); StoreData("poe-beam-$tier", false) }
        beamOcclusion = true; StoreData("poe-beam-occlusion", true)
        poeBoxOcclusion = true; StoreData("poe-box-occlusion", true)
        beamColumnEnabled = true; StoreData("poe-beam-column", true)
        for (k in beamEffectKeys) { val v = k == "pool" || k == "icon" || k == "poeBox"; beamEffects[k] = v; StoreData("poe-fx-$k", v) }
        StoreData("low-value",         5000);   StoreData("medium-value",      20000)
        StoreData("high-value",        50000);  StoreData("insane-value",      100000)
        StoreData("hide-below-value",  0);      StoreData("poe-alert-volume",  -20)
        StoreData("poe-display-price", true);   StoreData("poe-use-remote",    true)
    }

    private fun displayRanges() {
        SendMessage("== PoE Loot Filter ==")
        SendMessage("Prices: $displayPrices  Live GE: $useLiveGEPrices")
        SendMessage("Low: $lowValue  Med: $mediumValue  High: $highValue  Insane: $insaneValue")
        SendMessage("Hide Below: $hideBelowValue  Alert Vol: ${alertVolume}dB")
        SendMessage("Res folder: ${resFolder.absolutePath}")
        SendMessage("::poesettings to open settings panel")
    }

    /**
     * Loads prices on a background thread: inline, with no timeout, an
     * unreachable CDN hung the client on a black screen at startup.
     */
    private fun refreshGEPrices() {
        Thread({ gePriceMap = loadGEPrices() }, "PoeLootFilter-GEPrices").apply { isDaemon = true }.start()
    }

    fun loadGEPrices(): Map<String, String> {
        return if (useLiveGEPrices) {
            try {
                val url  = URL("https://cdn.2009scape.org/gedata/latest.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"; conn.setRequestProperty("User-Agent", "Mozilla/5.0")
                conn.connectTimeout = 5000; conn.readTimeout = 10000
                if (conn.responseCode != HttpURLConnection.HTTP_OK) return emptyMap()
                val content = conn.inputStream.bufferedReader().use(BufferedReader::readText)
                val items   = content.trim().removeSurrounding("[", "]").split("},").map { it.trim() + "}" }
                val map     = mutableMapOf<String, String>()
                for (item in items) {
                    val pairs  = item.removeSurrounding("{", "}").split(",")
                    val itemId = pairs.find { it.trim().startsWith("\"item_id\"") }?.split(":")?.get(1)?.trim()?.trim('"')
                    val value  = pairs.find { it.trim().startsWith("\"value\"")   }?.split(":")?.get(1)?.trim()?.trim('"')
                    if (itemId != null && value != null) map[itemId] = value
                }
                map
            } catch (_: Exception) { emptyMap() }
        } else {
            try {
                val file = File(resFolder, "item_configs.json")
                if (!file.exists()) return emptyMap()
                BufferedReader(InputStreamReader(file.inputStream(), StandardCharsets.UTF_8))
                    .useLines { lines ->
                        val json  = lines.joinToString("\n")
                        val items = json.trim().removeSurrounding("[", "]").split("},").map { it.trim() + "}" }
                        val map   = mutableMapOf<String, String>()
                        for (item in items) {
                            val pairs = item.removeSurrounding("{", "}").split(",")
                            val id    = pairs.find { it.trim().startsWith("\"id\"") }?.split(":")?.get(1)?.trim()?.trim('"')
                            val ge    = pairs.find { it.trim().startsWith("\"grand_exchange_price\"") }?.split(":")?.get(1)?.trim()?.trim('"')
                            if (id != null && ge != null) map[id] = ge
                        }
                        map
                    }
            } catch (_: Exception) { emptyMap() }
        }
    }

    private fun formatValue(value: Int): String = when {
        value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
        value >= 10_000    -> "%.1fK".format(value / 1_000.0)
        else               -> DecimalFormat("#,###").format(value)
    }

    private fun isTagged(id: Int) = taggedItems.contains(id)
    private fun isHidden(id: Int) = ignoredItems.contains(id)
}