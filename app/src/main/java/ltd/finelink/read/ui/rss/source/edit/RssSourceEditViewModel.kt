package ltd.finelink.read.ui.rss.source.edit

import android.app.Application
import android.content.Intent
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.RuleComplete
import ltd.finelink.read.help.http.CookieStore
import ltd.finelink.read.utils.*

import kotlinx.coroutines.Dispatchers
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.getClipText
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.stackTraceStr
import ltd.finelink.read.utils.toastOnUi


class RssSourceEditViewModel(application: Application) : BaseViewModel(application) {
    var autoComplete = false
    var rssSource: RssSource? = null

    fun initData(intent: Intent, onFinally: () -> Unit) {
        execute {
            val key = intent.getStringExtra("sourceUrl")
            if (key != null) {
                appDb.rssSourceDao.getByKey(key)?.let {
                    rssSource = it
                }
            }
        }.onFinally {
            onFinally()
        }
    }

    fun save(source: RssSource, success: ((RssSource) -> Unit)) {
        execute {
            if (source.sourceName.isBlank() || source.sourceName.isBlank()) {
                throw NoStackTraceException(context.getString(R.string.non_null_name_url))
            }
            rssSource?.let {
                appDb.rssSourceDao.delete(it)
            }
            appDb.rssSourceDao.insert(source)
            rssSource = source
            source
        }.onSuccess {
            success(it)
        }.onError {
            context.toastOnUi(it.localizedMessage)
            it.printOnDebug()
        }
    }

    fun pasteSource(onSuccess: (source: RssSource) -> Unit) {
        execute(context = Dispatchers.Main) {
            var source: RssSource? = null
            context.getClipText()?.let { json ->
                source = GSON.fromJsonObject<RssSource>(json).getOrThrow()
            }
            source
        }.onError {
            context.toastOnUi(it.localizedMessage)
        }.onSuccess {
            if (it != null) {
                onSuccess(it)
            } else {
                context.toastOnUi("格式不对")
            }
        }
    }

    fun importSource(text: String, finally: (source: RssSource) -> Unit) {
        execute {
            val text1 = text.trim()
            GSON.fromJsonObject<RssSource>(text1).getOrThrow().let {
                finally.invoke(it)
            }
        }.onError {
            context.toastOnUi(it.stackTraceStr)
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