package ltd.finelink.read.ui.config

import android.app.Application
import android.content.Context
import kotlinx.coroutines.delay
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.help.AppWebDav
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.restart
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx

class ConfigViewModel(application: Application) : BaseViewModel(application) {

    fun upWebDavConfig() {
        execute {
            AppWebDav.upConfig()
        }
    }

    fun clearCache() {
        execute {
            BookHelp.clearCache()
            FileUtils.delete(context.cacheDir.absolutePath)
        }.onSuccess {
            appCtx.toastOnUi(R.string.clear_cache_success)
        }
    }

    fun clearWebViewData() {
        execute {
            FileUtils.delete(context.getDir("webview", Context.MODE_PRIVATE))
            appCtx.toastOnUi(R.string.clear_webview_data_success)
            delay(3000)
            appCtx.restart()
        }
    }

    fun shrinkDatabase() {
        execute {
            appDb.openHelper.writableDatabase.execSQL("VACUUM")
        }.onSuccess {
            appCtx.toastOnUi(R.string.success)
        }
    }

}
