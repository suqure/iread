package ltd.finelink.read.ui.book.read.config

import android.app.Application
import android.os.Bundle
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.utils.getClipText
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.toastOnUi

class LocalTtsEditViewModel(app: Application) : BaseViewModel(app) {

    var id: Long? = null

    fun initData(arguments: Bundle?, success: (localTTS: LocalTTS) -> Unit) {
        execute {
            if (id == null) {
                val argumentId = arguments?.getLong("id")
                if (argumentId != null && argumentId != 0L) {
                    id = argumentId
                    return@execute appDb.localTTSDao.get(argumentId)
                }
            }
            return@execute null
        }.onSuccess {
            it?.let {
                success.invoke(it)
            }
        }
    }

    fun save(localTTS: LocalTTS, success: (() -> Unit)? = null) {
        id = localTTS.id
        execute {
            appDb.localTTSDao.update(localTTS)
            if (ReadAloud.ttsEngine == localTTS.id.toString()) ReadAloud.upReadAloudClass()
        }.onSuccess {
            success?.invoke()
        }
    }

    fun importFromClip(onSuccess: (localTTS: LocalTTS) -> Unit) {
        val text = context.getClipText()
        if (text.isNullOrBlank()) {
            context.toastOnUi("剪贴板为空")
        } else {
            importSource(text, onSuccess)
        }
    }

    fun importSource(text: String, onSuccess: (localTTS: LocalTTS) -> Unit) {
        val text1 = text.trim()
        execute {
            when {
                text1.isJsonObject() -> {
                    LocalTTS.fromJson(text1).getOrThrow()
                }
                text1.isJsonArray() -> {
                    LocalTTS.fromJsonArray(text1).getOrThrow().first()
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