package ltd.finelink.read.ui.store.book

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.utils.postEvent

class BookSpeakerEditViewModel(app: Application) : BaseViewModel(app) {

    val bookData = MutableLiveData<BookSpeaker>()

    fun initData(arguments: Bundle?, success: () -> Unit) {
        execute {
            val id = arguments?.getLong("id",0)
             id?.let {
                 appDb.bookSpeakerDao.get(id)?.let {
                     bookData.postValue(it)
                 }
             }
        }.onSuccess {
            it?.let {
                success.invoke()
            }
        }
    }

    fun save(bookSpeaker: BookSpeaker, success: (() -> Unit)? = null) {
        execute {
            appDb.bookSpeakerDao.update(bookSpeaker)
            postEvent(EventBus.UP_BOOK_SPEAKER,  bookSpeaker)
        }.onSuccess {
            success?.invoke()
        }
    }




}