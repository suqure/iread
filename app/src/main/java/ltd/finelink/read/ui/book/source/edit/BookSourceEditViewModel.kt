package ltd.finelink.read.ui.book.source.edit

import android.app.Application
import android.content.Intent
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.RuleComplete
import ltd.finelink.read.help.config.SourceConfig
import ltd.finelink.read.help.http.CookieStore
import ltd.finelink.read.help.http.newCallStrResponse
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.storage.ImportOldData
import ltd.finelink.read.utils.*
import kotlinx.coroutines.Dispatchers
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonArray
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.getClipText
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.toastOnUi


class BookSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var bookSource: BookSource? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val sourceUrl = intent.getStringExtra("sourceUrl")
            var source: BookSource? = null
            if (sourceUrl != null) {
                source = appDb.bookSourceDao.getBookSource(sourceUrl)
            }
            source?.let {
                bookSource = it
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: BookSource, success: ((BookSource) -> Unit)? = null) {
        execute {
            if (source.bookSourceUrl.isBlank() || source.bookSourceName.isBlank()) {
                throw NoStackTraceException(context.getString(R.string.non_null_name_url))
            }
            if (!source.equal(bookSource ?: BookSource())) {
                source.lastUpdateTime = System.currentTimeMillis()
            }
            bookSource?.let {
                appDb.bookSourceDao.delete(it)
                SourceConfig.removeSource(it.bookSourceUrl)
            }
            appDb.bookSourceDao.insert(source)
            bookSource = source
            source
        }.onSuccess {
            success?.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: BookSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            val text = context.getClipText()
            if (text.isNullOrBlank()) {
                throw NoStackTraceException("剪贴板为空")
            } else {
                importSource(text, onSuccess)
            }
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
            it.printOnDebug()
        }
    }

    fun importSource(text: String, finally: (source: BookSource) -> Unit) {
        execute {
            importSource(text)
        }.onSuccess {
            finally.invoke(it)
        }.onError {
            context.toastOnUi(it.localizedMessage ?: "Error")
            it.printOnDebug()
        }
    }

    suspend fun importSource(text: String): BookSource {
        return when {
            text.isAbsUrl() -> {
                val text1 = okHttpClient.newCallStrResponse { url(text) }.body
                importSource(text1!!)
            }

            text.isJsonArray() -> {
                if (text.contains("ruleSearchUrl") || text.contains("ruleFindUrl")) {
                    val items: List<Map<String, Any>> = jsonPath.parse(text).read("$")
                    val jsonItem = jsonPath.parse(items[0])
                    ImportOldData.fromOldBookSource(jsonItem)
                } else {
                    GSON.fromJsonArray<BookSource>(text).getOrThrow()[0]
                }
            }

            text.isJsonObject() -> {
                if (text.contains("ruleSearchUrl") || text.contains("ruleFindUrl")) {
                    val jsonItem = jsonPath.parse(text)
                    ImportOldData.fromOldBookSource(jsonItem)
                } else {
                    GSON.fromJsonObject<BookSource>(text).getOrThrow()
                }
            }

            else -> throw NoStackTraceException("格式不对")
        }
    }

    fun clearCookie(url: String) {
        execute {
            CookieStore.removeCookie(url)
        }
    }

    fun ruleComplete(rule: String?, preRule: String? = null, type: Int = 1): String? {
        if (autoComplete) {
            return RuleComplete.autoComplete(rule, preRule, type)
        }
        return rule
    }

}