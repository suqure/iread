package ltd.finelink.read.ui.login

import android.app.Application
import android.content.Intent
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BaseSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.toastOnUi

class SourceLoginViewModel(application: Application) : BaseViewModel(application) {

    var source: BaseSource? = null

    fun initData(intent: Intent, success: (bookSource: BaseSource) -> Unit) {
        execute {
            val sourceKey = intent.getStringExtra("key")
                ?: throw NoStackTraceException("没有参数")
            when (intent.getStringExtra("type")) {
                "bookSource" -> source = appDb.bookSourceDao.getBookSource(sourceKey)
                "rssSource" -> source = appDb.rssSourceDao.getByKey(sourceKey)
            }
            source
        }.onSuccess {
            if (it != null) {
                success.invoke(it)
            } else {
                context.toastOnUi("未找到书源")
            }
        }
    }

}