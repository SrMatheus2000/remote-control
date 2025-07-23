package dev.wandscheer.remote_control

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.StandardSocketOptions.SO_BROADCAST
import java.net.StandardSocketOptions.SO_REUSEADDR
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel

class UdpChannel private constructor(
    private val channel: DatagramChannel
) {
    val localAddress: InetSocketAddress? get() = channel.localAddress as? InetSocketAddress

    companion object {
        fun open(bindAddress: InetAddress? = null, bindPort: Int? = 0): UdpChannel {
            val ch = DatagramChannel.open().apply {
                setOption(SO_BROADCAST, true)
                setOption(SO_REUSEADDR, true)
                bind(bindPort?.let { InetSocketAddress(bindAddress, it) })
            }
            val addr = ch.localAddress as? InetSocketAddress
            println("Listening to UDP on ${addr?.address}:${addr?.port}")
            return UdpChannel(ch)
        }
    }

    suspend fun send(data: ByteArray, host: String, port: Int) =
        send(data, InetSocketAddress(host, port))

    suspend fun send(data: ByteArray, address: InetSocketAddress) = withContext(Dispatchers.IO) {
        channel.send(ByteBuffer.wrap(data), address)
    }

    suspend fun sendBroadcast(
        data: ByteArray,
        port: Int = SERVER_UDP_PORT,
        address: InetAddress = InetAddress.getByName("255.255.255.255")
    ) = withContext(Dispatchers.IO) {
        channel.send(ByteBuffer.wrap(data), InetSocketAddress(address, port))
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