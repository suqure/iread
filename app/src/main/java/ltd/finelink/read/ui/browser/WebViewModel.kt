package ltd.finelink.read.ui.browser

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.webkit.URLUtil
import androidx.documentfile.provider.DocumentFile
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.data.appDb
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.CacheManager
import ltd.finelink.read.help.IntentData
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.source.SourceVerificationHelp
import ltd.finelink.read.model.analyzeRule.AnalyzeUrl
import ltd.finelink.read.utils.DocumentUtils
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.toastOnUi
import ltd.finelink.read.utils.writeBytes
import java.io.File
import java.util.Date

class WebViewModel(application: Application) : BaseViewModel(application) {
    var baseUrl: String = ""
    var html: String? = null
    val headerMap: HashMap<String, String> = hashMapOf()
    var sourceVerificationEnable: Boolean = false
    var sourceOrigin: String = ""
    var key = ""

    fun initData(
        intent: Intent,
        success: () -> Unit
    ) {
        execute {
            val url = intent.getStringExtra("url")
                ?: throw NoStackTraceException("url不能为空")
            sourceOrigin = intent.getStringExtra("sourceOrigin") ?: ""
            key = SourceVerificationHelp.getKey(sourceOrigin)
            sourceVerificationEnable = intent.getBooleanExtra("sourceVerificationEnable", false)
            val headerMapF = IntentData.get<Map<String, String>>(url)
            val analyzeUrl = AnalyzeUrl(url, headerMapF = headerMapF)
            baseUrl = analyzeUrl.url
            headerMap.putAll(analyzeUrl.headerMap)
            if (analyzeUrl.isPost()) {
                html = analyzeUrl.getStrResponseAwait(useWebView = false).body
            }
        }.onSuccess {
            success.invoke()
        }.onError {
            context.toastOnUi("error\n${it.localizedMessage}")
            it.printOnDebug()
        }
    }

    fun saveImage(webPic: String?, path: String) {
        webPic ?: return
        execute {
            val fileName = "${AppConst.fileNameFormat.format(Date(System.currentTimeMillis()))}.jpg"
            webData2bitmap(webPic)?.let { biteArray ->
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)
                            ?.writeBytes(context, biteArray)
                    }
                } else {
                    val file = FileUtils.createFileIfNotExist(File(path), fileName)
                    file.writeBytes(biteArray)
                }
            } ?: throw Throwable("NULL")
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

    fun saveVerificationResult(intent: Intent, success: () -> Unit) {
        execute {
            if (sourceVerificationEnable) {
                val url = intent.getStringExtra("url")!!
                val source = appDb.bookSourceDao.getBookSource(sourceOrigin)
                val key = "${sourceOrigin}_verificationResult"
                html = AnalyzeUrl(
                    url,
                    headerMapF = headerMap,
                    source = source
                ).getStrResponseAwait(useWebView = false).body
                CacheManager.putMemory(key, html ?: "")
            }
        }.onSuccess {
            success.invoke()
        }
    }

}