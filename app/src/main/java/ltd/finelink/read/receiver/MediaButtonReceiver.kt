package ltd.finelink.read.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.help.LifecycleHelp
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.model.AudioPlay
import ltd.finelink.read.model.ReadAloud
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.service.AudioPlayService
import ltd.finelink.read.service.BaseReadAloudService
import ltd.finelink.read.ui.book.audio.AudioPlayActivity
import ltd.finelink.read.ui.book.read.ReadBookActivity
import ltd.finelink.read.utils.getPrefBoolean
import ltd.finelink.read.utils.postEvent


/**
 * Created by GKF on 2018/1/6.
 * 监听耳机键
 */
class MediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (handleIntent(context, intent) && isOrderedBroadcast) {
            abortBroadcast()
        }
    }

    companion object {

        fun handleIntent(context: Context, intent: Intent): Boolean {
            val intentAction = intent.action
            if (Intent.ACTION_MEDIA_BUTTON == intentAction) {
                @Suppress("DEPRECATION")
                val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    ?: return false
                val keycode: Int = keyEvent.keyCode
                val action: Int = keyEvent.action
                if (action == KeyEvent.ACTION_DOWN) {
                    when (keycode) {
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                ReadBook.moveToPrevChapter(true)
                            } else {
                                ReadAloud.prevParagraph(context)
                            }
                        }
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            if (context.getPrefBoolean("mediaButtonPerNext", false)) {
                                ReadBook.moveToNextChapter(true)
                            } else {
                                ReadAloud.nextParagraph(context)
                            }
                        }
                        else -> readAloud(context)
                    }
                }
            }
            return true
        }

        fun readAloud(context: Context, isMediaKey: Boolean = true) {
            when {
                BaseReadAloudService.isRun -> {
                    if (BaseReadAloudService.isPlay()) {
                        ReadAloud.pause(context)
                        AudioPlay.pause(context)
                    } else {
                        ReadAloud.resume(context)
                        AudioPlay.resume(context)
                    }
                }
                AudioPlayService.isRun -> {
                    if (AudioPlayService.pause) {
                        AudioPlay.resume(context)
                    } else {
                        AudioPlay.pause(context)
                    }
                }
                LifecycleHelp.isExistActivity(ReadBookActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)
                LifecycleHelp.isExistActivity(AudioPlayActivity::class.java) ->
                    postEvent(EventBus.MEDIA_BUTTON, true)
                else -> if (AppConfig.mediaButtonOnExit || LifecycleHelp.activitySize() > 0 || !isMediaKey) {
                    ReadAloud.upReadAloudClass()
                    if (ReadBook.book != null) {
                        ReadBook.readAloud()
                    } else {
                        appDb.bookDao.lastReadBook?.let {
                            ReadBook.resetData(it)
                            ReadBook.clearTextChapter()
                            ReadBook.loadContent(false) {
                                ReadBook.readAloud()
                            }
                        }
                    }
                }
            }
        }
    }

}
