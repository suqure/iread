package ltd.finelink.read.ui.main.bookshelf

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.isActive
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.text
import ltd.finelink.read.model.webBook.WebBook
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.NetworkUtils
import ltd.finelink.read.utils.fromJsonArray
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.toastOnUi
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import kotlin.collections.set

class BookshelfViewModel(application: Application) : BaseViewModel(application) {
    val addBookProgressLiveData = MutableLiveData(-1)
    var addBookJob: Coroutine<*>? = null

    fun addBookByUrl(bookUrls: String) {
        var successCount = 0
        addBookJob = execute {
            val hasBookUrlPattern: List<BookSource> by lazy {
                appDb.bookSourceDao.hasBookUrlPattern
            }
            val urls = bookUrls.split("\n")
            for (url in urls) {
                val bookUrl = url.trim()
                if (bookUrl.isEmpty()) continue
                if (appDb.bookDao.getBook(bookUrl) != null) {
                    successCount++
                    continue
                }
                val baseUrl = NetworkUtils.getBaseUrl(bookUrl) ?: continue
                var source = appDb.bookSourceDao.getBookSourceAddBook(baseUrl)
                if (source == null) {
                    for (bookSource in hasBookUrlPattern) {
                        try {
                            if (bookUrl.matches(bookSource.bookUrlPattern!!.toRegex())) {
                                source = bookSource
                                break
                            }
                        } catch (_: Exception) {
                        }
                    }
                }
                val bookSource = source ?: continue
                val book = Book(
                    bookUrl = bookUrl,
                    origin = bookSource.bookSourceUrl,
                    originName = bookSource.bookSourceName
                )
                kotlin.runCatching {
                    WebBook.getBookInfoAwait(bookSource, book)
                }.onSuccess {
                    it.order = appDb.bookDao.minOrder - 1
                    it.save()
                    successCount++
                    addBookProgressLiveData.postValue(successCount)
                }
            }
        }.onSuccess {
            if (successCount > 0) {
                context.toastOnUi(R.string.success)
            } else {
                context.toastOnUi("添加网址失败")
            }
        }.onError {
            AppLog.put("添加网址出错\n${it.localizedMessage}", it, true)
        }.onFinally {
            addBookProgressLiveData.postValue(-1)
        }
    }

    fun exportBookshelf(books: List<Book>?, success: (file: File) -> Unit) {
        execute {
            books?.let {
                val path = "${context.filesDir}/books.json"
                FileUtils.delete(path)
                val file = FileUtils.createFileWithReplace(path)
                FileOutputStream(file).use { out ->
                    val writer = JsonWriter(OutputStreamWriter(out, "UTF-8"))
                    writer.setIndent("  ")
                    writer.beginArray()
                    books.forEach {
                        val bookMap = hashMapOf<String, String?>()
                        bookMap["name"] = it.name
                        bookMap["author"] = it.author
                        bookMap["intro"] = it.getDisplayIntro()
                        GSON.toJson(bookMap, bookMap::class.java, writer)
                    }
                    writer.endArray()
                    writer.close()
                }
                file
            } ?: throw NoStackTraceException("书籍不能为空")
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi("导出书籍出错\n${it.localizedMessage}")
        }
    }

    fun importBookshelf(str: String, groupId: Long) {
        execute {
            val text = str.trim()
            when {
                text.isAbsUrl() -> {
                    okHttpClient.newCallResponseBody {
                        url(text)
                    }.text().let {
                        importBookshelf(it, groupId)
                    }
                }

                text.isJsonArray() -> {
                    importBookshelfByJson(text, groupId)
                }

                else -> {
                    throw NoStackTraceException("格式不对")
                }
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }

    private fun importBookshelfByJson(json: String, groupId: Long) {
        execute {
            val bookSources = appDb.bookSourceDao.allEnabled
            GSON.fromJsonArray<Map<String, String?>>(json).getOrThrow().forEach { bookInfo ->
                if (!isActive) return@execute
                val name = bookInfo["name"] ?: ""
                val author = bookInfo["author"] ?: ""
                if (name.isNotEmpty() && appDb.bookDao.getBook(name, author) == null) {
                    WebBook.preciseSearch(this, bookSources, name, author)
                        .onSuccess {
                            val book = it.first
                            if (groupId > 0) {
                                book.group = groupId
                            }
                            book.save()
                        }.onError { e ->
                            context.toastOnUi(e.localizedMessage)
                        }
                }
            }
        }.onError {
            it.printOnDebug()
        }.onFinally {
            context.toastOnUi(R.string.success)
        }
    }

}
