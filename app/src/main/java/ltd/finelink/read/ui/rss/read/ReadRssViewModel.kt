package ltd.finelink.read.ui.rss.read

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BaseSource
import ltd.finelink.read.data.entities.RssArticle
import ltd.finelink.read.data.entities.RssReadAloudRule
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.data.entities.RssStar
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.JsExtensions
import ltd.finelink.read.help.TTS
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.help.tts.TTSModel
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.analyzeRule.AnalyzeUrl
import ltd.finelink.read.model.rss.Rss
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.StringUtils
import ltd.finelink.read.utils.TextUtils
import ltd.finelink.read.utils.WavFileUtils
import ltd.finelink.read.utils.normalText
import ltd.finelink.read.utils.textArray
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.writeBytes
import org.jsoup.Jsoup
import splitties.init.appCtx
import java.io.File
import java.util.Date
import kotlin.coroutines.cancellation.CancellationException


class ReadRssViewModel(application: Application) : BaseViewModel(application), JsExtensions,
    Player.Listener {
    var rssSource: RssSource? = null
    var rssArticle: RssArticle? = null
    var tts: TTS? = null
    var model: TTSModel? = null
    var title: String? = null
    var rssReadAloudRule: RssReadAloudRule? = null
    var currentUrl: String? = null
    var current: String? = null
    val contentLiveData = MutableLiveData<String>()
    val urlLiveData = MutableLiveData<AnalyzeUrl>()
    var rssStar: RssStar? = null
    val upTtsMenuData = MutableLiveData<Boolean>()
    val upStarMenuData = MutableLiveData<Boolean>()
    private var generateFileTask: Coroutine<*>? = null
    private val generateTaskActiveLock = Mutex()

    private val ttsFolderPath: String by lazy {
        appCtx.cacheDir.absolutePath + File.separator + "AppTTS" + File.separator
    }

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build()
    }

    override fun getSource(): BaseSource? {
        return rssSource
    }


    fun initData(intent: Intent) {
        exoPlayer.addListener(this)
        execute {
            val origin = intent.getStringExtra("origin") ?: return@execute
            val link = intent.getStringExtra("link")
            rssSource = appDb.rssSourceDao.getByKey(origin)
            if (link != null) {
                rssStar = appDb.rssStarDao.get(origin, link)
                rssArticle = rssStar?.toRssArticle() ?: appDb.rssArticleDao.get(origin, link)
                val rssArticle = rssArticle ?: return@execute
                if (!rssArticle.description.isNullOrBlank()) {
                    contentLiveData.postValue(rssArticle.description!!)
                } else {
                    rssSource?.let {
                        val ruleContent = it.ruleContent
                        if (!ruleContent.isNullOrBlank()) {
                            loadContent(rssArticle, ruleContent)
                        } else {
                            loadUrl(rssArticle.link, rssArticle.origin)
                        }
                    } ?: loadUrl(rssArticle.link, rssArticle.origin)
                }
            } else {
                val ruleContent = rssSource?.ruleContent
                if (ruleContent.isNullOrBlank()) {
                    loadUrl(origin, origin)
                } else {
                    val rssArticle = RssArticle()
                    rssArticle.origin = origin
                    rssArticle.link = origin
                    rssArticle.title = rssSource!!.sourceName
                    loadContent(rssArticle, ruleContent)
                }
            }
            rssSource?.let {
                rssReadAloudRule = appDb.rssReadAloudRuleDao.get(it.sourceUrl)
            }
        }.onFinally {
            upStarMenuData.postValue(true)
        }
    }

    private fun loadUrl(url: String, baseUrl: String) {
        val analyzeUrl = AnalyzeUrl(
            mUrl = url, baseUrl = baseUrl, headerMapF = rssSource?.getHeaderMap()
        )
        urlLiveData.postValue(analyzeUrl)
    }

    private fun initModel(): Boolean {
        model?.let {
            return true
        }
        ReadAloud.ttsEngine?.let {
            if (StringUtils.isNumeric(it)) {
                appDb.localTTSDao.get(it.toLong())?.let { tts ->
                    model = LocalTTSHelp.loadModel(tts)
                    return true
                }
            }
        }
        return false
    }

    private fun loadContent(rssArticle: RssArticle, ruleContent: String) {
        val source = rssSource ?: return
        Rss.getContent(viewModelScope, rssArticle, ruleContent, source).onSuccess(IO) { body ->
            rssArticle.description = body
            appDb.rssArticleDao.insert(rssArticle)
            rssStar?.let {
                it.description = body
                appDb.rssStarDao.insert(it)
            }
            contentLiveData.postValue(body)
        }.onError {
            contentLiveData.postValue("加载正文失败\n${it.stackTraceToString()}")
        }
    }

    fun refresh(finish: () -> Unit) {
        rssArticle?.let { rssArticle ->
            rssSource?.let {
                val ruleContent = it.ruleContent
                if (!ruleContent.isNullOrBlank()) {
                    loadContent(rssArticle, ruleContent)
                } else {
                    finish.invoke()
                }
            } ?: let {
                appCtx.toastOnUi("订阅源不存在")
                finish.invoke()
            }
        } ?: finish.invoke()
    }

    fun favorite() {
        execute {
            rssStar?.let {
                appDb.rssStarDao.delete(it.origin, it.link)
                rssStar = null
            } ?: rssArticle?.toStar()?.let {
                appDb.rssStarDao.insert(it)
                rssStar = it
            }
        }.onSuccess {
            upStarMenuData.postValue(true)
        }
    }

    fun saveImage(webPic: String?, uri: Uri) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            val byteArray = webData2bitmap(webPic) ?: throw NoStackTraceException("NULL")
            uri.writeBytes(context, fileName, byteArray)
        }.onError {
            context.toastOnUi("保存图片失败:${it.localizedMessage}")
        }.onSuccess {
            context.toastOnUi("保存成功")
        }
    }

    private suspend fun webData2bitmap(data: String): ByteArray? {
        return if (URLUtil.isValidUrl(data)) {
            okHttpClient.newCallResponseBody {
                url(data)
            }.bytes()
        } else {
            Base64.decode(data.split(",").toTypedArray()[1], Base64.DEFAULT)
        }
    }

    fun clHtml(content: String): String {
        return when {
            !rssSource?.style.isNullOrEmpty() -> {
                """
                    <style>
                        ${rssSource?.style}
                    </style>
                    $content
                """.trimIndent()
            }

            content.contains("<style>".toRegex()) -> {
                content
            }

            else -> {
                """
                    <style>
                        img{max-width:100% !important; width:auto; height:auto;}
                        video{object-fit:fill; max-width:100% !important; width:auto; height:auto;}
                        body{word-wrap:break-word; height:auto;max-width: 100%; width:auto;}
                    </style>
                    $content
                """.trimIndent()
            }
        }
    }

    @Synchronized
    fun readAloud(text: String) {
        if (initModel()) {
            generateAndPlayAudios(text.split("\n"))
            return
        }
        if (tts == null) {
            tts = TTS().apply {
                setSpeakStateListener(object : TTS.SpeakStateListener {
                    override fun onStart() {
                        upTtsMenuData.postValue(true)
                    }

                    override fun onDone() {
                        upTtsMenuData.postValue(false)
                    }
                })
            }
        }
        tts?.speak(text)
    }

    fun stopPlay() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        generateFileTask?.cancel()

    }

    fun readAloudRuleFile(html: String): String {

        rssReadAloudRule?.let { rule ->
            if (rule.acceptClass.isNotEmpty() || rule.acceptIds.isNotEmpty() ||
                rule.acceptTags.isNotEmpty() || rule.ignoreClass.isNotEmpty() ||
                rule.ignoreIds.isNotEmpty() || rule.ignoreTags.isNotEmpty()
            ) {
                var filters: MutableList<String> = mutableListOf()
                var doc = Jsoup.parse(html)
                for (e in doc.body().allElements) {
                    if (e.text().isEmpty()) {
                        continue
                    }
                    if (rule.ignoreIds.isNotEmpty()&&e.id().isNotEmpty()) {
                        if (rule.ignoreIds.split(",").contains(e.id())) {
                            continue
                        }
                    }
                    if (rule.ignoreClass.isNotEmpty()&&e.className().isNotEmpty()) {
                        if (rule.ignoreClass.split(",").contains(e.className())) {
                            continue
                        }
                    }
                    if(rule.ignoreTags.isNotEmpty()){
                        if (rule.ignoreTags.split(",").contains(e.tag().normalName())) {
                            continue
                        }
                    }
                    if(rule.acceptTags.isEmpty()&&rule.acceptClass.isEmpty()&&rule.acceptIds.isEmpty()){
                        filters.add(e.text())
                    }else{
                        if (rule.acceptTags.isNotEmpty()) {
                            if (rule.acceptTags.split(",").contains(e.tagName())) {
                                filters.add(e.normalText())
                                continue
                            }
                        }
                        if (rule.acceptClass.isNotEmpty()) {
                            if (rule.acceptClass.split(",").contains(e.className())) {
                                filters.add(e.normalText())
                                continue
                            }
                        }
                        if (rule.acceptIds.isNotEmpty()) {
                            if (rule.acceptIds.split(",").contains(e.id())) {
                                filters.add(e.normalText())
                                continue
                            }
                        }
                    }
                }
                return filters.toTypedArray().joinToString("\n")
            }
        }
        return Jsoup.parse(html).textArray().joinToString("\n")
    }


    private fun generateAndPlayAudios(contentList: List<String>) {
        exoPlayer.clearMediaItems()
        generateFileTask?.cancel()
        generateFileTask = execute {
            upTtsMenuData.postValue(true)
            generateTaskActiveLock.withLock {
                ensureActive()
                contentList.forEachIndexed { index, content ->
                    ensureActive()
                    var text = content
                    var texts = TextUtils.spiltDialogue(text)
                    texts.forEachIndexed { sIndex, t ->
                        ensureActive()
                        val fileName = md5SpeakFileName(t)
                        val speakText = t.replace(AppPattern.notReadAloudRegex, "")
                        if (speakText.isEmpty()) {
                            AppLog.put("阅读段落内容为空，使用无声音频代替。\n朗读文本：$t")
                            createSilentSound(fileName)
                        } else if (!hasSpeakFile(fileName)) {
                            runCatching {
                                generateFile(
                                    speakText,
                                    "${ttsFolderPath}$fileName.wav",
                                    currentUrl ?: rssSource!!.sourceUrl,
                                    rssSource!!.sourceName,
                                    title ?: "",
                                    0,
                                    0,
                                    index,
                                    sIndex
                                )
                            }.onFailure {
                                when (it) {
                                    is CancellationException -> Unit
                                    else -> {
                                        upTtsMenuData.postValue(false)
                                    }
                                }
                                Log.e("WebViewTTS", "生成文件出错$speakText", it);
                                return@execute
                            }
                        }
                        val file = getSpeakFileAsMd5(fileName)
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                        launch(Dispatchers.Main) {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                            }
                            exoPlayer.addMediaItem(mediaItem)
                            if (!exoPlayer.isPlaying) {
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            }
                        }
                    }
                }
                upTtsMenuData.postValue(false)
            }
        }.onError {
            upTtsMenuData.postValue(false)
            AppLog.put("朗读生成出错\n${it.localizedMessage}", it, true)
        }
    }

    private fun createSilentSound(fileName: String) {
        val file = createSpeakFile(fileName)
        file.writeBytes(appCtx.resources.openRawResource(R.raw.silent_sound).readBytes())
    }

    private fun createSpeakFile(name: String): File {
        return FileUtils.createFileIfNotExist("${ttsFolderPath}$name.wav")
    }

    private fun md5SpeakFileName(content: String): String {
        return MD5Utils.md5Encode16(
            rssArticle?.title ?: ""
        ) + "_" + MD5Utils.md5Encode16("${ReadAloud.localTTS?.id}-|-$${ReadAloud.localTTS?.speakerId}-|-$content")
    }

    private fun getSpeakFileAsMd5(name: String): File {
        return File("${ttsFolderPath}$name.wav")
    }

    private fun hasSpeakFile(name: String): Boolean {
        return FileUtils.exist("${ttsFolderPath}$name.wav")
    }


    private fun generateFile(
        text: String,
        fileName: String,
        bookUrl: String = "",
        bookName: String = "",
        chapterTitle: String = "",
        chapterIndex: Int = 0,
        pageIndex: Int = 0,
        position: Int = 0,
        subIndex: Int = 0,
        speakerId: Long = 0,
    ) {
        model!!.init()
        var content = text
        if (content.trim().matches("(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)".toRegex())) {
            content = content.substring(1, text.length - 1)
        }
        val audio = model!!.tts(content, speakerId)
        WavFileUtils.rawToWave(fileName, audio, model!!.sampleRate())
        var cache = TTSCache(
            System.currentTimeMillis(),
            bookUrl,
            bookName,
            model!!.modelId(),
            model!!.speakerId(),
            content,
            fileName,
            chapterTitle,
            chapterIndex,
            pageIndex,
            position,
            subIndex
        )
        appDb.ttsCacheDao.insert(cache)
        Log.d("RssTTS", "generate wav success:$text")
    }

    override fun onCleared() {
        super.onCleared()
        generateFileTask?.cancel()
        tts?.clearTts()
    }


}