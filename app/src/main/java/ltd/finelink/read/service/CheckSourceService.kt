package ltd.finelink.read.service

import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.script.ScriptException
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseService
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.BookSourceType
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.constant.NotificationId
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.exception.ContentEmptyException
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.exception.TocEmptyException
import ltd.finelink.read.help.IntentData
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.source.exploreKinds
import ltd.finelink.read.model.CheckSource
import ltd.finelink.read.model.Debug
import ltd.finelink.read.model.webBook.WebBook
import ltd.finelink.read.ui.book.source.manage.BookSourceActivity
import ltd.finelink.read.utils.activityPendingIntent
import ltd.finelink.read.utils.onEachParallel
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.servicePendingIntent
import ltd.finelink.read.utils.toastOnUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.mozilla.javascript.WrappedException
import splitties.init.appCtx
import splitties.systemservices.notificationManager
import java.util.concurrent.Executors
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * 校验书源
 */
class CheckSourceService : BaseService() {
    private var threadCount = AppConfig.threadCount
    private var searchCoroutine =
        Executors.newFixedThreadPool(min(threadCount, AppConst.MAX_THREAD)).asCoroutineDispatcher()
    private var notificationMsg = appCtx.getString(R.string.service_starting)
    private var checkJob: Job? = null
    private var originSize = 0
    private var finishCount = 0

    private val notificationBuilder by lazy {
        NotificationCompat.Builder(this, AppConst.channelIdReadAloud)
            .setSmallIcon(R.drawable.ic_network_check)
            .setOngoing(true)
            .setContentTitle(getString(R.string.check_book_source))
            .setContentIntent(
                activityPendingIntent<BookSourceActivity>("activity")
            )
            .addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                servicePendingIntent<CheckSourceService>(IntentAction.stop)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> IntentData.get<List<String>>("checkSourceSelectedIds")?.let {
                check(it)
            }

            IntentAction.resume -> upNotification()
            IntentAction.stop -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Debug.finishChecking()
        searchCoroutine.close()
        postEvent(EventBus.CHECK_SOURCE_DONE, 0)
        notificationManager.cancel(NotificationId.CheckSourceService)
    }

    private fun check(ids: List<String>) {
        if (checkJob?.isActive == true) {
            toastOnUi("已有书源在校验,等完成后再试")
            return
        }
        checkJob = lifecycleScope.launch(searchCoroutine) {
            flow {
                for (origin in ids) {
                    appDb.bookSourceDao.getBookSource(origin)?.let {
                        emit(it)
                    }
                }
            }.onStart {
                originSize = ids.size
                finishCount = 0
                notificationMsg = getString(R.string.progress_show, "", 0, originSize)
                upNotification()
            }.onEachParallel(threadCount) {
                checkSource(it)
            }.onEach {
                finishCount++
                notificationMsg = getString(
                    R.string.progress_show,
                    it.bookSourceName,
                    finishCount,
                    originSize
                )
                upNotification()
                appDb.bookSourceDao.update(it)
            }.onCompletion {
                stopSelf()
            }.collect()
        }
    }

    private suspend fun checkSource(source: BookSource) {
        kotlin.runCatching {
            withTimeout(CheckSource.timeout) {
                doCheckSource(source)
            }
        }.onSuccess {
            Debug.updateFinalMessage(source.bookSourceUrl, "校验成功")
        }.onFailure {
            coroutineContext.ensureActive()
            when (it) {
                is TimeoutCancellationException -> source.addGroup("校验超时")
                is ScriptException, is WrappedException -> source.addGroup("js失效")
                !is NoStackTraceException -> source.addGroup("网站失效")
            }
            source.addErrorComment(it)
            Debug.updateFinalMessage(source.bookSourceUrl, "校验失败:${it.localizedMessage}")
        }
        source.respondTime = Debug.getRespondTime(source.bookSourceUrl)
    }

    private suspend fun doCheckSource(source: BookSource) {
        Debug.startChecking(source)
        source.removeInvalidGroups()
        source.removeErrorComment()
        //校验搜索书籍
        if (CheckSource.checkSearch) {
            val searchWord = source.getCheckKeyword(CheckSource.keyword)
            if (!source.searchUrl.isNullOrBlank()) {
                source.removeGroup("搜索链接规则为空")
                val searchBooks = WebBook.searchBookAwait(source, searchWord)
                if (searchBooks.isEmpty()) {
                    source.addGroup("搜索失效")
                } else {
                    source.removeGroup("搜索失效")
                    checkBook(searchBooks.first().toBook(), source)
                }
            } else {
                source.addGroup("搜索链接规则为空")
            }
        }
        //校验发现书籍
        if (CheckSource.checkDiscovery && !source.exploreUrl.isNullOrBlank()) {
            val url = source.exploreKinds().firstOrNull {
                !it.url.isNullOrBlank()
            }?.url
            if (url.isNullOrBlank()) {
                source.addGroup("发现规则为空")
            } else {
                source.removeGroup("发现规则为空")
                val exploreBooks = WebBook.exploreBookAwait(source, url)
                if (exploreBooks.isEmpty()) {
                    source.addGroup("发现失效")
                } else {
                    source.removeGroup("发现失效")
                    checkBook(exploreBooks.first().toBook(), source, false)
                }
            }
        }
        val finalCheckMessage = source.getInvalidGroupNames()
        if (finalCheckMessage.isNotBlank()) {
            throw NoStackTraceException(finalCheckMessage)
        }
    }

    /**
     *校验书源的详情目录正文
     */
    private suspend fun checkBook(book: Book, source: BookSource, isSearchBook: Boolean = true) {
        kotlin.runCatching {
            if (!CheckSource.checkInfo) {
                return
            }
            //校验详情
            if (book.tocUrl.isBlank()) {
                WebBook.getBookInfoAwait(source, book)
            }
            if (!CheckSource.checkCategory || source.bookSourceType == BookSourceType.file) {
                return
            }
            //校验目录
            val toc = WebBook.getChapterListAwait(source, book).getOrThrow().asSequence()
                .filter { !(it.isVolume && it.url.startsWith(it.title)) }
                .take(2)
                .toList()
            val nextChapterUrl = toc.getOrNull(1)?.url ?: toc.first().url
            if (!CheckSource.checkContent) {
                return
            }
            //校验正文
            WebBook.getContentAwait(
                bookSource = source,
                book = book,
                bookChapter = toc.first(),
                nextChapterUrl = nextChapterUrl,
                needSave = false
            )
        }.onFailure {
            val bookType = if (isSearchBook) "搜索" else "发现"
            when (it) {
                is ContentEmptyException -> source.addGroup("${bookType}正文失效")
                is TocEmptyException -> source.addGroup("${bookType}目录失效")
                else -> throw it
            }
        }.onSuccess {
            val bookType = if (isSearchBook) "搜索" else "发现"
            source.removeGroup("${bookType}目录失效")
            source.removeGroup("${bookType}正文失效")
        }
    }

    private fun upNotification() {
        notificationBuilder.setContentText(notificationMsg)
        notificationBuilder.setProgress(originSize, finishCount, false)
        postEvent(EventBus.CHECK_SOURCE, notificationMsg)
        notificationManager.notify(NotificationId.CheckSourceService, notificationBuilder.build())
    }

    /**
     * 更新通知
     */
    override fun startForegroundNotification() {
        notificationBuilder.setContentText(notificationMsg)
        notificationBuilder.setProgress(originSize, finishCount, false)
        postEvent(EventBus.CHECK_SOURCE, notificationMsg)
        startForeground(NotificationId.CheckSourceService, notificationBuilder.build())
    }

}