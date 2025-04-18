package ltd.finelink.read.ui.book.source.debug

import android.app.Application
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.model.Debug

class BookSourceDebugModel(application: Application) : BaseViewModel(application),
    Debug.Callback {

    var bookSource: BookSource? = null
    private var callback: ((Int, String) -> Unit)? = null
    var searchSrc: String? = null
    var bookSrc: String? = null
    var tocSrc: String? = null
    var contentSrc: String? = null

    fun init(sourceUrl: String?, finally: () -> Unit) {
        sourceUrl?.let {
            //优先使用这个，不会抛出异常
            execute {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }.onFinally {
                finally.invoke()
            }
        }
    }

    fun observe(callback: (Int, String) -> Unit) {
        this.callback = callback
    }

    fun startDebug(key: String, start: (() -> Unit)? = null, error: (() -> Unit)? = null) {
        execute {
            Debug.callback = this@BookSourceDebugModel
            Debug.startDebug(this, bookSource!!, key)
        }.onStart {
            start?.invoke()
        }.onError {
            error?.invoke()
        }
    }

    override fun printLog(state: Int, msg: String) {
        when (state) {
            10 -> searchSrc = msg
            20 -> bookSrc = msg
            30 -> tocSrc = msg
            40 -> contentSrc = msg
            else -> callback?.invoke(state, msg)
        }
    }

    override fun onCleared() {
        super.onCleared()
        Debug.cancelDebug(true)
    }

}
