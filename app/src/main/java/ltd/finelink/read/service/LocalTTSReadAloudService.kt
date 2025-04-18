package ltd.finelink.read.service

import android.app.PendingIntent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ltd.finelink.read.R
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.help.tts.TTSModel
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.TextUtils
import ltd.finelink.read.utils.WavFileUtils
import ltd.finelink.read.utils.servicePendingIntent
import ltd.finelink.read.utils.toastOnUi
import java.io.File


/**
 * 在线朗读
 */
class LocalTTSReadAloudService : BaseReadAloudService(),
    Player.Listener {

    private val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(this).build()
    }
    private val ttsFolderPath: String by lazy {
        cacheDir.absolutePath + File.separator + "AppTTS" + File.separator
    }
    private var speechRate: Int = AppConfig.speechRatePlay + 5
    private var generateFileTask: Coroutine<*>? = null
    private var playIndexJob: Job? = null
    private var playErrorNo = 0
    private var subIndex = HashMap<Int, Int>()
    private val generateTaskActiveLock = Mutex()
    private val model: TTSModel by lazy {
        LocalTTSHelp.loadModel(ReadAloud.localTTS!!)
    }
    private var playFlag:Boolean = false

    private val speakerMap = HashMap<String,Long>()

    override fun onCreate() {
        super.onCreate()
        exoPlayer.addListener(this)
    }

    @Synchronized
    private fun initModels() {
        model.init()
        speakerMap.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        generateFileTask?.cancel()
        exoPlayer.release()

    }

    override fun play() {
        playFlag()
        pageChanged = false
        exoPlayer.stop()
        if (!requestFocus()) return
        if (contentList.isEmpty()) {
            AppLog.putDebug("朗读列表为空")
            ReadBook.readAloud()
        } else {
            super.play()
            generateAndPlayAudios()
        }
    }

    override fun playStop() {
        exoPlayer.stop()
        playIndexJob?.cancel()
    }

    override fun playFlag() {
        playFlag = true
        val fileName = "flag_sound"
        if(!hasSpeakFile(fileName)){
            createFlagSound(fileName)
        }
        val file = getSpeakFileAsMd5(fileName)
        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
        }
        exoPlayer.addMediaItem(mediaItem)
        if (!exoPlayer.isPlaying) {
            exoPlayer.playWhenReady = true
            exoPlayer.prepare()
        }
    }

    private fun getSpeaker(text:String,index:Int):Long{
        ReadBook.book?.let {book->
            appDb.readAloudBookDao.get(book.bookUrl)?.let {
                if(it.modelId==model.modelId()){
                    if(text.trim().matches( "(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)".toRegex())){
                        var t = text.trim()
                        t = t.substring(1,t.length-1)
                        if(it.advanceMode){
                            var detailId = MD5Utils.md5Encode16( it.bookUrl)+"_" +MD5Utils.md5Encode16("${textChapter!!.chapter.index}-|-$index-|-$t")
                            speakerMap[detailId]?.let {speakerId->
                                return speakerId
                            }
                            appDb.bookSpeakerDetailDao.get(it.bookUrl,detailId)?.let {detail->
                                appDb.bookSpeakerDao.get(it.bookUrl,detail.spkName)?.let {speaker->
                                    speakerMap[detailId]= speaker.speakerId
                                    return speaker.speakerId
                                }
                            }
                            return it.dialogueId
                        }else{
                            return it.dialogueId
                        }
                    }else{
                        return it.speakerId
                    }

                }
            }
        }
        return 0L
    }


    private fun updateNextPos() {
        readAloudNumber += contentList[nowSpeak].length + 1 - paragraphStartPos
        paragraphStartPos = 0
        if (nowSpeak < contentList.lastIndex) {
            nowSpeak++
        } else {
            nextChapter()
        }
    }

    private fun generateAndPlayAudios() {
        generateFileTask?.cancel()
        generateFileTask = execute {
            generateTaskActiveLock.withLock {
                ensureActive()
                contentList.forEachIndexed { index, content ->
                    ensureActive()
                    if (index < nowSpeak) return@forEachIndexed
                    var text = content
                    if (paragraphStartPos > 0 && index == nowSpeak) {
                        text = text.substring(paragraphStartPos)
                    }
                    var texts = TextUtils.spiltDialogue(text)
                    subIndex[index] = texts.size
                    texts.forEachIndexed { sIndex,t ->
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
                                    ReadBook.book!!.bookUrl,
                                    ReadBook.book!!.name,
                                    textChapter!!.title,
                                    textChapter!!.chapter.index,
                                    ReadBook.durPageIndex,
                                    index,
                                    sIndex,
                                    getSpeaker(speakText,index)
                                )
                            }.onFailure {
                                when (it) {
                                    is CancellationException -> Unit
                                    else -> pauseReadAloud()
                                }
                                Log.e("LocalTTS", "生成文件出错$speakText", it);
                                return@execute
                            }
                        }
                        val file = getSpeakFileAsMd5(fileName)
                        val mediaItem = MediaItem.fromUri(Uri.fromFile(file))
                        launch(Main) {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.stop()
                                exoPlayer.clearMediaItems()
                            }
                            exoPlayer.addMediaItem(mediaItem)
                            if (!exoPlayer.isPlaying) {
                                exoPlayer.playWhenReady = !pause
                                exoPlayer.prepare()
                            }
                        }
                    }
                }
            }
        }.onError {
            AppLog.put("朗读生成出错\n${it.localizedMessage}", it, true)
        }
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
        speakerId:Long=0,
    ) {
        initModels()
        var content = text
        if(content.trim().matches( "(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)".toRegex())){
            content = content.substring(1,text.length-1)
        }
        val audio = model.tts(content,speakerId)
        WavFileUtils.rawToWave(fileName, audio, model.sampleRate())
        var cache = TTSCache(
            System.currentTimeMillis(),
            bookUrl,
            bookName,
            model.modelId(),
            model.speakerId(),
            content,
            fileName,
            chapterTitle,
            chapterIndex,
            pageIndex,
            position,
            subIndex
        )
        appDb.ttsCacheDao.insert(cache)
        Log.d("LocalTTS", "generate wav success:$text")
    }

    private fun md5SpeakFileName(content: String): String {
        return MD5Utils.md5Encode16(textChapter?.title ?: "") + "_" +
                MD5Utils.md5Encode16("${ReadAloud.localTTS?.id}-|-$${ReadAloud.localTTS?.speakerId}-|-$content")
    }

    private fun createSilentSound(fileName: String) {
        val file = createSpeakFile(fileName)
        file.writeBytes(resources.openRawResource(R.raw.silent_sound).readBytes())
    }

    private fun createFlagSound(fileName: String){
        val file = createSpeakFile(fileName)
        file.writeBytes(resources.openRawResource(R.raw.flag).readBytes())
    }

    private fun hasSpeakFile(name: String): Boolean {
        return FileUtils.exist("${ttsFolderPath}$name.wav")
    }

    private fun getSpeakFileAsMd5(name: String): File {
        return File("${ttsFolderPath}$name.wav")
    }

    private fun createSpeakFile(name: String): File {
        return FileUtils.createFileIfNotExist("${ttsFolderPath}$name.wav")
    }


    override fun pauseReadAloud(abandonFocus: Boolean) {
        super.pauseReadAloud(abandonFocus)
        kotlin.runCatching {
            playIndexJob?.cancel()
            exoPlayer.pause()
        }
    }

    override fun resumeReadAloud() {
        super.resumeReadAloud()
        kotlin.runCatching {
            if (pageChanged) {
                play()
            } else {
                exoPlayer.play()
                upPlayPos()
            }
        }
    }

    private fun upPlayPos() {
        playIndexJob?.cancel()
        val textChapter = textChapter ?: return
        playIndexJob = lifecycleScope.launch {
            upTtsProgress(readAloudNumber + 1)
            if (exoPlayer.duration <= 0) {
                return@launch
            }
            val speakTextLength = contentList[nowSpeak].length
            if (speakTextLength <= 0) {
                return@launch
            }
            val sleep = exoPlayer.duration / speakTextLength
            val start = speakTextLength * exoPlayer.currentPosition / exoPlayer.duration
            for (i in start..contentList[nowSpeak].length) {
                if (readAloudNumber + i > textChapter.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    if (pageIndex < textChapter.pageSize) {
                        ReadBook.moveToNextPage()
                        upTtsProgress(readAloudNumber + i.toInt())
                    }
                }
                delay(sleep)
            }
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        generateFileTask?.cancel()
        speechRate = AppConfig.speechRatePlay + 5
        generateAndPlayAudios()
        var speed = speechRate / 10f
        exoPlayer.playbackParameters = PlaybackParameters(speed, speed)
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_IDLE -> {
                // 空闲
            }

            Player.STATE_BUFFERING -> {
                // 缓冲中
            }

            Player.STATE_READY -> {
                // 准备好
                if (pause) return
                exoPlayer.play()
                upPlayPos()
            }

            Player.STATE_ENDED -> {
                // 结束
                playErrorNo = 0
                subIndex[nowSpeak] = subIndex[nowSpeak]!! - 1
                if (subIndex[nowSpeak]!! <= 0) {
                    updateNextPos()
                }
            }
        }
    }

    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) return
        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
            playErrorNo = 0
        }
        if(playFlag){
            playFlag = false
            return
        }
        subIndex[nowSpeak] = subIndex[nowSpeak]!! - 1
        if (subIndex[nowSpeak]!! <= 0) {
            updateNextPos()
            upPlayPos()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        AppLog.put("朗读错误\n${contentList[nowSpeak]}", error)
        playErrorNo++
        if (playErrorNo >= 5) {
            toastOnUi("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})")
            AppLog.put("朗读连续5次错误, 最后一次错误代码(${error.localizedMessage})", error)
            pauseReadAloud()
        } else {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
                exoPlayer.playWhenReady = true
                exoPlayer.prepare()
            } else {
                exoPlayer.clearMediaItems()
                updateNextPos()
            }
        }
    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<LocalTTSReadAloudService>(actionStr)
    }

}