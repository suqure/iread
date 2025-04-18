package ltd.finelink.read.help.tts

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class ChatTTS  @JvmOverloads constructor(
    private var localTTS: LocalTTS
) : TTSModel {
    private var tts: Module? = null
    private var gpt: Module? = null
    private var config: ChatConfig? = null
    private var speaker: ChatSpeaker? = null
    private val speakerMap = HashMap<Long,ChatSpeaker>()
    private var init = false
    private var refineText = false
    private var topK:Tensor? = null
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
        val bertTokenMap: MutableMap<String, Int> = HashMap()
        var modelConfig: ModelConfig? = null
        try {
            val br = BufferedReader(InputStreamReader(getInputStream(path,"vocab.txt")))
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
            config = ChatConfig(
                bertTokenMap,
                modelConfig.data!!.samplingRate!!
            )
        }
        initSpeaker()
        initParams()
        tts = Module.load(getIFilePath(path,"sovits.pt"))
        gpt = Module.load(getIFilePath(path,"gpt.pt"))
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
                var convert = gson.fromJson(sb.toString(), ChatFeature::class.java)
                speaker = toChatSpeaker(convert)
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
    }

    private fun initParams(){
        var shape = longArrayOf(1)
        topK = Tensor.fromBlob(intArrayOf(localTTS.topK!!),shape)
        topP = Tensor.fromBlob(floatArrayOf(localTTS.topP!!),shape)
        temperature = Tensor.fromBlob(floatArrayOf(localTTS.temperature!!),shape)
        refineText = localTTS.refineText
    }

    override fun tts(text: String,speakerId:Long): FloatArray {
        var normtext = config!!.textNormalizer(text,getLang())
        var speaker = getSpeaker(speakerId)
        if(refineText){
            normtext = refineText(normtext).replace("[Stts]", "")
                .replace("[spk_emb]", "")
                .replace("[empty_spk]", "").trim()
        }
        normtext = config!!.prepareGenerate(normtext, spkPrompt = speaker.prompt)
        val infer = config!!.getInput(normtext);
        val semantic: IValue = gpt!!.forward(
            IValue.from(speaker.speaker),
            IValue.from(infer[0]),
            IValue.from(infer[1]),
            IValue.from(temperature),
            IValue.from(topP),
            IValue.from(topK)
        )
        var hiddens = semantic.toTuple()[1]
        val ttsResult: IValue = tts!!.forward(IValue.from(hiddens.toTensorList()[0]))
        return addSilence(ttsResult.toTensor().dataAsFloatArray)
    }

    private fun refineText(text:String):String{
        var normtext = config!!.prepareRefineText(text)
        val infer = config!!.getInput(normtext);
        val semantic: IValue = gpt!!.forward(
            IValue.from(speaker!!.speaker),
            IValue.from(infer[0]),
            IValue.from(infer[1]),
            IValue.from(temperature),
            IValue.from(topP),
            IValue.from(topK)
        )
        var ids = semantic.toTuple()[0]
        var tokens = ids.toTensorList()[0].dataAsLongArray
        return config!!.convertTokensToText(tokens, getLang())
    }

    private fun loadSpeaker(speakerId:Long):ChatSpeaker?{
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
                        var convert = gson.fromJson(sb.toString(), ChatFeature::class.java)
                        return toChatSpeaker(convert)
                    }
                } catch (e: IOException) {
                    AppLog.put("读取文件出错",e,false)
                }
            }
        }

        return null
    }


    private fun toChatSpeaker(convert:ChatFeature):ChatSpeaker{
        convert.prompt?.let {
            return ChatSpeaker(Tensor.fromBlob(covertToIntArray(convert.source),convert.shape),it)
        }
        return ChatSpeaker(Tensor.fromBlob(convert.source,convert.shape),convert.prompt)
    }

    private fun covertToIntArray(arrays:FloatArray):IntArray{
        val result = IntArray(arrays.size)
        for(i in arrays.indices){
            result[i] = arrays[i].toInt()
        }
        return  result;
    }

    private fun getSpeaker(speakerId:Long):ChatSpeaker{
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
data class ChatSpeaker(val speaker: Tensor, val prompt: String?=null)