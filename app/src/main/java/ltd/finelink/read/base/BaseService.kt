package ltd.finelink.read.base

import android.content.Intent
import android.os.IBinder
import androidx.annotation.CallSuper
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import ltd.finelink.read.R
import ltd.finelink.read.help.LifecycleHelp
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.lib.permission.Permissions
import ltd.finelink.read.lib.permission.PermissionsCompat
import ltd.finelink.read.utils.LogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlin.coroutines.CoroutineContext

abstract class BaseService : LifecycleService() {

    private val simpleName = this::class.simpleName.toString()

    fun <T> execute(
        scope: CoroutineScope = lifecycleScope,
        context: CoroutineContext = Dispatchers.IO,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        executeContext: CoroutineContext = Dispatchers.Main,
        block: suspend CoroutineScope.() -> T
    ) = Coroutine.async(scope, context, start, executeContext, block)

    @CallSuper
    override fun onCreate() {
        super.onCreate()
        LifecycleHelp.onServiceCreate(this)
        checkNotificationPermission()
    }

    @CallSuper
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        LogUtils.d(simpleName) {
            "onStartCommand $intent ${intent?.toUri(0)}"
        }
        startForegroundNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    @CallSuper
    override fun onTaskRemoved(rootIntent: Intent?) {
        LogUtils.d(simpleName, "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        LifecycleHelp.onServiceDestroy(this)
    }

    /**
     * 开启前台服务并发送通知
     */
    open fun startForegroundNotification() {

    }

    /**
     * 检测通知权限
     */
    private fun checkNotificationPermission() {
        PermissionsCompat.Builder()
            .addPermissions(Permissions.POST_NOTIFICATIONS)
            .rationale(R.string.notification_permission_rationale)
            .onGranted {
                if (lifecycleScope.isActive) {
                    startForegroundNotification()
                }
            }
            .request()
    }
}