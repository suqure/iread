package ltd.finelink.read.api.controller

import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import ltd.finelink.read.api.ReturnData
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookProgress
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.help.CacheManager
import ltd.finelink.read.help.book.BookHelp
import ltd.finelink.read.help.book.ContentProcessor
import ltd.finelink.read.help.book.isLocal
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.glide.ImageLoader
import ltd.finelink.read.model.BookCover
import ltd.finelink.read.model.ImageProvider
import ltd.finelink.read.model.ReadBook
import ltd.finelink.read.model.localBook.LocalBook
import ltd.finelink.read.model.webBook.WebBook
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.cnCompare
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.printOnDebug
import ltd.finelink.read.utils.stackTraceStr
import splitties.init.appCtx

object BookController {

    private lateinit var book: Book
    private var bookSource: BookSource? = null
    private var bookUrl: String = ""

    /**
     * 书架所有书籍
     */
    val bookshelf: ReturnData
        get() {
            val books = appDb.bookDao.all
            val returnData = ReturnData()
            return if (books.isEmpty()) {
                returnData.setErrorMsg("还没有添加小说")
            } else {
                val data = when (AppConfig.bookshelfSort) {
                    1 -> books.sortedByDescending { it.latestChapterTime }
                    2 -> books.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> books.sortedBy { it.order }
                    else -> books.sortedByDescending { it.durChapterTime }
                }
                returnData.setData(data)
            }
        }

    /**
     * 获取封面
     */
    fun getCover(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val coverPath = parameters["path"]?.firstOrNull()
        val ftBitmap = ImageLoader.loadBitmap(appCtx, coverPath).submit()
        return try {
            returnData.setData(ftBitmap.get())
        } catch (e: Exception) {
            returnData.setData(BookCover.defaultDrawable.toBitmap())
        }
    }

    /**
     * 获取正文图片
     */
    fun getImg(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val bookUrl = parameters["url"]?.firstOrNull()
            ?: return returnData.setErrorMsg("bookUrl为空")
        val src = parameters["path"]?.firstOrNull()
            ?: return returnData.setErrorMsg("图片链接为空")
        val width = parameters["width"]?.firstOrNull()?.toInt() ?: 640
        if (BookController.bookUrl != bookUrl) {
            book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("bookUrl不对")
            bookSource = appDb.bookSourceDao.getBookSource(book.origin)
        }
        BookController.bookUrl = bookUrl
        val bitmap = runBlocking {
            ImageProvider.cacheImage(book, src, bookSource)
            ImageProvider.getImage(book, src, width)
        }
        return returnData.setData(bitmap)
    }

    /**
     * 更新目录
     */
    fun refreshToc(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        try {
            val bookUrl = parameters["url"]?.firstOrNull()
            if (bookUrl.isNullOrEmpty()) {
                return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
            }
            val book = appDb.bookDao.getBook(bookUrl)
                ?: return returnData.setErrorMsg("未在数据库找到对应书籍，请先添加")
            if (book.isLocal) {
                val toc = LocalBook.getChapterList(book)
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            } else {
                val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
                    ?: return returnData.setErrorMsg("未找到对应书源,请换源")
                val toc = runBlocking {
                    if (book.tocUrl.isBlank()) {
                        WebBook.getBookInfoAwait(bookSource, book)
                    }
                    WebBook.getChapterListAwait(bookSource, book).getOrThrow()
                }
                appDb.bookChapterDao.delByBook(book.bookUrl)
                appDb.bookChapterDao.insert(*toc.toTypedArray())
                appDb.bookDao.update(book)
                return returnData.setData(toc)
            }
        } catch (e: Exception) {
            return returnData.setErrorMsg(e.localizedMessage ?: "refresh toc error")
        }
    }

    /**
     * 获取目录
     */
    fun getChapterList(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        val chapterList = appDb.bookChapterDao.getChapterList(bookUrl)
        if (chapterList.isEmpty()) {
            return refreshToc(parameters)
        }
        return returnData.setData(chapterList)
    }

    /**
     * 获取正文
     */
    fun getBookContent(parameters: Map<String, List<String>>): ReturnData {
        val bookUrl = parameters["url"]?.firstOrNull()
        val index = parameters["index"]?.firstOrNull()?.toInt()
        val returnData = ReturnData()
        if (bookUrl.isNullOrEmpty()) {
            return returnData.setErrorMsg("参数url不能为空，请指定书籍地址")
        }
        if (index == null) {
            return returnData.setErrorMsg("参数index不能为空, 请指定目录序号")
        }
        val book = appDb.bookDao.getBook(bookUrl)
        val chapter = runBlocking {
            var chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
            var wait = 0
            while (chapter == null && wait < 30) {
                delay(1000)
                chapter = appDb.bookChapterDao.getChapter(bookUrl, index)
                wait++
            }
            chapter
        }
        if (book == null || chapter == null) {
            return returnData.setErrorMsg("未找到")
        }
        var content: String? = BookHelp.getContent(book, chapter)
        if (content != null) {
            val contentProcessor = ContentProcessor.get(book.name, book.origin)
            content = runBlocking {
                contentProcessor.getContent(book, chapter, content!!, includeTitle = false)
                    .toString()
            }
            return returnData.setData(content)
        }
        val bookSource = appDb.bookSourceDao.getBookSource(book.origin)
            ?: return returnData.setErrorMsg("未找到书源")
        try {
            content = runBlocking {
                WebBook.getContentAwait(bookSource, book, chapter).let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    contentProcessor.getContent(book, chapter, it, includeTitle = false)
                        .toString()
                }
            }
            returnData.setData(content)
        } catch (e: Exception) {
            returnData.setErrorMsg(e.stackTraceStr)
        }
        return returnData
    }

    /**
     * 保存书籍
     */
    suspend fun saveBook(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<Book>(postData).getOrNull()?.let { book ->

            book.save()
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 删除书籍
     */
    fun deleteBook(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<Book>(postData).getOrNull()?.let { book ->
            book.delete()
            return returnData.setData("")
        }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 保存进度
     */
    suspend fun saveBookProgress(postData: String?): ReturnData {
        val returnData = ReturnData()
        GSON.fromJsonObject<BookProgress>(postData)
            .onFailure { it.printOnDebug() }
            .getOrNull()?.let { bookProgress ->
                appDb.bookDao.getBook(bookProgress.name, bookProgress.author)?.let { book ->
                    book.durChapterIndex = bookProgress.durChapterIndex
                    book.durChapterPos = bookProgress.durChapterPos
                    book.durChapterTitle = bookProgress.durChapterTitle
                    book.durChapterTime = bookProgress.durChapterTime

                    appDb.bookDao.update(book)
                    ReadBook.book?.let {
                        if (it.name == bookProgress.name &&
                            it.author == bookProgress.author
                        ) {
                            ReadBook.webBookProgress = bookProgress
                        }
                    }
                    return returnData.setData("")
                }
            }
        return returnData.setErrorMsg("格式不对")
    }

    /**
     * 添加本地书籍
     */
    fun addLocalBook(parameters: Map<String, List<String>>): ReturnData {
        val returnData = ReturnData()
        val fileName = parameters["fileName"]?.firstOrNull()
            ?: return returnData.setErrorMsg("fileName 不能为空")
        val fileData = parameters["fileData"]?.firstOrNull()
            ?: return returnData.setErrorMsg("fileData 不能为空")
        kotlin.runCatching {
            LocalBook.importFileOnLine(fileData, fileName)
        }.onFailure {
            return when (it) {
                is SecurityException -> returnData.setErrorMsg("需重新设置书籍保存位置!")
                else -> returnData.setErrorMsg("保存书籍错误\n${it.localizedMessage}")
            }
        }
        return returnData.setData(true)
    }

    /**
     * 保存web阅读界面配置
     */
    fun saveWebReadConfig(postData: String?): ReturnData {
        val returnData = ReturnData()
        postData?.let {
            CacheManager.put("webReadConfig", postData)
        } ?: CacheManager.delete("webReadConfig")
        return returnData.setData("")
    }

    /**
     * 获取web阅读界面配置
     */
    fun getWebReadConfig(): ReturnData {
        val returnData = ReturnData()
        val data = CacheManager.get("webReadConfig")
            ?: return returnData.setErrorMsg("没有配置")
        return returnData.setData(data)
    }

}
