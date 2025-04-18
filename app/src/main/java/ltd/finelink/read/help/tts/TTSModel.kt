package ltd.finelink.read.help.tts

import ltd.finelink.read.data.entities.LocalTTS
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream

interface TTSModel {

    fun modelId():Long

    fun type():Int

    fun refresh(localTTS: LocalTTS)

    fun init()
    fun tts(text: String,speakerId:Long=0):FloatArray

    fun sampleRate():Int

    fun getLang():String

    fun speakerId():Long

    fun getInputStream(path:String, name:String): InputStream {
        if(path == "asset"){
            return appCtx.assets.open(name)
        }
        return FileInputStream(path+ File.separator+name)
    }

    fun addSilence(audio:FloatArray,second:Float=0.5f): FloatArray{
        var size:Int = (sampleRate()*second).toInt()+audio.size
        var result = FloatArray(size)
        for(i in result.indices){
            if(i<audio.size){
                result[i] = audio[i]
            }else{
                result[i] = 0f
            }

        }
        return  result;
    }

    fun getIFilePath(path:String, name:String):String{
        if(path == "asset"){
            val file = File(appCtx.filesDir, name)
            if (file.exists() && file.length() > 0) {
                return file.absolutePath
            }
            appCtx.assets.open(name!!).use { `is` ->
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
        return path+ File.separator+name
    }
}