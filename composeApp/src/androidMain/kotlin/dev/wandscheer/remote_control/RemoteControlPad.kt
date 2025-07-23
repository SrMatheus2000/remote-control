package dev.wandscheer.remote_control

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Simple wire-format for the demo – tweak to your protocol */
private fun encode(vararg parts: Any) = parts.joinToString(",")

/**
 *  A track-pad, scroll bar, and three mouse buttons laid out like:
 *
 *  ┌─────────────────┬───┐
 *  │                 │   │  <- touch/drag (track-pad)      │ scroll bar
 *  │                 │   │
 *  ├─────────────────┴───┤
 *  │   LMB   | M | RMB   │
 *  └─────────────────────┘
 */
@Composable
fun RemoteControlPad(
    modifier: Modifier = Modifier, onEvent: (RemoteEvent) -> Unit = { }
) {
    Column(modifier) {

        /* ───────────── top row ───────────── */
        Row(Modifier.weight(1f)) {

            /* track-pad area --------------------------------------------------- */
            Box(
                Modifier.weight(1f).fillMaxHeight().background(color = Color.LightGray).pointerInput(Unit) {
                    awaitEachGesture {
                        // wait for first contact
                        val down = awaitFirstDown()
                        val ids = mutableSetOf(down.id)
                        var lastPos = down.position

                        // tell peer "TOUCH_BEGIN, <n fingers>"
                        onEvent(TouchDown)

                        while (true) {
                            val event = awaitPointerEvent()
                            ids += event.changes.map { it.id }

                            // number of contacts currently on surface
                            val fingers = event.changes.count { it.pressed }

                            // send movement delta of primary pointer
                            val primary = event.changes.firstOrNull { it.id == down.id }
                            if (primary != null && primary.pressed) {
                                val delta = primary.position - lastPos
                                if (delta.getDistance() >= 1f) {       // ignore jitter
                                    onEvent(TouchMove(fingers, delta.x.toInt().toShort(), delta.y.toInt().toShort()))
                                    lastPos = primary.position
                                }
                                primary.consume()
                            } else break  // up
                        }

                        // touch ended
                        onEvent(TouchUp)
                    }
                })

            /* scroll bar ------------------------------------------------------- */
            Box(
                Modifier.width(52.dp).fillMaxHeight().background(color = Color.DarkGray).pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        var lastY = down.position.y
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.pressed) {
                                val dy = change.position.y - lastY
                                if (abs(dy) >= 2f) {
                                    onEvent(Scroll(dy.toInt().toShort()))
                                    lastY = change.position.y
                                }
                                change.consume()
                            } else break
                        }
                    }
                })
        }

        /* ───────────── bottom buttons ───────────── */
        Row(
            Modifier.fillMaxWidth().height(76.dp)
        ) {
            @Composable
            fun button(code: RemoteEvent, weight: Float = 1f) = Button(
                onClick = {
                    onEvent(code)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.weight(weight).fillMaxHeight().border(1.dp, Color.DarkGray)
            ) { /* empty, visual handled by surface */ }

            button(LeftClick)
            button(MiddleClick, weight = 0.6f)
            button(RightClick)
        }
    }
}