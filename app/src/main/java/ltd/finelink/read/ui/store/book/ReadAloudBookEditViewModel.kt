package ltd.finelink.read.ui.store.book

import android.app.Application
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.utils.postEvent

class ReadAloudBookEditViewModel(app: Application) : BaseViewModel(app) {

    val bookData = MutableLiveData<ReadAloudBook>()

    fun initData(arguments: Bundle?, success: () -> Unit) {
        execute {
            val bookUrl = arguments?.getString("id")
             bookUrl?.let {
                 appDb.readAloudBookDao.get(bookUrl)?.let {
                     bookData.postValue(it)
                 }
             }
        }.onSuccess {
            it?.let {
                success.invoke()
            }
        }
    }

    fun save(readAloudBook: ReadAloudBook, success: (() -> Unit)? = null) {
        execute {
            appDb.readAloudBookDao.update(readAloudBook)
            postEvent(EventBus.UP_ALOUD_BOOK,  readAloudBook)
        }.onSuccess {
            success?.invoke()
        }
    }




}