package dev.wandscheer.remote_control

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.application
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import remotecontrol.composeapp.generated.resources.Res
import remotecontrol.composeapp.generated.resources.remote_control_icon
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface


fun firstLocalIPv4(): Inet4Address = NetworkInterface.getNetworkInterfaces().asSequence()          // all NICs
    .filter { it.isUp && !it.isLoopback && !it.isVirtual }     // skip loopback/VPN/tun
    .flatMap { it.inetAddresses.asSequence() }                 // all addresses
    .filterIsInstance<Inet4Address>()                          // keep v4 only
    .firstOrNull()                                             // pick the first one
    ?: error("No suitable IPv4 found")

fun ipv4WithDefaultGateway(): Inet4Address {
    // ── run “route print -4” and collect its text ───────────────────────────────
    val text = ProcessBuilder("cmd", "/c", "route PRINT -4")
        .redirectErrorStream(true)
        .start()
        .inputStream.reader()
        .use { it.readText() }

    // ── find the first “0.0.0.0  0.0.0.0  <gateway>  <interface> …” line ────────
    val ifaceIp = text.lineSequence().dropWhile { !it.trimStart().startsWith("Active Routes:") }
        .firstOrNull { it.trimStart().startsWith("0.0.0.0") }?.trim()
        ?.split(Regex("\\s+"))      // collapse runs of spaces
        ?.getOrNull(3)              // column 4 = Interface
        ?: return firstLocalIPv4()

    return InetAddress.getByName(ifaceIp) as Inet4Address
}

fun main() = application {
    val scope = rememberCoroutineScope()

    val mouseController = remember { RemoteMouseController() }

    val udp = remember { UdpChannel.open(bindAddress = ipv4WithDefaultGateway(), bindPort = SERVER_UDP_PORT) }

    DisposableEffect(Unit) {
        scope.launch {
            while (isActive) {
                val (data, remote) = udp.receive()
                if (data.contentEquals(BROADCAST_IDENTIFIER)) {
                    println("Received broadcast from ${remote?.address}:${remote?.port}")
                    if (remote != null) udp.send(BROADCAST_IDENTIFIER, remote.address.hostAddress, remote.port)
                    continue
                }
                println("${remote?.address}:${remote?.port} → ${RemoteEvent.decode(data)}")
                RemoteEvent.decode(data)?.let { mouseController.processEvent(it) }
            }
        }
        onDispose {
            udp.close()
        }
    }

    val icon = painterResource(Res.drawable.remote_control_icon)

    Tray(
        icon = icon,
        tooltip = "Remote Control",
    ) {
        Item("Quit") { exitApplication() }
    }
}