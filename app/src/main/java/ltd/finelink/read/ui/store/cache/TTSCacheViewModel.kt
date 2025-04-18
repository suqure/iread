package ltd.finelink.read.ui.store.cache

import android.app.Application
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.utils.DocumentUtils
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.WavFileUtils
import ltd.finelink.read.utils.isContentScheme
import ltd.finelink.read.utils.openOutputStream
import splitties.init.appCtx
import java.io.File
import java.io.FileInputStream

/**
 * 订阅源管理数据修改
 * 修改数据要copy,直接修改会导致界面不刷新
 */
class TTSCacheViewModel(application: Application) : BaseViewModel(application) {

    private val ttsFolderPath: String by lazy {
        appCtx.cacheDir.absolutePath + File.separator + "AppTTS" + File.separator
    }
    fun clearAll(vararg sources: TTSCache) {
        execute {
            FileUtils.listDirsAndFiles(ttsFolderPath)?.forEach {
                FileUtils.delete(it.absolutePath)
            }
            appDb.ttsCacheDao.deleteAll()
        }
    }


    fun del(vararg ttsCache: TTSCache) {
        execute {
            appDb.ttsCacheDao.delete(*ttsCache)
            ttsCache.forEach { cache->
                FileUtils.delete(cache.file)
            }
        }
    }

    fun export(path:String,ttsCache:List<TTSCache>,success: (fileName:String) -> Unit,finish:()->Unit){
        var file = System.currentTimeMillis().toString()+".wav"
        execute {
            var sources:MutableList<String> = java.util.ArrayList()
            ttsCache.forEach {
                sources.add(it.file)
            }
            if (path.isContentScheme()) {
                val uri = Uri.parse(path)
                DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                    DocumentUtils.createFileIfNotExist(doc, file)?.let {
                        it.openOutputStream()?.let { out->
                            WavFileUtils.rawToCombine(sources,out)
                        }
                    }
                }
            } else {
                WavFileUtils.rawToCombine(sources,FileUtils.createFileIfNotExist(File(path),file).outputStream())
            }
        }.onSuccess {
            it?.let {
                success.invoke(file)
            }
        }.onFinally {
            finish()
        }
    }

    fun exportPer(path:String,ttsCache:List<TTSCache>,success: () -> Unit,finish:()->Unit){
        execute {
            var count = 0
            ttsCache.forEach { cache->
                var source = File(cache.file)
                var fileName = cache.bookName+cache.chapterTitle+cache.position+"-$count.wav"
                if (path.isContentScheme()) {
                    val uri = Uri.parse(path)
                    DocumentFile.fromTreeUri(context, uri)?.let { doc ->
                        DocumentUtils.createFileIfNotExist(doc, fileName)?.let {
                             it.openOutputStream()?.let {outputStream->
                                val inputStream = FileInputStream(source)
                                inputStream.use {
                                    outputStream.use {
                                        inputStream.copyTo(outputStream)
                                        outputStream.flush()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    FileUtils.copy(source,File(path,fileName))
                }
                count++
            }

        }.onSuccess {
            it?.let {
                success.invoke()
            }
        }.onFinally {
            finish()
        }
    }



    fun update(vararg rssSource: TTSCache) {
        execute { appDb.ttsCacheDao.update(*rssSource) }
    }





}