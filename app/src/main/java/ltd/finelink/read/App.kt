package ltd.finelink.read

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import com.github.liuyueyi.quick.transfer.constants.TransType
import com.jeremyliao.liveeventbus.LiveEventBus
import com.jeremyliao.liveeventbus.logger.DefaultLogger
import kotlinx.coroutines.launch
import ltd.finelink.read.base.AppContextWrapper
import ltd.finelink.read.constant.AppConst.channelIdDownload
import ltd.finelink.read.constant.AppConst.channelIdReadAloud
import ltd.finelink.read.constant.AppConst.channelIdWeb
import ltd.finelink.read.constant.PreferKey
import ltd.finelink.read.data.appDb
import ltd.finelink.read.help.CrashHandler
import ltd.finelink.read.help.DefaultData
import ltd.finelink.read.help.LifecycleHelp
import ltd.finelink.read.help.RuleBigDataHelp
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.config.ThemeConfig.applyDayNight
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.help.http.Cronet
import ltd.finelink.read.help.http.ObsoleteUrlFactory
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.source.SourceHelp
import ltd.finelink.read.help.storage.Backup
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.utils.ChineseUtils
import ltd.finelink.read.utils.LogUtils
import ltd.finelink.read.utils.defaultSharedPreferences
import ltd.finelink.read.utils.getPrefBoolean
import splitties.systemservices.notificationManager
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class App : Application() {

    private lateinit var oldConfig: Configuration

    override fun onCreate() {
        super.onCreate()
        LogUtils.d("App", "onCreate")
        LogUtils.logDeviceInfo()
        oldConfig = Configuration(resources.configuration)
        CrashHandler(this)
        //预下载Cronet so
        Cronet.preDownload()
        createNotificationChannels()
        LiveEventBus.config()
            .lifecycleObserverAlwaysActive(true)
            .autoClear(false)
            .enableLogger(BuildConfig.DEBUG || AppConfig.recordLog)
            .setLogger(EventLogger())
        applyDayNight(this)
        registerActivityLifecycleCallbacks(LifecycleHelp)
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(AppConfig)
        DefaultData.upVersion()
        Coroutine.async {
            URL.setURLStreamHandlerFactory(ObsoleteUrlFactory(okHttpClient))
            //初始化封面
            BookCover.toString()
            //清除过期数据
            appDb.cacheDao.clearDeadline(System.currentTimeMillis())
            if (getPrefBoolean(PreferKey.autoClearExpired, true)) {
                val clearTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
                appDb.searchBookDao.clearExpired(clearTime)
            }
            RuleBigDataHelp.clearInvalid()
            BookHelp.clearInvalidCache()
            Backup.clearCache()
            //初始化简繁转换引擎
            when (AppConfig.chineseConverterType) {
                1 -> launch {
                    ChineseUtils.fixT2sDict()
                }

                2 -> ChineseUtils.preLoad(true, TransType.SIMPLE_TO_TRADITIONAL)
            }
            //调整排序序号
            SourceHelp.adjustSortNumber()

        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val diff = newConfig.diff(oldConfig)
        if ((diff and ActivityInfo.CONFIG_UI_MODE) != 0) {
            applyDayNight(this)
        }
        oldConfig = Configuration(newConfig)
    }



    /**
     * 创建通知ID
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val downloadChannel = NotificationChannel(
            channelIdDownload,
            getString(R.string.action_download),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val readAloudChannel = NotificationChannel(
            channelIdReadAloud,
            getString(R.string.read_aloud),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val webChannel = NotificationChannel(
            channelIdWeb,
            getString(R.string.web_service),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            enableLights(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        //向notification manager 提交channel
        notificationManager.createNotificationChannels(
            listOf(
                downloadChannel,
                readAloudChannel,
                webChannel
            )
        )
    }

    class EventLogger : DefaultLogger() {

        override fun log(level: Level, msg: String) {
            super.log(level, msg)
            LogUtils.d(TAG, msg)
        }

        override fun log(level: Level, msg: String, th: Throwable?) {
            super.log(level, msg, th)
            LogUtils.d(TAG, "$msg\n${th?.stackTraceToString()}")
        }

        companion object {
            private const val TAG = "[LiveEventBus]"
        }
    }

}
