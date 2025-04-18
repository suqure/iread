package ltd.finelink.read.help.tts

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.externalFiles
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class ConvertTTS @JvmOverloads constructor(
    private var localTTS: LocalTTS
) : TTSModel {
    private var convert: Module? = null
    private var ext: Module? = null
    private var base: TTSModel? = null
    private var init = false
    private var config: ModelConfig?=null
    private var src: IValue?=null
    private var tgt: IValue?=null
    private var tau: IValue?=null
    private var refer: LocalTTS?=null
    private val resample: WavResampleRate = WavResampleRate()
    private val tgtMap =  HashMap<Long, IValue>()


    private val speakerFolderPath: String by lazy {
        appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator
    }
    private val ttsFolderPath: String by lazy {
        appCtx.cacheDir.absolutePath + File.separator + "AppTTS" + File.separator
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
                initParams()
                initTarget()
                refer = appDb.localTTSDao.get(localTTS.refId!!);
                initRefer(refer!!);
            }
        }
    }


    override fun init() {
        if (init) {
            return
        }
        init = true
        val path = localTTS.local!!
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
                config = gson.fromJson(sb.toString(), ModelConfig::class.java)

            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
        initTarget();
        initParams()
        convert = Module.load(getIFilePath(path,"convert.pt"))
        ext = Module.load(getIFilePath(path,"ext.pt"))
        refer = appDb.localTTSDao.get(localTTS.refId!!);
        initRefer(refer!!)

    }

    private fun initRefer(refer:LocalTTS){
        if(base==null){
            base = NormalTTS(refer)
        }else{
            base!!.refresh(refer)
            src = null
        }
        base!!.init()
    }

    private fun initTarget(){
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
                var speaker = gson.fromJson(sb.toString(), ConvertFeature::class.java)
                var data = Tensor.fromBlob(speaker.source,speaker.shape)
                tgt = IValue.from(data)
            }
        } catch (e: IOException) {
            AppLog.put("读取文件出错",e,false)
        }
    }


    private fun getTargetBySpeakerId(speakerId:Long):IValue?{
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
                        var speaker = gson.fromJson(sb.toString(), ConvertFeature::class.java)
                        var data = Tensor.fromBlob(speaker.source,speaker.shape)
                        return IValue.from(data)
                    }
                } catch (e: IOException) {
                    AppLog.put("读取文件出错",e,false)
                }
            }
        }

        return null;
    }

    private fun getSpeaker(speakerId:Long=0):IValue{
        if(speakerId>0){
            tgtMap[speakerId]?.let {
                return it
            }
            getTargetBySpeakerId(speakerId)?.let {
                tgtMap[speakerId] = it
                return it
            }
        }

        return tgt!!
    }

    private fun initParams(){
        var shape = longArrayOf(1)
        var temperature = Tensor.fromBlob(floatArrayOf(localTTS.temperature!!),shape)
        tau = IValue.from(temperature)
    }

    override fun tts(text: String,speaker: Long): FloatArray {
        val fileName = md5SpeakFileName(text)
        var audio:FloatArray? = null;
        if(hasSpeakFile(fileName)){
            audio = resample.loadAudio("${ttsFolderPath}$fileName.wav", sampleRate(), -1)
        }else{
            audio = base!!.tts(text)
            var sourceRate = base!!.sampleRate();
            if(sourceRate!=sampleRate()){
                audio = resample.resamplePcm(audio,sourceRate,sampleRate())
            }
        }
        val shape = longArrayOf(audio.size.toLong())
        var source =  IValue.from(Tensor.fromBlob(audio,shape))
        if(src==null){
            src = ext!!.forward(source)
        }
        val ttsResult: IValue = convert!!.forward(source,src,getSpeaker(speaker),tau);
        return ttsResult.toTensor().dataAsFloatArray
    }

    private fun md5SpeakFileName(content: String): String {
        return MD5Utils.md5Encode16(ReadBook.curTextChapter?.title ?: "") + "_" +
                MD5Utils.md5Encode16("${refer?.id}-|-$${refer?.speakerId}-|-$content")
    }

    private fun hasSpeakFile(name: String): Boolean {
        return FileUtils.exist("${ttsFolderPath}$name.wav")
    }

    override fun sampleRate(): Int {
        return config!!.data!!.samplingRate!!
    }

    override fun getLang(): String {
        return base!!.getLang()
    }

    override fun speakerId(): Long {
        return localTTS.speakerId?:0
    }
}