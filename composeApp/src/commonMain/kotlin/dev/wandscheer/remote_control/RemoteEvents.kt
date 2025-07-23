package dev.wandscheer.remote_control

sealed interface RemoteEvent {
    /** Turn the event into a ByteArray ready for DatagramChannel.send() */
    fun encode(): ByteArray

    companion object {
        /*───── opcodes ─────*/
        protected const val OP_TOUCH_MOVE: Byte = 0x01
        protected const val OP_TOUCH_DOWN: Byte = 0x02
        protected const val OP_TOUCH_UP: Byte = 0x03
        protected const val OP_SCROLL: Byte = 0x04
        protected const val OP_LEFT_CLICK: Byte = 0x11
        protected const val OP_MIDDLE_CLICK: Byte = 0x12
        protected const val OP_RIGHT_CLICK: Byte = 0x13

        /** Parse raw bytes back into a [RemoteEvent] (null if malformed) */
        fun decode(raw: ByteArray): RemoteEvent? {
            if (raw.isEmpty()) return null
            return when (raw[0]) {
                OP_TOUCH_MOVE -> {
                    if (raw.size < 6) return null
                    TouchMove(
                        fingers = raw[1].toInt() and 0xFF, dx = beShort(raw, 2), dy = beShort(raw, 4)
                    )
                }

                OP_TOUCH_DOWN -> TouchDown
                OP_TOUCH_UP -> TouchUp
                OP_SCROLL -> {
                    if (raw.size < 4) return null
                    Scroll(beShort(raw, 2))
                }

                OP_LEFT_CLICK -> LeftClick
                OP_MIDDLE_CLICK -> MiddleClick
                OP_RIGHT_CLICK -> RightClick
                else -> null
            }
        }
    }
}

data class TouchMove(val fingers: Int, val dx: Short, val dy: Short) : RemoteEvent {
    override fun encode() = byteArrayOf(
        RemoteEvent.OP_TOUCH_MOVE, fingers.toByte()
    ) + beBytes(dx) + beBytes(dy)
}

data object TouchDown : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_TOUCH_DOWN, 0)
}

data object TouchUp : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_TOUCH_UP, 0)
}

data class Scroll(val delta: Short) : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_SCROLL, 0) + beBytes(delta)
}

data object LeftClick : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_LEFT_CLICK, 0)
}

data object MiddleClick : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_MIDDLE_CLICK, 0)
}

data object RightClick : RemoteEvent {
    override fun encode() = byteArrayOf(RemoteEvent.OP_RIGHT_CLICK, 0)
}

private fun beBytes(v: Short) = byteArrayOf(((v.toInt() ushr 8) and 0xFF).toByte(),
    (v.toInt() and 0xFF).toByte())

private fun beShort(buf: ByteArray, i: Int) =
    (((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)).toShort()
