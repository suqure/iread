package ltd.finelink.read.model

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.tts.LocalTTSHelp
import ltd.finelink.read.service.BaseReadAloudService
import ltd.finelink.read.service.LocalTTSReadAloudService
import ltd.finelink.read.service.TTSReadAloudService
import ltd.finelink.read.utils.LogUtils
import ltd.finelink.read.utils.StringUtils
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx

object ReadAloud {
    private var aloudClass: Class<*> = getReadAloudClass()
    val ttsEngine get() = ReadBook.book?.getTtsEngine() ?: AppConfig.ttsEngine
    var localTTS: LocalTTS? = null

    private fun getReadAloudClass(): Class<*> {
        val ttsEngine = ttsEngine
        if (ttsEngine.isNullOrBlank()) {
            return TTSReadAloudService::class.java
        }
        if (StringUtils.isNumeric(ttsEngine)) {
            localTTS = appDb.localTTSDao.get(ttsEngine.toLong())
            if(localTTS!=null){
                LocalTTSHelp.refreshModel(localTTS!!)
                return LocalTTSReadAloudService::class.java
            }
        }
        return TTSReadAloudService::class.java
    }

    fun upReadAloudClass() {
        stop(appCtx)
        aloudClass = getReadAloudClass()
    }

    fun play(
        context: Context,
        play: Boolean = true,
        pageIndex: Int = ReadBook.durPageIndex,
        startPos: Int = 0
    ) {
        val intent = Intent(context, aloudClass)
        intent.action = IntentAction.play
        intent.putExtra("play", play)
        intent.putExtra("pageIndex", pageIndex)
        intent.putExtra("startPos", startPos)
        LogUtils.d("ReadAloud", intent.toString())
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: Exception) {
            val msg = "启动朗读服务出错\n${e.localizedMessage}"
            AppLog.put(msg, e)
            context.toastOnUi(msg)
        }
    }

    fun playByEventBus(
        play: Boolean = true,
        pageIndex: Int = ReadBook.durPageIndex,
        startPos: Int = 0
    ) {
        val bundle = Bundle().apply {
            putBoolean("play", play)
            putInt("pageIndex", pageIndex)
            putInt("startPos", startPos)
        }
        postEvent(EventBus.READ_ALOUD_PLAY, bundle)
    }

    fun pause(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.pause
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun resume(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.resume
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun stop(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.stop
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun prevParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.prevParagraph
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun nextParagraph(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.nextParagraph
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun upTtsSpeechRate(context: Context) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.upTtsSpeechRate
            ContextCompat.startForegroundService(context, intent)
        }
    }

    fun setTimer(context: Context, minute: Int) {
        if (BaseReadAloudService.isRun) {
            val intent = Intent(context, aloudClass)
            intent.action = IntentAction.setTimer
            intent.putExtra("minute", minute)
            ContextCompat.startForegroundService(context, intent)
        }
    }

}