package ltd.finelink.read.utils

import ltd.finelink.read.help.tts.WavResampleRate
import splitties.init.appCtx
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavFileUtils {

    private val resample: WavResampleRate = WavResampleRate()

    @Throws(IOException::class)
    fun rawToWave(file: String, data: FloatArray, sampleRate: Int) {
        // creating the empty wav file.
        val waveFile = FileUtils.createFileIfNotExist(file)
        rawToWave(FileOutputStream(waveFile), data, sampleRate)
    }

    @Throws(IOException::class)
    fun rawToWave(out: OutputStream, data: FloatArray, sampleRate: Int) {

        var output: DataOutputStream? = null
        try {
            output = DataOutputStream(out)
            // WAVE header
            // chunk id
            writeString(output, "RIFF")
            // chunk size
            writeInt(output, 36 + data.size * 2)
            // format
            writeString(output, "WAVE")
            // subchunk 1 id
            writeString(output, "fmt ")
            // subchunk 1 size
            writeInt(output, 16)
            // audio format (1 = PCM)
            writeShort(output, 1.toShort())
            // number of channels
            writeShort(output, 1.toShort())
            // sample rate
            writeInt(output, sampleRate)
            // byte rate
            writeInt(output, sampleRate * 2)
            // block align
            writeShort(output, 2.toShort())
            // bits per sample
            writeShort(output, 16.toShort())
            // subchunk 2 id
            writeString(output, "data")
            // subchunk 2 size
            writeInt(output, data.size * 2)
            val shortData = floatArray2ShortArray(data)
            for (i in shortData.indices) {
                writeShort(output, shortData[i])
            }
        } finally {
            output?.close()
        }
    }

    @Throws(IOException::class)
    private fun writeInt(output: DataOutputStream, value: Int) {
        output.write(value)
        output.write(value shr 8)
        output.write(value shr 16)
        output.write(value shr 24)
    }

    @Throws(IOException::class)
    private fun writeShort(output: DataOutputStream, value: Short) {
        output.write(value.toInt())
        output.write(value.toInt() shr 8)
    }

    @Throws(IOException::class)
    private fun writeString(output: DataOutputStream, value: String) {
        for (element in value) {
            output.write(element.code)
        }
    }

    private fun floatArray2ShortArray(values: FloatArray): ShortArray {
        val ret = ShortArray(values.size)
        for (i in values.indices) {
            values[i] = values[i] * Short.MAX_VALUE
            ret[i] = values[i].toInt().toShort()
        }
        return ret
    }

    @Throws(IOException::class)
    fun floatArray2ByteArray(data: FloatArray): ByteArray {
        val byteStream = ByteArrayOutputStream()
        val output = DataOutputStream(byteStream)
        val shortData: ShortArray = floatArray2ShortArray(data)
        for (i in shortData.indices) {
            writeShort(output, shortData[i])
        }
        byteStream.close()
        output.close()
        return byteStream.toByteArray()

    }

    private fun byteArray2FloatArray(data: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val result = FloatArray(data.size / 2)
        var i = 0
        while (buffer.remaining() > 0) {
            val s = buffer.getShort()
            result[i] = s.toFloat() / Short.MAX_VALUE
            i++
        }
        return result
    }

    @Throws(IOException::class)
    fun combineWav(sources: List<String>, targetFile: String, sampleRate: Int = -1) {
        combineWav(sources, FileOutputStream(targetFile), sampleRate)
    }

    @Throws(IOException::class)
    fun combineWav(sources: List<String>, out: OutputStream, sampleRate: Int = -1) {
        var datas: MutableList<FloatArray> = java.util.ArrayList()
        var targetSr = sampleRate
        var size = 0
        for (i in sources.indices) {
            var data = resample.loadAudio(sources[i], targetSr)
            if (i == 0) {
                targetSr = resample.sampleRate;
            }
            size += data.size
            datas.add(data)
        }
        val target = FloatArray(size)
        var offset = 0
        for (i in datas.indices) {
            var source = datas[i]
            System.arraycopy(source, 0, target, offset, source.size)
            offset += source.size
        }
        rawToWave(out, target, targetSr)
    }

    @Throws(IOException::class)
    fun rawToCombine(sources: List<String>, out: OutputStream, sampleRate: Int = -1) {
        var output: DataOutputStream? = null
        val file = FileUtils.createFileIfNotExist(appCtx.cacheDir,"wav.tmp")
        try {
            output = DataOutputStream(out)
            var targetSr = sampleRate
            var size = 0
            val to = DataOutputStream(file.outputStream())
            for (i in sources.indices) {
                var data = resample.loadAudio(sources[i], targetSr)
                if (i == 0) {
                    targetSr = resample.sampleRate;
                }
                size += data.size
                val shortData = floatArray2ShortArray(data)
                for (i in shortData.indices) {
                    writeShort(to, shortData[i])
                }
            }
            to.flush()
            to.close()
            writeHead(output, targetSr, size)
            val ins = DataInputStream(file.inputStream())
            var buffer = ByteArray(1024)
            while(ins.read(buffer)!=-1){
                output.write(buffer)
            }
        } finally {
            output?.close()
            file.delete()
        }
    }



    private fun writeHead(output: DataOutputStream, sampleRate: Int, size: Int) {
        // WAVE header
        // chunk id
        writeString(output, "RIFF")
        // chunk size
        writeInt(output, 36 + size * 2)
        // format
        writeString(output, "WAVE")
        // subchunk 1 id
        writeString(output, "fmt ")
        // subchunk 1 size
        writeInt(output, 16)
        // audio format (1 = PCM)
        writeShort(output, 1.toShort())
        // number of channels
        writeShort(output, 1.toShort())
        // sample rate
        writeInt(output, sampleRate)
        // byte rate
        writeInt(output, sampleRate * 2)
        // block align
        writeShort(output, 2.toShort())
        // bits per sample
        writeShort(output, 16.toShort())
        // subchunk 2 id
        writeString(output, "data")
        // subchunk 2 size
        writeInt(output, size * 2)
    }


    private fun toFloatArray(data: List<Float>): FloatArray {
        val result = FloatArray(data.size)
        for (i in data.indices) {
            result[i] = data[i]
        }
        return result
    }


}