package ltd.finelink.read.help.tts

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.entities.LocalTTS
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class NormalTTS @JvmOverloads constructor(
    private var localTTS: LocalTTS
) : TTSModel {
    private var bert: Module? = null
    private var tts: Module? = null
    private var config: T2sConfig? = null
    private var init = false
    private var speed: Tensor? = null
    private var speakerId: Int = 0
    override fun modelId(): Long {
        return localTTS.id
    }

    override fun type(): Int {
        return localTTS.type
    }

    override fun refresh(newLocalTTS: LocalTTS) {
        if (localTTS.id != newLocalTTS.id) {
            localTTS = newLocalTTS
            init = false
        } else {
            localTTS = newLocalTTS
            if (init) {
                initParams()
            }
        }
    }


    override fun init() {
        if (init) {
            return
        }
        init = true
        val path = localTTS.local!!
        val symbolMap: MutableMap<String, Int> = HashMap()
        val bertTokenMap: MutableMap<String, Int> = HashMap()
        val pinyinMap: MutableMap<String, String> = HashMap()
        val phoneticMap: MutableMap<String, String> = HashMap()
        var modelConfig: ModelConfig? = null
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path, "vocab.txt")))
            var count = 0
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    bertTokenMap[line] = count
                } else {
                    break
                }
                count++
            }
            br.close()
        } catch (e: IOException) {
            AppLog.put("读取文件出错", e, false)
        }
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path, "opencpop-strict.txt")))
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    val value = line.split("\t".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                    pinyinMap[value[0]] = value[1]
                } else {
                    break
                }
            }
            br.close()
        } catch (e: IOException) {
            AppLog.put("读取文件出错", e, false)
        }

        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path, "cmudict.rep")))
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    val value =
                        line.split("  ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    phoneticMap[value[0]] = value[1]
                } else {
                    break
                }
            }
            br.close()
        } catch (e: IOException) {
            AppLog.put("读取文件出错", e, false)
        }

        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path, "config.json")))
            val sb = StringBuilder()
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    sb.append(line)
                } else {
                    break
                }
            }
            br.close()
            if (sb.isNotEmpty()) {
                val gson = GsonBuilder()
                    .setFieldNamingStrategy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    .create()
                modelConfig = gson.fromJson(sb.toString(), ModelConfig::class.java)
                for (i in 0 until modelConfig.symbols.size) {
                    symbolMap[modelConfig.symbols[i]] = i
                }
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错", e, false)
        }
        if (modelConfig != null) {
            modelConfig.data?.spk2id?.forEach { (s, i) ->
                speakerId = i;
            }
            config = T2sConfig(
                symbolMap,
                bertTokenMap,
                pinyinMap,
                phoneticMap,
                modelConfig.data!!.samplingRate!!,
                modelConfig.data!!.addBlank!!,
            )
        }
        tts = Module.load(getIFilePath(path, "tts.pt"))
        try {
            bert = Module.load(getIFilePath(path, "bert.pt"))
        } catch (e: Exception) {
            AppLog.put("读取文件出错", e, false)
        }
        initParams()
    }

    private fun initParams() {
        var shape = longArrayOf(1)
        speed = Tensor.fromBlob(floatArrayOf(localTTS.speed!!), shape)
    }

    override fun tts(text: String,spkId:Long): FloatArray {
        val data: G2PData = when (getLang()) {
            "zh" -> config!!.g2pMix(text)
            else -> config!!.g2pEnMix(text)
        }
        val inputs = data.text?.let { config!!.getBertInput(it) }
        val infer = config!!.getTtsInferTensor(data)
        val berts = bert?.let {
            val b: IValue = it.forward(
                IValue.from(inputs!![0]),
                IValue.from(inputs[1]),
                IValue.from(inputs[2]),
                IValue.from(infer[3])
            )
            config!!.getInferBert(b.toTensor())
        }?:config!!.getEmptyBert(infer[0].shape()[0])
        val speaker = intArrayOf(speakerId)
        val speakerShape = longArrayOf(1)
        val st = Tensor.fromBlob(speaker, speakerShape)
        val ttsResult = tts!!.forward(
            IValue.from(infer[0]),
            IValue.from(st),
            IValue.from(infer[1]),
            IValue.from(infer[2]),
            IValue.from(berts[0]),
            IValue.from(berts[1]),
            IValue.from(speed)
        )
        return addSilence(ttsResult.toTensor().dataAsFloatArray)
    }

    override fun sampleRate(): Int {
        return config?.sampleRate!!
    }

    override fun getLang(): String {
        return localTTS.mainLang ?: "zh"
    }

    override fun speakerId(): Long {
        return localTTS.speakerId?:0
    }
}