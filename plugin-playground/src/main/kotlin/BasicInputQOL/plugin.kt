package BasicInputQOL

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import rt4.Keyboard
import KondoKit.Exposed
import java.awt.event.*
import javax.swing.SwingUtilities

class plugin : Plugin() {
    private var cameraDebugEnabled = false
    private var mouseDebugEnabled = false

    @Exposed(description = "Zoom amount per scroll notch (Default: 150, native client is 50)")
    var zoomStep: Int = 150

    private var lastSavedZoomStep = -1

    companion object {
        private var lastMouseWheelX = 0
        private var lastMouseWheelY = 0
        private val defaultCameraPYZ = Triple(128.0, 0.0, 600)
        var instance: plugin? = null
        var wheelEventCount = 0
        var lastWheelDiff = 0
    }

    override fun Init() {
        instance = this
        zoomStep = (API.GetData("basicinputqol-zoom-step") as? Int) ?: 150
        lastSavedZoomStep = zoomStep
        API.AddMouseListener(MouseCallbacks)
        API.AddMouseWheelListener(MouseWheelCallbacks)
    }

    override fun ProcessCommand(commandStr: String?, args: Array<out String>?) {
        commandStr ?: return
        if (API.PlayerHasPrivilege(Privileges.JMOD)) {
            when(commandStr) {
                "::mousedebug" -> mouseDebugEnabled = !mouseDebugEnabled
                "::cameradebug" -> cameraDebugEnabled = !cameraDebugEnabled
            }
        }
    }

    override fun Draw(timeDelta: Long) {
        if (zoomStep != lastSavedZoomStep) {
            lastSavedZoomStep = zoomStep
            API.StoreData("basicinputqol-zoom-step", zoomStep)
        }
        if (mouseDebugEnabled) {
            API.DrawText(
                FontType.SMALL,
                FontColor.YELLOW,
                TextModifier.LEFT,
                "Mouse Coords: (${API.GetMouseX()}, ${API.GetMouseY()})",
                10,
                40
            )
            API.DrawText(
                FontType.SMALL,
                FontColor.YELLOW,
                TextModifier.LEFT,
                "Wheel events: $wheelEventCount, lastDiff=$lastWheelDiff, overScrollable=${API.IsMouseOverScrollableInterface()}, zoom=${API.GetCameraZoom()}",
                10,
                60
            )
        }
        if (cameraDebugEnabled) {
            API.DrawText(
                FontType.SMALL,
                FontColor.YELLOW,
                TextModifier.LEFT,
                "Camera: [P=${API.GetCameraPitch()}, Y=${API.GetCameraYaw()}, Z=${API.GetCameraZoom()}]",
                10,
                50
            )
        }
    }

    object MouseWheelCallbacks : MouseWheelListener {
        override fun mouseWheelMoved(e: MouseWheelEvent?) {
            e ?: return
            wheelEventCount++
            val forceZoom = API.IsKeyPressed(Keyboard.KEY_SHIFT) || API.IsKeyPressed(Keyboard.KEY_CTRL)
            if (forceZoom || !API.IsMouseOverScrollableInterface()) {
                // Use the event's own delta instead of GetMouseWheelRotation()/GetPreviousMouseWheelRotation():
                // those read a counter the core game loop also zeroes every tick (JavaMouseWheel.getRotation()),
                // racing with this AWT callback and occasionally producing a bogus diff that stalls zoom.
                val diff = e.wheelRotation
                lastWheelDiff = diff
                val step = instance?.zoomStep ?: 150
                val newZoom = (API.GetCameraZoom() + diff * step).coerceIn(1, 3500)
                API.SetCameraZoom(newZoom)
            }
        }
    }

    object MouseCallbacks : MouseAdapter() {
        override fun mouseDragged(e: MouseEvent?) {
           e ?: return
           if (SwingUtilities.isMiddleMouseButton(e)) {
               val x = e.x
               val y = e.y
               val accelX = lastMouseWheelX - x
               val accelY = lastMouseWheelY - y
               lastMouseWheelX = x
               lastMouseWheelY = y
               API.UpdateCameraYaw(accelX * 2.0)
               API.UpdateCameraPitch(-accelY * 2.0)
           }
        }

        override fun mouseClicked(e: MouseEvent?) {
            e ?: return

/*            val width = API.GetWindowDimensions().width;
            val compassBordersX = intArrayOf(width - 165, width - 125)
            val compassBordersY = intArrayOf(0, 45)

            if (
                e.x in compassBordersX[0]..compassBordersX[1]
                && e.y in compassBordersY[0]..compassBordersY[1]
            )
            {
                API.SetCameraPitch(defaultCameraPYZ.first)
                API.SetCameraYaw(defaultCameraPYZ.second)
            }*/
        }

        override fun mousePressed(e: MouseEvent?) {
            e ?: return
            if (SwingUtilities.isMiddleMouseButton(e)) {
                lastMouseWheelX = e.x
                lastMouseWheelY = e.y
            }
        }
    }
}