package ltd.finelink.read.model.remote

import android.net.Uri
import ltd.finelink.read.constant.AppPattern.archiveFileRegex
import ltd.finelink.read.constant.AppPattern.bookFileRegex
import ltd.finelink.read.constant.BookType
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.lib.webdav.Authorization
import ltd.finelink.read.lib.webdav.WebDav
import ltd.finelink.read.lib.webdav.WebDavFile
import ltd.finelink.read.model.analyzeRule.CustomUrl
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.utils.NetworkUtils
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.readBytes
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File

class RemoteBookWebDav(
    val rootBookUrl: String,
    val authorization: Authorization,
    val serverID: Long? = null
) : RemoteBookManager() {

    init {
        runBlocking {
            WebDav(rootBookUrl, authorization).makeAsDir()
        }
    }


    @Throws(Exception::class)
    override suspend fun getRemoteBookList(path: String): MutableList<RemoteBook> {
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        val remoteBooks = mutableListOf<RemoteBook>()
        //读取文件列表
        val remoteWebDavFileList: List<WebDavFile> = WebDav(path, authorization).listFiles()
        //转化远程文件信息到本地对象
        remoteWebDavFileList.forEach { webDavFile ->
            if (webDavFile.isDir
                || bookFileRegex.matches(webDavFile.displayName)
                || archiveFileRegex.matches(webDavFile.displayName)
            ) {
                //扩展名符合阅读的格式则认为是书籍
                remoteBooks.add(RemoteBook(webDavFile))
            }
        }
        return remoteBooks
    }

    override suspend fun getRemoteBook(path: String): RemoteBook? {
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        val webDavFile = WebDav(path, authorization).getWebDavFile()
            ?: return null
        return RemoteBook(webDavFile)
    }

    override suspend fun downloadRemoteBook(remoteBook: RemoteBook): Uri {
        AppConfig.defaultBookTreeUri
            ?: throw NoStackTraceException("没有设置书籍保存位置!")
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        val webdav = WebDav(remoteBook.path, authorization)
        return webdav.downloadInputStream().let { inputStream ->
            LocalBook.saveBookFile(inputStream, remoteBook.filename)
        }
    }

    override suspend fun upload(book: Book) {
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        val localBookUri = Uri.parse(book.bookUrl)
        val putUrl = "$rootBookUrl${File.separator}${book.originName}"
        val webDav = WebDav(putUrl, authorization)
        if (localBookUri.isContentScheme()) {
            webDav.upload(
                byteArray = localBookUri.readBytes(appCtx),
                contentType = "application/octet-stream"
            )
        } else {
            webDav.upload(localBookUri.path!!)
        }
        book.origin = BookType.webDavTag + CustomUrl(putUrl)
            .putAttribute("serverID", serverID)
            .toString()
        book.save()
    }

    override suspend fun delete(remoteBookUrl: String) {
        if (!NetworkUtils.isAvailable()) throw NoStackTraceException("网络不可用")
        WebDav(remoteBookUrl, authorization).delete()
    }

}
