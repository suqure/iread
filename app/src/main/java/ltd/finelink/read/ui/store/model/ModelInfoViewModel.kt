package ltd.finelink.read.ui.store.model

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers.IO
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.File

class ModelInfoViewModel(application: Application) : BaseViewModel(application) {
    val modelData = MutableLiveData<LocalTTS>()
    val waitDialogData = MutableLiveData<Boolean>()
    private val ttsFolderPath: String by lazy {
        appCtx.cacheDir.absolutePath + File.separator + "AppTTS" + File.separator
    }

    fun initData(intent: Intent) {
        execute {
            val id = intent.getLongExtra("id",0)
            appDb.localTTSDao.get(id)?.let {
                upBook(it)
                return@execute
            }

            throw NoStackTraceException("未找到书籍")
        }.onError {
            AppLog.put(it.localizedMessage, it)
            context.toastOnUi(it.localizedMessage)
        }
    }



    private fun upBook(book: LocalTTS) {
        execute {
            modelData.postValue(book)
            upCoverByRule(book)
        }
    }

    private fun upCoverByRule(book: LocalTTS) {
        execute {
            if (book.cover.isNullOrBlank()) {

                modelData.postValue(book)

            }
        }
    }

    fun refreshBook(book: LocalTTS) {
        execute(executeContext = IO) {
            appDb.localTTSDao.get(book.id)?.let {
                upBook(it)
            }
        }.onError {
            AppLog.put("下载模型<${book.name}>失败", it)
        }
    }



    fun saveBook(book: LocalTTS?, success: (() -> Unit)? = null) {
        book ?: return
        execute {
            appDb.localTTSDao.update(book)
        }.onSuccess {
            success?.invoke()
        }
    }





    fun getModel(toastNull: Boolean = true): LocalTTS? {
        val book = modelData.value
        if (toastNull && book == null) {
            context.toastOnUi("book is null")
        }
        return book
    }


    fun clearCache() {
        execute {
            modelData.value?.let {
                var caches = appDb.ttsCacheDao.searchByModel(it.id)
                for(cache in caches){
                    FileUtils.delete(cache.file)
                }
                appDb.ttsCacheDao.deleteByModel(it.id)
            }
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }.onError {
            context.toastOnUi("清理缓存出错\n${it.localizedMessage}")
        }
    }




}
