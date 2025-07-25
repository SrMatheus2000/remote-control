package dev.wandscheer.remote_control

import android.view.KeyEvent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
private val backPreIme = Modifier.onInterceptKeyBeforeSoftKeyboard { keyEvent ->
    keyEvent.key == Key.Back && keyEvent.type == KeyEventType.KeyDown
}

@Composable
fun KeyboardCapture(
    onEvent: (RemoteEvent) -> Unit = { }
) {
    var value by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current

    BasicTextField(
        value = value,
        onValueChange = { new ->
            val diff = new.drop(value.length)          // text just typed
            value = new
            if (diff.isNotEmpty()) {
                onEvent(TextInput(diff))
            }
        },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.None
        ),
        modifier = Modifier
            .focusRequester(focus)
            .then(backPreIme)
            .onPreviewKeyEvent { event ->
                // ACTION_DOWN / ACTION_UP from hardware or some soft-KBs
                val vk = androidKeyToWin(event.key.nativeKeyCode) ?: return@onPreviewKeyEvent false
                val ev = if (event.type == KeyEventType.KeyDown) KeyDown(vk) else KeyUp(vk)
                onEvent(ev)
                false                 // don’t block TextField; we want text too
            }
            .size(1.dp)               // virtually invisible
    )

    LaunchedEffect(Unit) {
        focus.requestFocus()          // keep focus forever
        kb?.show()                    // invite IME up-front
    }
}

/** Map Android key codes → Windows virtual-key (very small subset demo) */
private fun androidKeyToWin(code: Int): Short? = when (code) {
    KeyEvent.KEYCODE_DEL -> 0x08   // VK_BACK
    KeyEvent.KEYCODE_ENTER -> 0x0D   // VK_RETURN
    KeyEvent.KEYCODE_SPACE -> 0x20   // VK_SPACE
    KeyEvent.KEYCODE_DPAD_LEFT -> 0x25   // VK_LEFT
    KeyEvent.KEYCODE_DPAD_UP -> 0x26
    KeyEvent.KEYCODE_DPAD_RIGHT -> 0x27
    KeyEvent.KEYCODE_DPAD_DOWN -> 0x28
    else -> null
}
