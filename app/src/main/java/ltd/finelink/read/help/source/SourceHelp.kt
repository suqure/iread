package ltd.finelink.read.help.source

import android.os.Handler
import android.os.Looper
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BaseSource
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.utils.EncoderUtils
import ltd.finelink.read.utils.NetworkUtils
import ltd.finelink.read.utils.splitNotBlank
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx

object SourceHelp {

    private val handler = Handler(Looper.getMainLooper())
    private val list18Plus by lazy {
        try {
            return@lazy String(appCtx.assets.open("18PlusList.txt").readBytes())
                .splitNotBlank("\n").map {
                    EncoderUtils.base64Decode(it)
                }.toHashSet()
        } catch (e: Exception) {
            return@lazy hashSetOf<String>()
        }
    }

    fun getSource(key: String?): BaseSource? {
        key ?: return null
        return appDb.bookSourceDao.getBookSource(key)
            ?: appDb.rssSourceDao.getByKey(key)
    }

    fun insertRssSource(vararg rssSources: RssSource) {
        val rssSourcesGroup = rssSources.groupBy {
            is18Plus(it.sourceUrl)
        }
        rssSourcesGroup[true]?.forEach {
            handler.post {
                appCtx.toastOnUi("${it.sourceName}是18+网址,禁止导入.")
            }
        }
        rssSourcesGroup[false]?.let {
            appDb.rssSourceDao.insert(*it.toTypedArray())
        }
    }

    fun insertBookSource(vararg bookSources: BookSource) {
        val bookSourcesGroup = bookSources.groupBy {
            is18Plus(it.bookSourceUrl)
        }
        bookSourcesGroup[true]?.forEach {
            handler.post {
                appCtx.toastOnUi("${it.bookSourceName}是18+网址,禁止导入.")
            }
        }
        bookSourcesGroup[false]?.let {
            appDb.bookSourceDao.insert(*it.toTypedArray())
        }
    }

    private fun is18Plus(url: String?): Boolean {
        url ?: return false
        val baseUrl = NetworkUtils.getBaseUrl(url) ?: return false
        kotlin.runCatching {
            val host = baseUrl.split("//", ".").let {
                if (it.size > 2) "${it[it.lastIndex - 1]}.${it.last()}" else return false
            }
            return list18Plus.contains(host)
        }
        return false
    }

    /**
     * 调整排序序号
     */
    fun adjustSortNumber() {
        if (
            appDb.bookSourceDao.maxOrder > 99999
            || appDb.bookSourceDao.minOrder < -99999
        ) {
            val sources = appDb.bookSourceDao.allPart
            sources.forEachIndexed { index, bookSource ->
                bookSource.customOrder = index
            }
            appDb.bookSourceDao.upOrder(sources)
        }
    }

}