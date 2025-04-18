package ltd.finelink.read.help.tts

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.help.tiktoken.Tokenizer
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import okio.ByteString
import okio.ByteString.Companion.encodeUtf8
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class FishSpeech  @JvmOverloads constructor(
    private var localTTS: LocalTTS
) : TTSModel {
    private var tts: Module? = null
    private var gpt: Module? = null
    private var config: FishConfig? = null
    private var speaker: Tensor? = null
    private val speakerMap = HashMap<Long,Tensor>()
    private var init = false
    private var repetitionPenalty:Tensor? = null
    private var topP: Tensor? = null
    private var temperature: Tensor? = null
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
        val ranks = mutableMapOf<ByteString, Int>()
        val specials = mutableMapOf<ByteString, Int>()
        var modelConfig: ModelConfig? = null
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path,"vocab.txt")))
            var count = 0
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    ranks[Tokenizer.coverVocabToByString(line)] = count
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
                modelConfig.symbols.forEachIndexed { index, s ->
                    specials[s.encodeUtf8()] = index
                }
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }


        if (modelConfig != null) {
            config = FishConfig(
                ranks,specials,
                modelConfig.data!!.samplingRate!!
            )
        }
        initSpeaker()
        initParams()
        tts = Module.load(getIFilePath(path,"sovits.pt"))
        gpt = Module.load(getIFilePath(path,"llm.pt"))
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
                var convert = gson.fromJson(sb.toString(), FishFeature::class.java)
                speaker = Tensor.fromBlob(convert.source,convert.shape)
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
    }

    private fun initParams(){
        var shape = longArrayOf(1)
        repetitionPenalty = Tensor.fromBlob(floatArrayOf(1.5f),shape)
        topP = Tensor.fromBlob(floatArrayOf(localTTS.topP!!),shape)
        temperature = Tensor.fromBlob(floatArrayOf(localTTS.temperature!!),shape)
    }

    override fun tts(text: String,speakerId:Long): FloatArray {
        var normtext = config!!.textNormalizer(text,getLang())
        normtext = config!!.prepareGenerate(normtext)
        val tokens = config!!.getInput(normtext);
        val semantic: IValue = gpt!!.forward(
            IValue.from(tokens),
            IValue.from(topP),
            IValue.from(repetitionPenalty),
            IValue.from(temperature),
            IValue.from(getSpeaker(speakerId))
        )
        val length = Tensor.fromBlob(longArrayOf(semantic.toTensor().shape()[1]),longArrayOf(1))
        val ttsResult: IValue = tts!!.forward(semantic,IValue.from(length))
        return addSilence(ttsResult.toTensor().dataAsFloatArray)
    }



    private fun loadSpeaker(speakerId:Long):Tensor?{
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
                        var convert = gson.fromJson(sb.toString(), FishFeature::class.java)
                        return Tensor.fromBlob(convert.source,convert.shape)
                    }
                } catch (e: IOException) {
                    AppLog.put("读取文件出错",e,false)
                }
            }
        }

        return null
    }

    private fun getSpeaker(speakerId:Long):Tensor{
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