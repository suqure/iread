package ltd.finelink.read.help.tts

import androidx.annotation.Keep
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Keep
internal object ByteHelper {
    fun GetIntFromBytes(data: ByteArray?, byteOrder: ByteOrder): Int {
        if (data == null || data.size == 0) {
            return 0
        }
        val dataBuffer = ByteBuffer.wrap(data)
        dataBuffer.order(byteOrder)
        return when (data.size) {
            1 -> dataBuffer.get().toInt()
            2 -> dataBuffer.getShort().toInt()
            3 -> {
                val buf = ByteArray(3)
                dataBuffer[buf]

                // Bytes are sign extended and must be masked when packed into the integer.
                if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                    buf[2].toInt() shl 16 or (buf[1].toInt() and 0xff shl 8) or (buf[0]
                        .toInt() and 0xff)
                } else buf[0].toInt() shl 16 or (buf[1]
                    .toInt() and 0xff shl 8) or (buf[2].toInt() and 0xff)
            }

            4 -> dataBuffer.getInt()
            else -> throw IllegalArgumentException()
        }
    }

    fun GetUnsignedIntFromByte(data: Byte): Int {
        return data.toInt() and 0xff
    }

    fun GetNumberBytes(data: Int, byteOrder: ByteOrder?, byteCount: Int): ByteArray {
        val dataBytes = ByteArray(byteCount)
        for (i in 0 until byteCount) {
            dataBytes[i] = (data shr i * 8).toByte()
        }
        return dataBytes
    }

    fun GetIntBytes(data: Int, byteOrder: ByteOrder?): ByteArray {
        val buffer = ByteBuffer.allocate(Integer.BYTES)
        buffer.order(byteOrder)
        buffer.putInt(data)
        return buffer.array()
    }

    fun GetShortBytes(data: Short, byteOrder: ByteOrder?): ByteArray {
        val buffer = ByteBuffer.allocate(java.lang.Short.BYTES)
        buffer.order(byteOrder)
        buffer.putShort(data)
        return buffer.array()
    }

    fun GetASCIIBytes(data: String, byteOrder: ByteOrder?): ByteArray? {
        return try {
            data.toByteArray(charset("US-ASCII"))
        } catch (e: UnsupportedEncodingException) {
            null
        }
    }

    fun GetZeroByte(signed: Boolean): Byte {
        return (if (signed) 0 else 0 and 0xff).toByte()
    }
}
