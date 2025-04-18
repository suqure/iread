package ltd.finelink.read.help.tts

import androidx.annotation.Keep
import ltd.finelink.read.help.tts.ByteHelper.GetIntBytes
import ltd.finelink.read.help.tts.ByteHelper.GetIntFromBytes
import ltd.finelink.read.help.tts.ByteHelper.GetNumberBytes
import ltd.finelink.read.help.tts.ByteHelper.GetShortBytes
import ltd.finelink.read.help.tts.MathHelper.InterpolateLinear
import ltd.finelink.read.help.tts.MathHelper.Round
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import kotlin.math.max
import kotlin.math.min

@Keep
class WavResampleRate {
    var sampleRate = 0
    private var channels = 1
    private var bitsPerSample = 16
    private val DecimalPlaces = 6
    private var segmentOffset = 0.0
    private var decimationRate = 0.0
    var dataSize = 0
    private var _reader: DataInputStream? = null
    private var lastFrameProcessed: ByteArray? = null

    @Throws(IOException::class)
    fun loadAudioData(filePath: String?, sr: Int): ByteArray {
        return loadAudioData(FileInputStream(filePath), sr, -1, 0)
    }

    @Throws(IOException::class)
    fun loadAudioData(filePath: String?, sr: Int, readDurationInSec: Int): ByteArray {
        return loadAudioData(FileInputStream(filePath), sr, readDurationInSec, 0)
    }

    @Throws(IOException::class)
    fun loadAudioData(
        filePath: String?,
        sr: Int,
        readDurationInSec: Int,
        offsetDuration: Int
    ): ByteArray {
        return loadAudioData(FileInputStream(filePath), sr, readDurationInSec, offsetDuration)
    }

    @Throws(IOException::class)
    fun loadAudioData(
        `in`: InputStream?,
        sr: Int,
        readDurationInSec: Int,
        offsetDuration: Int
    ): ByteArray {
        return try {
            _reader = DataInputStream(BufferedInputStream(`in`))
            val header = readHeader()
            if (!parseHeader(header)) {
                throw RuntimeException("Unknow Header format")
            }
            decimationRate = Round(sampleRate.toDouble() / sr, DecimalPlaces)

            // The segment offset is initialized at half the decimation rate.
            segmentOffset = Round(decimationRate / 2, DecimalPlaces)
            if (sr > sampleRate) {
                segmentOffset *= -1.0
            }
            lastFrameProcessed = null
            val audio = readAudioBytes(readDurationInSec, offsetDuration)
            if (sr == -1 || sampleRate == sr) {
                audio
            } else {
                process(audio)
            }
        } finally {
            if (_reader != null) {
                _reader!!.close()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAudioBytes(readDurationInSec: Int, offsetDuration: Int): ByteArray {
        val outputStream = ByteArrayOutputStream()
        var current = 0
        var writeSecond = 0
        var bytesPopped: ByteArray? = null
        var bytesWritten = 0
        while (bytesWritten < dataSize) {
            bytesPopped = ByteArray(
                min(_reader!!.available().toDouble(), (frameSize * sampleRate).toDouble())
                    .toInt()
            )
            _reader!!.read(bytesPopped)
            if (current >= offsetDuration) {
                outputStream.write(bytesPopped)
                writeSecond++
            }
            if (readDurationInSec > 0 && writeSecond >= readDurationInSec) {
                break
            }
            current++
            bytesWritten += bytesPopped.size
        }
        outputStream.close()
        return outputStream.toByteArray()
    }

    @Throws(IOException::class)
    fun loadAudio(filePath: String?, sr: Int): FloatArray {
        return loadAudio(FileInputStream(filePath), sr, -1, 0)
    }

    @Throws(IOException::class)
    fun loadAudio(filePath: String?, sr: Int, readDurationInSec: Int): FloatArray {
        return loadAudio(FileInputStream(filePath), sr, readDurationInSec, 0)
    }

    @Throws(IOException::class)
    fun loadAudio(
        filePath: String?,
        sr: Int,
        readDurationInSec: Int,
        offsetDuration: Int
    ): FloatArray {
        return loadAudio(FileInputStream(filePath), sr, readDurationInSec, offsetDuration)
    }

    @Throws(IOException::class)
    fun loadAudio(
        `in`: InputStream?,
        sr: Int,
        readDurationInSec: Int,
        offsetDuration: Int
    ): FloatArray {
        val data = loadAudioData(`in`, sr, readDurationInSec, offsetDuration)
        return byteToFloat(data)
    }

    private fun byteToFloat(data: ByteArray): FloatArray {
        val inputBuffer = ByteBuffer.wrap(data)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val samples = FloatArray(data.size / frameSize)
        var i = 0
        while (inputBuffer.hasRemaining()) {
            var `val` = 0.0
            for (channel in 0 until channels) {
                if (bitsPerSample <= 8) {
                    `val` += (inputBuffer.get().toInt() and 0xff).toDouble()
                } else if (bitsPerSample <= 16) {
                    `val` += inputBuffer.getShort().toDouble()
                } else if (bitsPerSample <= 24) {
                    val sampleBytes = ByteArray(3)
                    inputBuffer[sampleBytes]
                    `val` += GetIntFromBytes(sampleBytes, ByteOrder.LITTLE_ENDIAN).toDouble()
                } else {
                    `val` = inputBuffer.getInt().toDouble()
                }
            }
            samples[i] = `val`.toFloat() / (channels * Short.MAX_VALUE)
            i++
        }
        return samples
    }

    private val channelSize: Int
        get() = bitsPerSample / 8
    private val frameSize: Int
        get() = channelSize * channels

    @Throws(UnsupportedEncodingException::class)
    private fun parseHeader(header: ByteArray): Boolean {
        val headerBuffer = ByteBuffer.wrap(header)
        val field = ByteArray(4)
        val longField = ByteArray(16)
        val shortField = ByteArray(2)
        // RIFF Chunk ID
        headerBuffer[field]
        if (String(field, charset("US-ASCII")) != "RIFF") {
            return false
        }
        // RIFF Chunk Size
        headerBuffer[field]
        // WAVE ID
        headerBuffer[field]
        if (String(field, charset("US-ASCII")) != "WAVE") {
            return false
        }
        // FMT Chunk ID
        do {
            if (!headerBuffer.hasRemaining()) {
                return false
            }
            headerBuffer[field]
        } while (String(field, charset("US-ASCII")) != "fmt ")
        // FMT Chunk Size
        headerBuffer[field]
        val fmtChunkSize = GetIntFromBytes(field, ByteOrder.LITTLE_ENDIAN)
        if (fmtChunkSize != 16) {
            return false
        }
        // Audio Format Code
        headerBuffer[shortField]
        val audioFormat = GetIntFromBytes(shortField, ByteOrder.LITTLE_ENDIAN)
        if (audioFormat != 1) {
            return false
        }
        // Number of Channels
        headerBuffer[shortField]
        channels = GetIntFromBytes(shortField, ByteOrder.LITTLE_ENDIAN)
        // Sampling Rate
        headerBuffer[field]
        sampleRate = GetIntFromBytes(field, ByteOrder.LITTLE_ENDIAN)
        // Data Rate
        headerBuffer[field]
        // Block Size
        headerBuffer[shortField]
        // Bits Per Sample
        headerBuffer[shortField]
        bitsPerSample = GetIntFromBytes(shortField, ByteOrder.LITTLE_ENDIAN)
        if (fmtChunkSize >= 18) {
            // cbSize (Extension Size)
            headerBuffer[shortField]
            if (fmtChunkSize >= 40) {
                // Valid Bits Per Sample
                headerBuffer[shortField]
                // Speaker Position Mask
                headerBuffer[field]
                // SubFormat
                headerBuffer[longField]
            }
        }
        // Data Chunk ID
        do {
            if (!headerBuffer.hasRemaining()) {
                return false
            }
            headerBuffer[field]
        } while (String(field, charset("US-ASCII")) != "data")

        // Data Chunk Size
        headerBuffer[field]
        dataSize = GetIntFromBytes(field, ByteOrder.LITTLE_ENDIAN)
        return true
    }

    private fun pop(size: Int): ByteArray? {
        return try {
            val buffer = ByteArray(size)
            _reader!!.read(buffer)
            buffer
        } catch (e: IOException) {
            null
        }
    }

    @Throws(IOException::class)
    private fun readHeader(): ByteArray {
        val headerStream = ByteArrayOutputStream()
        // Secondary buffers for parsing values
        var subChunkIDField: ByteArray?
        var subChunkSizeField: ByteArray?
        var subChunkSize = 0

        // Parse the RIFF chunk
        subChunkIDField = pop(4)
        headerStream.write(subChunkIDField)
        if (String(subChunkIDField!!, charset("US-ASCII")) != "RIFF") {
            throw RuntimeException("Unknow Header format")
        }
        headerStream.write(pop(8))
        do {
            headerStream.write(pop(subChunkSize))
            if (_reader!!.available() < 4) {
                throw RuntimeException("Unknow Header format")
            }
            subChunkIDField = pop(4)
            headerStream.write(subChunkIDField)
            subChunkSizeField = pop(4)
            subChunkSize =
                subChunkSizeField!![0].toInt() and 0xFF or (subChunkSizeField[1].toInt() and 0xFF shl 8
                        ) or (subChunkSizeField[2].toInt() and 0xFF shl 16) or (subChunkSizeField[3].toInt() and 0xFF shl 24)
            headerStream.write(subChunkSizeField)
        } while (String(subChunkIDField!!, charset("US-ASCII")) != "data")
        return headerStream.toByteArray()
    }

    private fun process(input: ByteArray): ByteArray {
        val inputBuffer = ByteBuffer.wrap(input)
        inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val outputStream = ByteArrayOutputStream()
        // Iteration
        var frameCount = 0
        // O(n) - Iterate through frames, dependent on n (input length).
        while (true) {
            val framePointer = Round(segmentOffset + frameCount * decimationRate, DecimalPlaces)
            // The decimal portion of the frame pointer.
            val framePointerDecimal = Round(framePointer % 1, DecimalPlaces)
            val weight = Round((framePointerDecimal + 1) % 1, DecimalPlaces)

            // The frame numbers used to perform interpolation.
            val leftFrameNumber = Math.round(framePointer - weight).toInt()
            val rightFrameNumber = leftFrameNumber + 1
            var size = leftFrameNumber
            if (framePointerDecimal > 0) {
                size = rightFrameNumber
            }
            if (input.size < size * frameSize + frameSize || input.isEmpty() && Math.round(
                    framePointerDecimal
                ).toInt() >= 0
            ) {
                break
            }
            val leftSamples =
                if (leftFrameNumber < 0) readFrameSamples(lastFrameProcessed) else readFrameSamples(
                    inputBuffer,
                    leftFrameNumber
                )
            val rightSamples =
                if (weight > 0) readFrameSamples(inputBuffer, rightFrameNumber) else null
            val interpolatedSamples = IntArray(channels)

            // O(1) - Iterate through channels, independent from n.
            for (channel in 0 until channels) {
                interpolatedSamples[channel] = InterpolateLinear(
                    leftSamples?.get(channel) ?: 0,
                    rightSamples?.get(channel) ?: 0, weight
                )
                try {
                    // Write interpolated samples to the output stream.
                    if (bitsPerSample <= 8) {
                        outputStream.write(interpolatedSamples[channel].toByte().toInt())
                    } else if (bitsPerSample <= 16) {
                        outputStream.write(
                            GetShortBytes(
                                interpolatedSamples[channel].toShort(),
                                ByteOrder.LITTLE_ENDIAN
                            )
                        )
                    } else if (bitsPerSample <= 24) {
                        outputStream.write(
                            GetNumberBytes(interpolatedSamples[channel], ByteOrder.LITTLE_ENDIAN, 3)
                        )
                    } else {
                        // Assuming 32-bit signed integers.
                        outputStream
                            .write(
                                GetIntBytes(
                                    interpolatedSamples[channel],
                                    ByteOrder.LITTLE_ENDIAN
                                )
                            )
                    }
                } catch (e: Exception) {
                    throw RuntimeException("write output stream error")
                }
            }
            frameCount++
            inputBuffer.position(
                max((leftFrameNumber * frameSize).toDouble(), 0.0).toInt()
            )
        }
        if (input.size >= frameSize) {
            lastFrameProcessed = Arrays.copyOfRange(input, input.size - frameSize, input.size)
            segmentOffset -= Round(
                (input.size / (decimationRate * frameSize) - frameCount) * decimationRate,
                DecimalPlaces
            )
        }
        return outputStream.toByteArray()
    }

    private fun readFrameSamples(buffer: ByteBuffer, frameNumber: Int): IntArray? {
        val position = frameNumber * frameSize
        if (buffer.limit() < position + frameSize) {
            return null
        }
        buffer.position(position)
        val samples = IntArray(channels)
        for (channel in 0 until channels) {
            if (bitsPerSample <= 8) {
                samples[channel] = buffer.get().toInt() and 0xff
            } else if (bitsPerSample <= 16) {
                samples[channel] = buffer.getShort().toInt()
            } else if (bitsPerSample <= 24) {
                val sampleBytes = ByteArray(3)
                buffer[sampleBytes]
                samples[channel] = GetIntFromBytes(sampleBytes, ByteOrder.LITTLE_ENDIAN)
            } else {
                // Assuming 32-bit signed integers.
                samples[channel] = buffer.getInt()
            }
        }
        return samples
    }

    private fun readFrameSamples(frame: ByteArray?): IntArray? {
        return if (frame == null) {
            null
        } else readFrameSamples(ByteBuffer.wrap(frame), 0)
    }

    fun resamplePcm(
        audio: FloatArray,
        sourceSr: Int,
        targetSr: Int,
        channels: Int=1,
        bitsPerSample: Int=16
    ): FloatArray {
        this.channels = channels
        this.bitsPerSample = bitsPerSample
        this.sampleRate = sourceSr
        if (sourceSr == targetSr) {
            return audio
        }
        var data = floatArray2ByteArray(audio)
        decimationRate = Round(sourceSr.toDouble() / targetSr, DecimalPlaces)

        // The segment offset is initialized at half the decimation rate.
        segmentOffset = Round(decimationRate / 2, DecimalPlaces)
        if (targetSr > sourceSr) {
            segmentOffset *= -1.0
        }
        lastFrameProcessed = null
        return byteToFloat(process(data))
    }

    private fun floatArray2ByteArray(data: FloatArray): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val output = DataOutputStream(byteStream)
        for (i in data.indices) {
            data[i] = data[i] * Short.MAX_VALUE
            var shortVal = data[i].toInt().toShort()
            output.write(shortVal.toInt())
            output.write(shortVal.toInt() shr 8)
        }
        byteStream.close()
        output.close()
        return byteStream.toByteArray()

    }
}
