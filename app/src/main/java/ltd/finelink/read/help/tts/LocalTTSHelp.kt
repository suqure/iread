package ltd.finelink.read.help.tts

import com.google.gson.Gson
import ltd.finelink.read.data.entities.LocalTTS
import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.Random

object LocalTTSHelp {

    private var model: TTSModel? = null

    private var ext: Module? = null

    private var spkArray: FloatArray? = null

    private var random: Random = Random()

    const val extsr:Int = 22050

    fun loadModel(localTTS: LocalTTS): TTSModel {
        if (model?.modelId() == localTTS.id) {
            return model as TTSModel
        } else {
              if (localTTS.type == 1) {
                model = NormalTTS(localTTS)
            } else if (localTTS.type == 2)  {
                model = ConvertTTS(localTTS)
            }else  if (localTTS.type == 3) {
                model = ChatTTS(localTTS)
            }else  if (localTTS.type == 4){
                model = CosyVoiceTTS(localTTS)
            }else{
                model = FishSpeech(localTTS)
            }
        }
        return model as TTSModel
    }

    fun refreshModel(localTTS: LocalTTS) {
        if (model?.type() == localTTS.type) {
            model!!.refresh(localTTS)
        } else {
            model = null
        }
    }

    fun loadExtModel(){
        if (ext == null) {
            ext = Module.load(getAssetPath("ext.pt"))
        }
    }
    fun loadSpkModel(){
        if(spkArray == null){
            val br = BufferedReader(InputStreamReader(FileInputStream(getAssetPath("spk.json"))))
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
                var convert = gson.fromJson(sb.toString(), ConvertFeature::class.java)
                spkArray = convert.source
            }
        }
    }

    fun extAudioFeature(audio:FloatArray): ConvertFeature {
        loadExtModel()
        return ext?.let {
            val shape = longArrayOf(audio.size.toLong())
            var source =  IValue.from(Tensor.fromBlob(audio,shape))
            var result = it.forward(source).toTensor()

            var feature = ConvertFeature()
            feature.shape = result.shape()
            feature.source= result.dataAsFloatArray
            return feature
        }!!
    }

    fun spkAudioFeature(seed:Long=2): ConvertFeature {
        loadSpkModel()
        spkArray?.let {
            var target = FloatArray(it.size/2)
            random.setSeed(seed)
            for(i in target.indices){
                var source = random.nextGaussian().toFloat()
                target[i] = source* it[i]+it[i+target.size]
            }
            var feature = ConvertFeature()
            feature.shape = longArrayOf(target.size.toLong())
            feature.source=  target
            return feature
        }!!
    }

    private fun getAssetPath(name: String): String {
        val file = File(appCtx.filesDir, name)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        appCtx.assets.open(name).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }

    }

}