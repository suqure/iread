package ltd.finelink.read.ui.book.explore

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ltd.finelink.read.BuildConfig
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.SearchBook
import ltd.finelink.read.model.webBook.WebBook
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.stackTraceStr
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.mapLatest
import java.util.concurrent.ConcurrentHashMap


@OptIn(ExperimentalCoroutinesApi::class)
class ExploreShowViewModel(application: Application) : BaseViewModel(application) {
    val bookshelf: MutableSet<String> = ConcurrentHashMap.newKeySet()
    val upAdapterLiveData = MutableLiveData<String>()
    val booksData = MutableLiveData<List<SearchBook>>()
    val errorLiveData = MutableLiveData<String>()
    private var bookSource: BookSource? = null
    private var exploreUrl: String? = null
    private var page = 1

    init {
        execute {
            appDb.bookDao.flowAll().mapLatest { books ->
                val keys = arrayListOf<String>()
                books.forEach {
                    keys.add("${it.name}-${it.author}")
                    keys.add(it.name)
                }
                keys
            }.catch {
                AppLog.put("发现列表界面获取书籍数据失败\n${it.localizedMessage}", it)
            }.collect {
                bookshelf.clear()
                bookshelf.addAll(it)
                upAdapterLiveData.postValue("isInBookshelf")
            }
        }.onError {
            AppLog.put("加载书架数据失败", it)
        }
    }

    fun initData(intent: Intent) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            exploreUrl = intent.getStringExtra("exploreUrl")
            if (bookSource == null && sourceUrl != null) {
                bookSource = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            explore()
        }
    }

    fun explore() {
        val source = bookSource
        val url = exploreUrl
        if (source == null || url == null) return
        WebBook.exploreBook(viewModelScope, source, url, page)
            .timeout(if (BuildConfig.DEBUG) 0L else 30000L)
            .onSuccess(IO) { searchBooks ->
                booksData.postValue(searchBooks)
                appDb.searchBookDao.insert(*searchBooks.toTypedArray())
                page++
            }.onError {
                it.printOnDebug()
                errorLiveData.postValue(it.stackTraceStr)
            }
    }

    fun isInBookShelf(name: String, author: String): Boolean {
        return if (author.isNotBlank()) {
            bookshelf.contains("$name-$author")
        } else {
            bookshelf.contains(name)
        }
    }

}
