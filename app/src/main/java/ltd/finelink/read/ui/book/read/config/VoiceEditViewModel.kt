package ltd.finelink.read.ui.book.read.config

import android.app.Application
import android.os.Bundle
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.toastOnUi

class VoiceEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (speaker: TTSSpeaker) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    return@execute appDb.ttsSpeakerDao.get(argumentId)
                }else{
                    return@execute TTSSpeaker(name= context.getString(R.string.new_name),type=2)
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(speaker: TTSSpeaker, success: (() -> Unit)? = null) {
        id = speaker.id
        execute {
            appDb.ttsSpeakerDao.insert(speaker)
        }.onSuccess {
            success?.invoke()
        }
    }


    fun importSource(text: String, onSuccess: (speaker: TTSSpeaker) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    TTSSpeaker.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    TTSSpeaker.fromJsonArray(text1).getOrThrow().first()
                }
                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onSuccess {
            onSuccess.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }
    }

}