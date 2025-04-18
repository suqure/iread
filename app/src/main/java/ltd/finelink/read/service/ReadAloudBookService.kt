package ltd.finelink.read.service

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseService
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.constant.NotificationId
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.book.ContentProcessor
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.llm.LLMHelp
import ltd.finelink.read.help.llm.LLMModel
import ltd.finelink.read.help.llm.MessageData
import ltd.finelink.read.help.llm.MessageRole
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.ui.book.read.page.provider.ChapterProvider
import ltd.finelink.read.ui.main.MainActivity
import ltd.finelink.read.ui.store.book.ReadAloudBookActivity
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.TextUtils
import ltd.finelink.read.utils.activityPendingIntent
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.servicePendingIntent
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import splitties.systemservices.notificationManager
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

/**
 * 分析书籍服务
 */
class ReadAloudBookService : BaseService() {

    companion object {
        val analyseProgress = ConcurrentHashMap<String, Int>()
        val analyseMsg = ConcurrentHashMap<String, String>()
    }

    data class AnalyseConfig(
        val startChapter: Int = -1,
        val endChapter: Int = -1
    )

    private val groupKey = "${appCtx.packageName}.readAloudBook"
    private val waitAnalyseBooks = linkedMapOf<String, AnalyseConfig>()
    private var analyseJob: Job? = null
    private var notificationContentText = appCtx.getString(R.string.service_starting)
    private val spPattern = Pattern.compile("^(\\[[a-zA-Z_\\d\\u4e00-\\u9fa5]{1,10}\\])")
    private val speakers:MutableList<String> = mutableListOf()
    private var generateTask: Coroutine<*>? = null
    private val generateTaskActiveLock = Mutex()
    private val model: LLMModel by lazy {
        LLMHelp.loadModel(appDb.llmConfigDao.get(-1)!!)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            IntentAction.start -> kotlin.runCatching {
                val bookUrl = intent.getStringExtra("bookUrl")!!
                if (!analyseProgress.contains(bookUrl)) {
                    val exportConfig = AnalyseConfig(
                        startChapter = intent.getIntExtra("start", -1),
                        endChapter = intent.getIntExtra("end", -1),
                    )
                    waitAnalyseBooks[bookUrl] = exportConfig
                    analyseMsg[bookUrl] = getString(R.string.analyse_wait)
                    analyse()
                }
            }.onFailure {
                toastOnUi(it.localizedMessage)
            }

            IntentAction.stop -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        analyseProgress.clear()
        analyseMsg.clear()
        model.unload()
    }

    @SuppressLint("MissingPermission")
    override fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_chapter_list)
            .setSubText(getString(R.string.read_aloud_analyse))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(groupKey)
            .setGroupSummary(true)
        startForeground(NotificationId.ReadAloudBookService, notification.build())
    }

    private fun upExportNotification(finish: Boolean = false,bookUrl:String?=null) {
        val notification = NotificationCompat.Builder(this, AppConst.channelIdDownload)
            .setSmallIcon(R.drawable.ic_chapter_list)
            .setSubText(getString(R.string.read_aloud_analyse))
            .setContentIntent(activityPendingIntent<MainActivity>("mainActivity"))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentText(notificationContentText)
            .setDeleteIntent(servicePendingIntent<ReadAloudBookService>(IntentAction.stop))
            .setGroup(groupKey)
        bookUrl?.let {
            notification.setContentIntent(activityPendingIntent<ReadAloudBookActivity>("readAloudBookActivity"){
                putExtra("bookUrl", bookUrl)
            })
        }
        if (!finish) {
            notification.setOngoing(true)
            notification.addAction(
                R.drawable.ic_stop_black_24dp,
                getString(R.string.cancel),
                servicePendingIntent<ReadAloudBookService>(IntentAction.stop)
            )
        }
        notificationManager.notify(NotificationId.ReadAloudBookService, notification.build())
    }

    private fun analyse() {
        if (analyseJob?.isActive == true) {
            return
        }
        analyseJob = lifecycleScope.launch(IO) {
            model.load()
            generateTask?.cancel()
            generateTask = execute {
                generateTaskActiveLock.withLock {
                    ensureActive()
                    while (true) {
                        val (bookUrl, exportConfig) = waitAnalyseBooks.entries.firstOrNull() ?: let {
                            notificationContentText = "分析完成"
                            upExportNotification(true)
                            stopSelf()
                            return@execute
                        }
                        analyseProgress[bookUrl] = 0
                        waitAnalyseBooks.remove(bookUrl)
                        val book = appDb.bookDao.getBook(bookUrl)
                        try {
                            book ?: throw NoStackTraceException("获取${bookUrl}书籍出错")
                            refreshChapterList(book)
                            notificationContentText = getString(
                                R.string.analyse_book_notification_content,
                                book.name,
                                waitAnalyseBooks.size
                            )
                            upExportNotification(bookUrl=bookUrl)
                            analyse(book,exportConfig)
                            analyseMsg[book.bookUrl] = getString(R.string.analyse_success)
                            postEvent(EventBus.FINISH_BOOK_ANALYSE,book)
                        } catch (e: Throwable) {
                            analyseMsg[bookUrl] = e.localizedMessage ?: "ERROR"
                            AppLog.put("分析书籍<${book?.name ?: bookUrl}>出错", e)
                        } finally {
                            analyseProgress.remove(bookUrl)
                        }
                    }
                }
            }

        }
    }

    private fun refreshChapterList(book: Book) {
        if (!book.isLocal) {
            return
        }
        if (LocalBook.getLastModified(book).getOrDefault(0L) < book.latestChapterTime) {
            return
        }
        kotlin.runCatching {
            LocalBook.getChapterList(book)
        }.onSuccess {
            book.latestChapterTime = System.currentTimeMillis()
            appDb.bookChapterDao.delByBook(book.bookUrl)
            appDb.bookChapterDao.insert(*it.toTypedArray())
        }
    }





    private suspend fun analyse(book: Book,config:AnalyseConfig){
        speakers.clear()
        appDb.readAloudBookDao.get(book.bookUrl)?.let {
            var start = it.durChapterIndex
            var end =  it.totalChapterNum
            if(config.startChapter!=-1){
                start = config.startChapter
            }
            if(config.endChapter!=-1){
                end = config.endChapter
            }
            for( i in start until end){
                appDb.bookChapterDao.getChapter(book.bookUrl,i)?.let { chapter->
                    it.durChapterIndex = i
                    appDb.readAloudBookDao.update(it)
                    analyseChapter(book,chapter,it)
                }
            }
        }
    }

    private suspend fun analyseChapter(book:Book,chapter: BookChapter,readAloudBook:ReadAloudBook){
        BookHelp.getContent(book, chapter)?.let {
            postEvent(EventBus.UP_BOOK_ANALYSE,chapter)
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            val contents = contentProcessor
                .getContent(book, chapter, it, includeTitle = false)
            var texts = handleChapterText(book,contents.textList,chapter)
            var pos=0
            var system = MessageData(MessageRole.System,"你是一个专业的对话处理助手，请将文本中出现的对话内容和说话人顺序提取出来")

            for(text in texts){
                var result = model.generate(arrayListOf(system,MessageData(MessageRole.User,text)))
                Log.d("Analyse","result:$result")
                pos = handleAnalyseText(book,chapter,result,contents.textList,pos,readAloudBook)
            }

        }
    }

    private fun saveDialogue(book:Book,chapter: BookChapter,text: String,index:Int){
        val dialogues = TextUtils.findDialogue(text)
        for(dialogue in dialogues) {
            val t = dialogue.substring(1,dialogue.length-1)
            var detailId = MD5Utils.md5Encode16( book.bookUrl)+"_" +MD5Utils.md5Encode16("${chapter.index}-|-$index-|-$t")
            appDb.bookSpeakerDetailDao.get(book.bookUrl,detailId)?.let {
                return
            }
            var detail =  BookSpeakerDetail(System.currentTimeMillis(), book.bookUrl,"[对话]",t,detailId,chapter.index,index)
            appDb.bookSpeakerDetailDao.insert(detail)
        }
    }

    private fun handleAnalyseText(book:Book,chapter: BookChapter,message:String,sources:List<String>,pos:Int,readAloudBook:ReadAloudBook):Int{
        var contents =message.split("\n").filter { it.trim().isNotEmpty() }
        var start = pos
        for(content in contents){
            val text = content.trim()
            val matcher = spPattern.matcher(text)
            if (matcher.find()) {
                val sp = matcher.group(1)
                sp?.let {
                    if(it.length<text.length){
                        var t = text.substring(it.length).trim()
                        getContentIndex(t,sources,start)?.let { index->
                            var detailId = MD5Utils.md5Encode16( book.bookUrl)+"_" +MD5Utils.md5Encode16("${chapter.index}-|-$index-|-$t")
                            var detail =  BookSpeakerDetail(System.currentTimeMillis(), book.bookUrl,sp,t,detailId,chapter.index,index)
                            appDb.bookSpeakerDetailDao.insert(detail)
                            start=index
                            if(!speakers.contains(sp)){
                                var speaker = BookSpeaker(System.currentTimeMillis(),book.bookUrl,readAloudBook.modelId,0,sp)
                                appDb.bookSpeakerDao.insert(speaker)
                                speakers.add(sp)
                                postEvent(EventBus.UP_BOOK_SPEAKER,speaker)
                            }

                        }

                    }
                }
            }
        }
        return start

    }

    private fun getContentIndex(text:String,contents:List<String>,start:Int):Int?{
        if (start<contents.size){
            for(i in  start until contents.size){
                if (contents[i].contains(text)){
                    return i+1
                }
            }
        }
        return null;
    }


    private fun handleChapterText(book:Book,contents:List<String>,chapter:BookChapter,maxLen:Int=128):List<String>{
        val result:MutableList<String> = mutableListOf()
        val stringBuilder = StringBuilder()
        val sb = StringBuffer()
        contents.forEachIndexed { index, content ->
            var dialogue = false
            if (book.getImageStyle().equals(Book.imgStyleText, true)) {
                var text = content.replace(ChapterProvider.srcReplaceChar, "▣")
                val srcList = LinkedList<String>()
                sb.setLength(0)
                val matcher = AppPattern.imgPattern.matcher(text)
                while (matcher.find()) {
                    matcher.group(1)?.let { src ->
                        srcList.add(src)
                        matcher.appendReplacement(sb, ChapterProvider.srcReplaceChar)
                    }
                }
                matcher.appendTail(sb)
                text = sb.toString()
                if(text.trim().matches("(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)".toRegex())){
                    dialogue = true
                }
                saveDialogue(book,chapter,text,index+1)
                stringBuilder.append(text)
            } else {
                saveDialogue(book,chapter,content,index+1)
                if(content.trim().matches("(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)".toRegex())){
                    dialogue = true
                }
                val matcher = AppPattern.imgPattern.matcher(content)
                var start = 0
                while (matcher.find()) {
                    val text = content.substring(start, matcher.start())
                    if (text.isNotBlank()) {
                        stringBuilder.append(text)
                    }
                    start = matcher.end()
                }
                if (start < content.length) {
                    val text = content.substring(start, content.length)
                    if (text.isNotBlank()) {
                        stringBuilder.append(text)
                    }
                }
            }
            if(stringBuilder.length>=maxLen&&!dialogue){
                result.add(stringBuilder.toString())
                stringBuilder.setLength(0)
            }
            if(stringBuilder.isNotEmpty()&&!dialogue){
                stringBuilder.append("\n")
            }
        }
        if(stringBuilder.isNotEmpty()){
            result.add(stringBuilder.toString())
        }
        return result;
    }



}