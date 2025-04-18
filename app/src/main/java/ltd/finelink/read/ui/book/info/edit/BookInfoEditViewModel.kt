package ltd.finelink.read.ui.book.info.edit

import android.app.Application
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.model.ReadBook

class BookInfoEditViewModel(application: Application) : BaseViewModel(application) {
    var book: Book? = null
    val bookData = MutableLiveData<Book>()

    fun loadBook(bookUrl: String) {
        execute {
            book = appDb.bookDao.getBook(bookUrl)
            book?.let {
                bookData.postValue(it)
            }
        }
    }

    fun saveBook(book: Book, success: (() -> Unit)?) {
        execute {
            if (ReadBook.book?.bookUrl == book.bookUrl) {
                ReadBook.book = book
            }
            appDb.bookDao.update(book)
        }.onSuccess {
            success?.invoke()
        }
    }
}