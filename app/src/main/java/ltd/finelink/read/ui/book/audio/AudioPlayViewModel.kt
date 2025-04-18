package ltd.finelink.read.ui.book.audio

import android.app.Application
import android.content.Intent
import androidx.lifecycle.viewModelScope
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.BookType
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.help.book.removeType
import ltd.finelink.read.model.AudioPlay
import ltd.finelink.read.model.AudioPlay.durChapter
import ltd.finelink.read.model.webBook.WebBook
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO

class AudioPlayViewModel(application: Application) : BaseViewModel(application) {

    fun initData(intent: Intent) = AudioPlay.apply {
        execute {
            val bookUrl = intent.getStringExtra("bookUrl") ?: return@execute
            val book = appDb.bookDao.getBook(bookUrl) ?: return@execute
            inBookshelf = intent.getBooleanExtra("inBookshelf", true)
            initBook(book)
        }.onFinally {
            saveRead()
        }
    }

    private fun initBook(book: Book) {
        val isSameBook = AudioPlay.book?.bookUrl == book.bookUrl
        if (isSameBook) {
            AudioPlay.upData(context, book)
        } else {
            AudioPlay.resetData(context, book)
        }
        if (durChapter == null) {
            if (book.tocUrl.isEmpty()) {
                loadBookInfo(book)
            } else {
                loadChapterList(book)
            }
        }
    }

    private fun loadBookInfo(book: Book) {
        val bookSource = AudioPlay.bookSource ?: return
        WebBook.getBookInfo(viewModelScope, bookSource, book).onSuccess(IO) {
            loadChapterList(book)
        }
    }

    private fun loadChapterList(book: Book) {
        val bookSource = AudioPlay.bookSource ?: return
        WebBook.getChapterList(viewModelScope, bookSource, book).onSuccess(IO) { cList ->
            book.save()
            appDb.bookChapterDao.insert(*cList.toTypedArray())
            AudioPlay.upDurChapter(book)
        }.onError {
            context.toastOnUi(R.string.error_load_toc)
        }
    }

    fun upSource() {
        execute {
            val book = AudioPlay.book ?: return@execute
            AudioPlay.bookSource = book.getBookSource()
        }
    }

    fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        execute {
            AudioPlay.book?.migrateTo(book, toc)
            book.removeType(BookType.updateError)
            AudioPlay.book?.delete()
            appDb.bookDao.insert(book)
            AudioPlay.book = book
            AudioPlay.bookSource = source
            appDb.bookChapterDao.insert(*toc.toTypedArray())
            AudioPlay.upDurChapter(book)
        }.onFinally {
            postEvent(EventBus.SOURCE_CHANGED, book.bookUrl)
        }
    }

    fun removeFromBookshelf(success: (() -> Unit)?) {
        execute {
            AudioPlay.book?.let {
                appDb.bookDao.delete(it)
            }
        }.onSuccess {
            success?.invoke()
        }
    }

}