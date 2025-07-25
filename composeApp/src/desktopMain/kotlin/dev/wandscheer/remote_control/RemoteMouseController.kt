package dev.wandscheer.remote_control

import java.awt.MouseInfo
import java.awt.Point
import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import kotlin.math.hypot

/**
 * Drives the local mouse/scroll wheel from [RemoteEvent]s.
 */
class RemoteMouseController {
    private val clickDistancePx = 6         // max move still counted a “tap”
    private val clickTimeMs = 220           // down→up ≤ 220 ms = tap
    private val scrollPixelsPerNotch = 50   // remote-pad px → one wheel notch

    private val robot = Robot().apply { autoDelay = 0 }
    private var gesture: GestureState? = null
    private var scrollAccumulator = 0

    private data class GestureState(
        var fingers: Int = 1,
        val startNs: Long,
        var lastNs: Long,
        val startPos: Point,
    )

    /** feed one event; returns true if it was consumed */
    fun processEvent(ev: RemoteEvent): Boolean = when (ev) {
        is TouchDown -> {
            gesture = GestureState(
                startNs = System.nanoTime(),
                lastNs = System.nanoTime(),
                startPos = MouseInfo.getPointerInfo().location,
            )
            true
        }

        is TouchMove -> {
            val g = gesture ?: return false
            g.lastNs = System.nanoTime()
            g.fingers = ev.fingers
            when (g.fingers) {
                1 -> moveBy(ev.dx, ev.dy)
                2 -> scrollBy(ev.dy.toInt())
            }
            true
        }

        is TouchUp -> {
            val g = gesture ?: return false
            when (g.fingers) {
                1 -> handleSingleFingerUp(g)
                2 -> handleTwoFingerTapOrEnd(g)
                else -> if (g.fingers >= 3) middleClick()
            }
            gesture = null
            scrollAccumulator = 0
            true
        }

        is Scroll -> {
            scrollBy(ev.delta.toInt()); true
        }

        is LeftClick -> {
            click(InputEvent.BUTTON1_DOWN_MASK); true
        }

        is MiddleClick -> {
            click(InputEvent.BUTTON2_DOWN_MASK); true
        }

        is RightClick -> {
            click(InputEvent.BUTTON3_DOWN_MASK); true
        }

        is KeyDown   -> { robot.keyPress(ev.vk.toInt());  true }
        is KeyUp     -> { robot.keyRelease(ev.vk.toInt()); true }
        is TextInput -> {
            ev.text.forEach { ch ->
                val vk = KeyEvent.getExtendedKeyCodeForChar(ch.code)
                robot.keyPress(vk)
                robot.keyRelease(vk)
            }
            true
        }
    }

    /*────────  single-finger helpers ────────*/
    private fun handleSingleFingerUp(g: GestureState) {
        val upTimeMs = (System.nanoTime() - g.startNs) / 1_000_000
        val movedPx = distance(MouseInfo.getPointerInfo().location, g.startPos)
        if (upTimeMs <= clickTimeMs && movedPx <= clickDistancePx) leftClick()
    }

    /*────────  two-finger helpers ────────*/
    private fun handleTwoFingerTapOrEnd(g: GestureState) {
        val upTimeMs = (System.nanoTime() - g.startNs) / 1_000_000
        val movedPx = distance(MouseInfo.getPointerInfo().location, g.startPos)
        if (upTimeMs <= clickTimeMs && movedPx <= clickDistancePx) rightClick()
    }

    /*────────  low-level actions ────────*/
    private fun leftClick() = click(InputEvent.BUTTON1_DOWN_MASK)
    private fun rightClick() = click(InputEvent.BUTTON3_DOWN_MASK)
    private fun middleClick() = click(InputEvent.BUTTON2_DOWN_MASK)

    private fun click(mask: Int) {
        robot.mousePress(mask)
        robot.mouseRelease(mask)
    }

    private fun moveBy(dx: Short, dy: Short) {
        val loc = MouseInfo.getPointerInfo().location
        val nx = (loc.x + dx)
        val ny = (loc.y + dy)
        robot.mouseMove(nx, ny)
    }

    private fun scrollBy(delta: Int) {
        scrollAccumulator += delta
        val notches = scrollAccumulator / scrollPixelsPerNotch
        if (notches != 0) {
            robot.mouseWheel(-notches)
            scrollAccumulator -= notches * scrollPixelsPerNotch
        }
    }

    private fun distance(a: Point, b: Point) = hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toInt()
}
