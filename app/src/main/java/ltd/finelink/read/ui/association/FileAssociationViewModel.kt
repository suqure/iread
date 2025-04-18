package ltd.finelink.read.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.constant.AppPattern.bookFileRegex
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.ArchiveUtils
import ltd.finelink.read.utils.FileDoc
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.isFileScheme
import ltd.finelink.read.utils.isJson
import ltd.finelink.read.utils.openInputStream
import ltd.finelink.read.utils.printOnDebug

class FileAssociationViewModel(application: Application) : BaseAssociationViewModel(application) {
    val importBookLiveData = MutableLiveData<Uri>()
    val onLineImportLive = MutableLiveData<Uri>()
    val openBookLiveData = MutableLiveData<String>()
    val notSupportedLiveData = MutableLiveData<Pair<Uri, String>>()

    fun dispatchIntent(uri: Uri) {
        execute {
            //如果是普通的url，需要根据返回的内容判断是什么
            if (uri.isContentScheme() || uri.isFileScheme()) {
                val fileDoc = FileDoc.fromUri(uri, false)
                val fileName = fileDoc.name
                if (fileName.matches(AppPattern.archiveFileRegex)) {
                    ArchiveUtils.deCompress(fileDoc, ArchiveUtils.TEMP_PATH) {
                        it.matches(bookFileRegex)
                    }.forEach {
                        dispatch(FileDoc.fromFile(it))
                    }
                } else {
                    dispatch(fileDoc)
                }
            } else {
                onLineImportLive.postValue(uri)
            }
        }.onError {
            it.printOnDebug()
            val msg = "无法打开文件\n${it.localizedMessage}"
            errorLive.postValue(msg)
            AppLog.put(msg, it)
        }
    }

    private fun dispatch(fileDoc: FileDoc) {
        kotlin.runCatching {
            if (fileDoc.openInputStream().getOrNull().isJson()) {
                importJson(fileDoc.uri)
                return
            }
        }.onFailure {
            it.printOnDebug()
            AppLog.put("尝试导入为JSON文件失败\n${it.localizedMessage}", it)
        }
        if (fileDoc.name.matches(bookFileRegex)) {
            importBookLiveData.postValue(fileDoc.uri)
            return
        }
        notSupportedLiveData.postValue(Pair(fileDoc.uri, fileDoc.name))
    }

    fun importBook(uri: Uri) {
        val book = LocalBook.importFile(uri)
        openBookLiveData.postValue(book.bookUrl)
    }
}