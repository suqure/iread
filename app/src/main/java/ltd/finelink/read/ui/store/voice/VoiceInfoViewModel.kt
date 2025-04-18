package ltd.finelink.read.ui.store.voice

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers.IO
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.toastOnUi

class VoiceInfoViewModel(application: Application) : BaseViewModel(application) {
    val modelData = MutableLiveData<TTSSpeaker>()
    val waitDialogData = MutableLiveData<Boolean>()

    fun initData(intent: Intent) {
        execute {
            val id = intent.getLongExtra("id",0)
            appDb.ttsSpeakerDao.get(id)?.let {
                upBook(it)
                return@execute
            }

            throw NoStackTraceException("未找到书籍")
        }.onError {
            AppLog.put(it.localizedMessage, it)
            context.toastOnUi(it.localizedMessage)
        }
    }

    fun upBook(intent: Intent) {
        execute {
            val id = intent.getLongExtra("id",0)
            appDb.ttsSpeakerDao.get(id)?.let {
                upBook(it)
            }


        }
    }

    private fun upBook(book: TTSSpeaker) {
        execute {
            modelData.postValue(book)
            upCoverByRule(book)
        }
    }

    private fun upCoverByRule(book: TTSSpeaker) {
        execute {
            if (book.cover.isNullOrBlank()) {
                modelData.postValue(book)
            }
        }
    }

    fun refreshBook(book: TTSSpeaker) {
        execute(executeContext = IO) {
            appDb.ttsSpeakerDao.get(book.id)?.let {
                upBook(it)
            }
        }.onError {
            AppLog.put("下载模型<${book.name}>失败", it)
        }
    }



    fun saveBook(book: TTSSpeaker?, success: (() -> Unit)? = null) {
        book ?: return
        execute {
            appDb.ttsSpeakerDao.update(book)
        }.onSuccess {
            success?.invoke()
        }
    }

    fun getModel(toastNull: Boolean = true): TTSSpeaker? {
        val book = modelData.value
        if (toastNull && book == null) {
            context.toastOnUi("book is null")
        }
        return book
    }

    fun clearCache() {
        execute {
            modelData.value?.let {
                var caches = appDb.ttsCacheDao.searchBySpeaker(it.id)
                for(cache in caches){
                    FileUtils.delete(cache.file)
                }
                appDb.ttsCacheDao.deleteBySpeaker(it.id)
            }
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }.onError {
            context.toastOnUi("清理缓存出错\n${it.localizedMessage}")
        }
    }

}
