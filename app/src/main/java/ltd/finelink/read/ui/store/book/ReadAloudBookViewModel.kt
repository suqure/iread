package ltd.finelink.read.ui.store.book

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.data.entities.LLMConfig
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.toastOnUi

class ReadAloudBookViewModel(application: Application) : BaseViewModel(application) {
    val waitDialogData = MutableLiveData<Boolean>()
    val readAloudBookData = MutableLiveData<ReadAloudBook>()
    val speakerListData = MutableLiveData<List<BookSpeaker>>()
    val bookData = MutableLiveData<Book>()
    var llm = MutableLiveData<LLMConfig>()



    fun initData(intent: Intent) {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl") ?: ""
            appDb.llmConfigDao.get(-1)?.let {
                if(it.download){
                     if(it.local==null||!FileUtils.exist(it.local!!)){
                         it.download = false
                         it.progress = 0;
                         appDb.llmConfigDao.update(it)
                     }
                }
                llm.postValue(it)
            }
            if (bookUrl.isNotBlank()) {
                var readAloudBook = appDb.readAloudBookDao.get(bookUrl)
                appDb.bookSpeakerDao.findByBook(bookUrl)?.let {
                    speakerListData.postValue(it)
                }
                appDb.bookDao.getBook(bookUrl)?.let {
                    bookData.postValue(it)
                    if(readAloudBook==null){
                        var chapters = LocalBook.getChapterList(it)
                        readAloudBook = ReadAloudBook(it.bookUrl,0,0,0,chapters.size,0,0)
                        appDb.readAloudBookDao.insert(readAloudBook!!)
                    }else if(it.totalChapterNum>0){
                        readAloudBook?.let {rb->
                            rb.totalChapterNum = it.totalChapterNum
                            appDb.readAloudBookDao.update(rb)
                        }
                    }
                    readAloudBookData.postValue(readAloudBook!!)
                    return@execute
                }
            }
            throw NoStackTraceException("未找到书籍")
        }.onError {
            AppLog.put(it.localizedMessage, it)
            context.toastOnUi(it.localizedMessage)
        }
    }

    fun clearCache() {
        execute {
            bookData.value?.let {
                var caches = appDb.ttsCacheDao.searchByBook(it.bookUrl)
                for(cache in caches){
                    FileUtils.delete(cache.file)
                }
                appDb.ttsCacheDao.deleteByBook(it.bookUrl)
            }
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }.onError {
            context.toastOnUi("清理缓存出错\n${it.localizedMessage}")
        }
    }

    fun clearLLm(){
        execute {
            llm.value?.let {
                FileUtils.delete(it.local!!,true)
                it.download = false
                it.progress= 0
                appDb.llmConfigDao.update(it)
                llm.postValue(it)
            }
        }.onSuccess {
            context.toastOnUi(R.string.clear_cache_success)
        }.onError {
            context.toastOnUi("清理引擎出错\n${it.localizedMessage}")
        }

    }




}
