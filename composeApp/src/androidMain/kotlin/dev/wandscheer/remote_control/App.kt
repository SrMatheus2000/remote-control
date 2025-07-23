package dev.wandscheer.remote_control

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.net.InetSocketAddress

@Composable
@Preview
fun App() {
    val scope = rememberCoroutineScope()

    val udp = remember { UdpChannel.open() }

    val (address, setAddress) = remember { mutableStateOf<InetSocketAddress?>(null) }

    DisposableEffect(Unit) {
        scope.launch {
            udp.sendBroadcast(BROADCAST_IDENTIFIER, SERVER_UDP_PORT)
            println("Sent broadcast")
            while (isActive) {
                val (data, remote) = udp.receive()
                if (data.contentEquals(BROADCAST_IDENTIFIER)) {
                    println("Received broadcast response from ${remote?.address}:${remote?.port}")
                    setAddress(remote)
                    break
                }
            }
        }
        onDispose { udp.close() }
    }

    val sendData = remember(address) {
        { data: String ->
            if (address == null) return@remember
            scope.launch {
                println("Sending data: $data")
                udp.send(data.encodeToByteArray(), address)
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