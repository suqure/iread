package ltd.finelink.read.ui.store.book.speaker

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.toastOnUi


class SpeakerDetailViewModel(application: Application) : BaseViewModel(application) {


    val bookData = MutableLiveData<Book>()

    fun initData(intent: Intent) {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl") ?: ""
            if (bookUrl.isNotBlank()) {
                appDb.bookDao.getBook(bookUrl)?.let {
                    bookData.postValue(it)
                    return@execute
                }
            }
            throw NoStackTraceException("未找到书籍")
        }.onError {
            AppLog.put(it.localizedMessage, it)
            context.toastOnUi(it.localizedMessage)
        }
    }
    fun clearAll() {
        execute {
            bookData.value?.let {
                appDb.bookSpeakerDetailDao.deleteByBook(it.bookUrl)
                appDb.bookSpeakerDao.deleteByBook(it.bookUrl)
                postEvent(EventBus.UP_BOOK_SPEAKER_DETAIL,it.bookUrl)
            }

        }
    }
    fun del(vararg sources: BookSpeakerDetail) {
        execute {
            var speakers:MutableSet<String> = mutableSetOf()
            var bookUrl = ""
            for(source in sources){
                speakers.add(source.spkName)
                appDb.bookSpeakerDetailDao.delete(source)
                bookUrl = source.bookUrl
            }
            for (speaker in speakers){
                var count = appDb.bookSpeakerDetailDao.countSpeaker(bookUrl,speaker)
                if(count==0){
                    appDb.bookSpeakerDao.deleteByBookAndSpeaker(bookUrl,speaker)
                }
            }
            postEvent(EventBus.UP_BOOK_SPEAKER_DETAIL,bookUrl)

        }
    }



}