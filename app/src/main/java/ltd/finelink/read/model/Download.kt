package ltd.finelink.read.model

import android.content.Context
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.service.DownloadService
import ltd.finelink.read.utils.startService

object Download {


    fun start(context: Context, url: String, fileName: String) {
        context.startService<DownloadService> {
            action = IntentAction.start
            putExtra("url", url)
            putExtra("fileName", fileName)
        }
    }

}