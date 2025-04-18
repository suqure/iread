package ltd.finelink.read.help.tts

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import okio.ByteString
import okio.ByteString.Companion.decodeBase64
import okio.ByteString.Companion.encodeUtf8
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class CosyVoiceTTS  @JvmOverloads constructor(
    private var localTTS: LocalTTS
) : TTSModel {
    private var tts: Module? = null
    private var llm: Module? = null
    private var config: CosyVoiceConfig? = null
    private var speaker:  Array<Tensor>? = null
    private val speakerMap = HashMap<Long, Array<Tensor>>()
    private var init = false
    private val speakerFolderPath: String by lazy {
        appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator
    }
    override fun modelId(): Long {
        return localTTS.id
    }

    override fun type(): Int {
        return localTTS.type
    }

    override fun refresh(newLocalTTS: LocalTTS) {
        if(localTTS.id != newLocalTTS.id){
            localTTS = newLocalTTS
            init = false
        }else{
            localTTS = newLocalTTS
            if(init){
                initSpeaker()
            }
        }
    }


    override fun init() {
        if (init) {
            return
        }
        init = true
        val path = localTTS.local!!
        val ranks = mutableMapOf<ByteString, Int>()
        val specials = mutableMapOf<ByteString, Int>()
        var modelConfig: ModelConfig? = null
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path,"multilingual.tiktoken")))
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    val (encodedToken, rankString) = line.split(" ")
                    val token = encodedToken.decodeBase64() ?: error("can't decode $encodedToken")
                    val rank = rankString.toInt()
                    ranks[token] = rank
                } else {
                    break
                }
            }
            br.close()
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path,"vocab.txt")))
            var count = ranks.size
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    specials[line.encodeUtf8()] = count
                } else {
                    break
                }
                count++
            }
            br.close()
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }

        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path,"config.json")))
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
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }

        if (modelConfig != null) {
            config = CosyVoiceConfig(ranks,specials,modelConfig!!.data!!.samplingRate!!)
        }
        initSpeaker()
        llm = Module.load(getIFilePath(path,"llm.pt"))
        tts = Module.load(getIFilePath(path,"flow.pt"))


    }

    private fun initSpeaker(){
        try {
            var prefix = ""
            var file = "speaker.json"
            if(localTTS.speakerId!! >0){
                file = speakerFolderPath +localTTS.speaker
                if(!FileUtils.exist(file)){
                    file = "speaker.json"
                    prefix=  localTTS.local!!
                    localTTS.speakerId = 0
                }
            }else{
                prefix = localTTS.local!!
            }
            val br = BufferedReader(InputStreamReader(getInputStream(prefix,file)))
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
                val gson = Gson()
                var feature = gson.fromJson(sb.toString(), CosyVoiceSpeaker::class.java)
                speaker = arrayOf<Tensor>(
                    Tensor.fromBlob(feature.embedding, feature.shape),
                    Tensor.fromBlob(feature.prompt, feature.promptShape),
                    Tensor.fromBlob(intArrayOf(feature.prompt.size), longArrayOf(1)),
                    Tensor.fromBlob(feature.speech, feature.speechShape),
                    Tensor.fromBlob(intArrayOf(feature.speech.size), longArrayOf(1)),
                    Tensor.fromBlob(feature.feat, feature.featShape),
                    Tensor.fromBlob(intArrayOf(feature.featShape[1].toInt()), longArrayOf(1))
                )
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
    }



    override fun tts(text: String,speakerId:Long): FloatArray {
        var normtext = config!!.textNormalizer(text,getLang())
        val infer = config!!.getInput(normtext);
        val feature = getSpeaker(speakerId)
        val tokens: IValue = llm!!.forward(
            IValue.from(infer[0]),
            IValue.from(infer[1]),
            IValue.from(feature[0]),
            IValue.from(feature[1]),
            IValue.from(feature[2]),
            IValue.from(feature[3]),
            IValue.from(feature[4])
        )
        val tokenLens = Tensor.fromBlob(intArrayOf(tokens.toTensor().shape()[1].toInt()), longArrayOf(1))
        val ttsResult: IValue = tts!!.forward(
            tokens,
            IValue.from(tokenLens),
            IValue.from(feature[0]),
            IValue.from(feature[3]),
            IValue.from(feature[4]),
            IValue.from(feature[5]),
            IValue.from(feature[6]),
        )
        return ttsResult.toTensor().dataAsFloatArray
    }



    private fun loadSpeaker(speakerId:Long): Array<Tensor>?{
        appDb.ttsSpeakerDao.get(speakerId)?.let {
            var file = speakerFolderPath +it.speakerFile()
            if(FileUtils.exist(file)){
                try {
                    var prefix = ""
                    val br = BufferedReader(InputStreamReader(getInputStream(prefix,file)))
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
                        val gson = Gson()
                        var feature = gson.fromJson(sb.toString(), CosyVoiceSpeaker::class.java)
                        return  arrayOf<Tensor>(
                            Tensor.fromBlob(feature.embedding, feature.shape),
                            Tensor.fromBlob(feature.prompt, feature.promptShape),
                            Tensor.fromBlob(intArrayOf(feature.prompt.size), longArrayOf(1)),
                            Tensor.fromBlob(feature.speech, feature.speechShape),
                            Tensor.fromBlob(intArrayOf(feature.speech.size), longArrayOf(1)),
                            Tensor.fromBlob(feature.feat, feature.featShape),
                            Tensor.fromBlob(intArrayOf(feature.featShape[1].toInt()), longArrayOf(1))
                        )
                    }
                } catch (e: IOException) {
                    AppLog.put("读取文件出错",e,false)
                }
            }
        }

        return null
    }

    private fun getSpeaker(speakerId:Long): Array<Tensor>{
        if(speakerId>0){
            speakerMap[speakerId]?.let {
                return it
            }
            loadSpeaker(speakerId)?.let {
                speakerMap[speakerId] = it
                return it
            }
        }
        return speaker!!
    }

    override fun sampleRate(): Int {
         return config?.sampleRate!!
    }

    override fun getLang(): String {
        return localTTS.mainLang?:"zh"
    }

    override fun speakerId(): Long {
        return localTTS.speakerId?:0
    }

}