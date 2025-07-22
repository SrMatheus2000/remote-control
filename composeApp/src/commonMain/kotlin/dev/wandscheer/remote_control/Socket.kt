package dev.wandscheer.remote_control

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UdpChannel private constructor(
    private val channel: DatagramChannel
) {
    val localAddress: InetSocketAddress? get() = channel.localAddress as? InetSocketAddress

    companion object {
        fun open(bindAddress: InetAddress? = null, bindPort: Int? = 0): UdpChannel {
            val ch = DatagramChannel.open().apply {
                bind(bindPort?.let { InetSocketAddress(bindAddress, it) })
            }
            val addr = ch.localAddress as? InetSocketAddress
            println("Listening to UDP on ${addr?.address}:${addr?.port}")
            return UdpChannel(ch)
        }
    }

    suspend fun send(data: ByteArray, host: String, port: Int) = withContext(Dispatchers.IO) {
        channel.send(ByteBuffer.wrap(data), InetSocketAddress(host, port))
    }

    suspend fun receive(maxSize: Int = 1500): Pair<ByteArray, InetSocketAddress?> =
        withContext(Dispatchers.IO) {
            val buf = ByteBuffer.allocate(maxSize)
            val addr = channel.receive(buf)
            buf.flip()
            ByteArray(buf.remaining()).also { buf.get(it) } to addr as InetSocketAddress?
        }

    fun close() = channel.close()
}