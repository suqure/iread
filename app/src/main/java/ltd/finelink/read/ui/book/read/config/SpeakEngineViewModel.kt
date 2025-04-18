package ltd.finelink.read.ui.book.read.config

import android.app.Application
import android.net.Uri
import android.speech.tts.TextToSpeech
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.DefaultData
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.text
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.readText
import ltd.finelink.read.utils.toastOnUi

class SpeakEngineViewModel(application: Application) : BaseViewModel(application) {

    val sysEngines: List<TextToSpeech.EngineInfo> by lazy {
        val tts = TextToSpeech(context, null)
        val engines = tts.engines
        tts.shutdown()
        engines
    }



    fun import(text: String) {
        when {
            text.isJsonArray() -> {
                LocalTTS.fromJsonArray(text).getOrThrow().let {
                    appDb.localTTSDao.insert(*it.toTypedArray())
                }
            }
            text.isJsonObject() -> {
                LocalTTS.fromJson(text).getOrThrow().let {
                    appDb.localTTSDao.insert(it)
                }
            }
            else -> {
                throw NoStackTraceException("格式不对")
            }
        }
    }

}