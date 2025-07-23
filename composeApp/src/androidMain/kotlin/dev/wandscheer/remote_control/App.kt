package dev.wandscheer.remote_control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.net.InetSocketAddress

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()
    val lifeCycleOwner = LocalLifecycleOwner.current

    val udp = remember { UdpChannel.open() }

    val (address, setAddress) = remember { mutableStateOf<InetSocketAddress?>(null) }

    DisposableEffect(lifeCycleOwner) {
        val job = scope.launch(Dispatchers.IO) {

            while (isActive) {
                udp.sendBroadcast(BROADCAST_IDENTIFIER, SERVER_UDP_PORT)
                println("➡️  broadcast sent, waiting…")

                val found = withTimeoutOrNull(5_000) {
                    val (data, remote) = udp.receive()
                    if (data.contentEquals(BROADCAST_IDENTIFIER)) {
                        println("✅  reply from ${remote?.address}:${remote?.port}")
                        remote
                    } else null
                }

                if (found != null) {
                    setAddress(found)
                    break
                } else {
                    println("⏱️  timeout, retrying…")
                }
            }
        }

        onDispose {
            job.cancel()
            udp.close()
        }
    }

    val sendData = remember(address) {
        { data: RemoteEvent ->
            if (address == null) return@remember
            scope.launch {
                println("Sending data: $data")
                udp.send(data.encode(), address)
            }
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier.safeContentPadding().fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RemoteControlPad {
                sendData(it)
            }
        }
    }
}