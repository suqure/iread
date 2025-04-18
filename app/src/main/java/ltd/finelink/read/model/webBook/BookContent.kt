package ltd.finelink.read.model.webBook

import ltd.finelink.read.R
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.rule.ContentRule
import ltd.finelink.read.exception.ContentEmptyException
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.model.Debug
import ltd.finelink.read.model.analyzeRule.AnalyzeRule
import ltd.finelink.read.model.analyzeRule.AnalyzeUrl
import ltd.finelink.read.utils.HtmlFormatter
import ltd.finelink.read.utils.NetworkUtils
import ltd.finelink.read.utils.mapAsync
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.flow
import org.apache.commons.text.StringEscapeUtils
import splitties.init.appCtx
import kotlin.coroutines.coroutineContext

/**
 * 获取正文
 */
object BookContent {

    @Throws(Exception::class)
    suspend fun analyzeContent(
        bookSource: BookSource,
        book: Book,
        bookChapter: BookChapter,
        baseUrl: String,
        redirectUrl: String,
        body: String?,
        nextChapterUrl: String?,
        needSave: Boolean = true
    ): String {
        body ?: throw NoStackTraceException(
            appCtx.getString(R.string.error_get_web_content, baseUrl)
        )
        Debug.log(bookSource.bookSourceUrl, "≡获取成功:${baseUrl}")
        Debug.log(bookSource.bookSourceUrl, body, state = 40)
        val mNextChapterUrl = if (nextChapterUrl.isNullOrEmpty()) {
            appDb.bookChapterDao.getChapter(book.bookUrl, bookChapter.index + 1)?.url
                ?: appDb.bookChapterDao.getChapter(book.bookUrl, 0)?.url
        } else {
            nextChapterUrl
        }
        val contentList = arrayListOf<String>()
        val nextUrlList = arrayListOf(redirectUrl)
        val contentRule = bookSource.getContentRule()
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.setCoroutineContext(coroutineContext)
        analyzeRule.chapter = bookChapter
        analyzeRule.nextChapterUrl = mNextChapterUrl
        coroutineContext.ensureActive()
        val titleRule = contentRule.title
        if (!titleRule.isNullOrBlank()) {
            val title = analyzeRule.runCatching {
                getString(titleRule)
            }.onFailure {
                Debug.log(bookSource.bookSourceUrl, "获取标题出错, ${it.localizedMessage}")
            }.getOrNull()
            if (!title.isNullOrBlank()) {
                bookChapter.title = title
                bookChapter.titleMD5 = null
                appDb.bookChapterDao.update(bookChapter)
            }
        }
        var contentData = analyzeContent(
            book, baseUrl, redirectUrl, body, contentRule, bookChapter, bookSource, mNextChapterUrl
        )
        contentList.add(contentData.first)
        if (contentData.second.size == 1) {
            var nextUrl = contentData.second[0]
            while (nextUrl.isNotEmpty() && !nextUrlList.contains(nextUrl)) {
                if (!mNextChapterUrl.isNullOrEmpty()
                    && NetworkUtils.getAbsoluteURL(redirectUrl, nextUrl)
                    == NetworkUtils.getAbsoluteURL(redirectUrl, mNextChapterUrl)
                ) break
                nextUrlList.add(nextUrl)
                coroutineContext.ensureActive()
                val res = AnalyzeUrl(
                    mUrl = nextUrl,
                    source = bookSource,
                    ruleData = book,
                    headerMapF = bookSource.getHeaderMap()
                ).getStrResponseAwait() //控制并发访问
                res.body?.let { nextBody ->
                    contentData = analyzeContent(
                        book, nextUrl, res.url, nextBody, contentRule,
                        bookChapter, bookSource, mNextChapterUrl,
                        printLog = false
                    )
                    nextUrl =
                        if (contentData.second.isNotEmpty()) contentData.second[0] else ""
                    contentList.add(contentData.first)
                    Debug.log(bookSource.bookSourceUrl, "第${contentList.size}页完成")
                }
            }
            Debug.log(bookSource.bookSourceUrl, "◇本章总页数:${nextUrlList.size}")
        } else if (contentData.second.size > 1) {
            Debug.log(bookSource.bookSourceUrl, "◇并发解析正文,总页数:${contentData.second.size}")
            flow {
                for (urlStr in contentData.second) {
                    emit(urlStr)
                }
            }.mapAsync(AppConfig.threadCount) { urlStr ->
                val res = AnalyzeUrl(
                    mUrl = urlStr,
                    source = bookSource,
                    ruleData = book,
                    headerMapF = bookSource.getHeaderMap()
                ).getStrResponseAwait() //控制并发访问
                analyzeContent(
                    book, urlStr, res.url, res.body!!, contentRule,
                    bookChapter, bookSource, mNextChapterUrl,
                    getNextPageUrl = false,
                    printLog = false
                ).first
            }.collect {
                coroutineContext.ensureActive()
                contentList.add(it)
            }
        }
        var contentStr = contentList.joinToString("\n")
        //全文替换
        val replaceRegex = contentRule.replaceRegex
        if (!replaceRegex.isNullOrEmpty()) {
            contentStr = contentStr.split(AppPattern.LFRegex).joinToString("\n") { it.trim() }
            contentStr = analyzeRule.getString(replaceRegex, contentStr)
            contentStr = contentStr.split(AppPattern.LFRegex).joinToString("\n") { "　　$it" }
        }
        Debug.log(bookSource.bookSourceUrl, "┌获取章节名称")
        Debug.log(bookSource.bookSourceUrl, "└${bookChapter.title}")
        Debug.log(bookSource.bookSourceUrl, "┌获取正文内容")
        Debug.log(bookSource.bookSourceUrl, "└\n$contentStr")
        if (!bookChapter.isVolume && contentStr.isBlank()) {
            throw ContentEmptyException("内容为空")
        }
        if (needSave) {
            BookHelp.saveContent(bookSource, book, bookChapter, contentStr)
        }
        return contentStr
    }

    @Throws(Exception::class)
    private suspend fun analyzeContent(
        book: Book,
        baseUrl: String,
        redirectUrl: String,
        body: String,
        contentRule: ContentRule,
        chapter: BookChapter,
        bookSource: BookSource,
        nextChapterUrl: String?,
        getNextPageUrl: Boolean = true,
        printLog: Boolean = true
    ): Pair<String, List<String>> {
        val analyzeRule = AnalyzeRule(book, bookSource)
        analyzeRule.setContent(body, baseUrl)
        analyzeRule.setCoroutineContext(coroutineContext)
        val rUrl = analyzeRule.setRedirectUrl(redirectUrl)
        analyzeRule.nextChapterUrl = nextChapterUrl
        val nextUrlList = arrayListOf<String>()
        analyzeRule.chapter = chapter
        //获取正文
        var content = analyzeRule.getString(contentRule.content, unescape = false)
        content = HtmlFormatter.formatKeepImg(content, rUrl)
        if (content.indexOf('&') > -1) {
            content = StringEscapeUtils.unescapeHtml4(content)
        }
        //获取下一页链接
        if (getNextPageUrl) {
            val nextUrlRule = contentRule.nextContentUrl
            if (!nextUrlRule.isNullOrEmpty()) {
                Debug.log(bookSource.bookSourceUrl, "┌获取正文下一页链接", printLog)
                analyzeRule.getStringList(nextUrlRule, isUrl = true)?.let {
                    nextUrlList.addAll(it)
                }
                Debug.log(bookSource.bookSourceUrl, "└" + nextUrlList.joinToString("，"), printLog)
            }
        }
        return Pair(content, nextUrlList)
    }
}
