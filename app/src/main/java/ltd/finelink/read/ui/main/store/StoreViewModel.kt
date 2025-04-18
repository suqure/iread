package ltd.finelink.read.ui.main.store

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.help.coroutine.Coroutine
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.toastOnUi
import splitties.init.appCtx
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.zip.ZipFile

class StoreViewModel(application: Application) : BaseViewModel(application) {
    val addStoreProgressLiveData = MutableLiveData(-1)
    var addStoreJob: Coroutine<*>? = null

    private val speakerDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator)
    }
    private val modelDir: File by lazy {
        FileUtils.createFolderIfNotExist(appCtx.externalFiles.absolutePath + File.separator + "model" + File.separator)
    }

    fun importFromZip(uri: Uri,finally: () -> Unit){
        execute {
            uri.inputStream(appCtx).getOrThrow().use {inputStream->
                var file =  FileUtils.createFileIfNotExist(appCtx.cacheDir,"import_tmp.zip")
                FileUtils.writeInputStream(file,inputStream)
                val zipFile = ZipFile(file)
                var type:Int? = null
                var config = StringBuffer()
                for (entry in zipFile.entries()) {
                    if(entry.name=="md.config"){
                        type = 0
                        zipFile.getInputStream(entry).use { input ->
                            var br =  BufferedReader(InputStreamReader(input))
                            while (true) {
                                val line = br.readLine()
                                if (line != null) {
                                    config.append(line)
                                } else {
                                    break
                                }
                            }
                        }
                        break
                    }else if(entry.name=="vc.config"){
                        type = 1
                        zipFile.getInputStream(entry).use { input ->
                            var br =  BufferedReader(InputStreamReader(input))
                            while (true) {
                                val line = br.readLine()
                                if (line != null) {
                                    config.append(line)
                                } else {
                                    break
                                }
                            }
                        }
                        break
                    }
                }
                type?.let {
                    if(it==0){
                        unzipModel(zipFile,config.toString())
                    }else if(it==1){
                        unzipVoice(zipFile,config.toString())
                    }
                    file.delete()
                    return@execute
                }

            }
            appCtx.toastOnUi(R.string.data_Illegal)
        }.onFinally {
            finally.invoke()
        }

    }

    private fun unzipModel(zipFile: ZipFile,config:String){
        var model = LocalTTS.fromJson(config)
        model.getOrNull()?.let {
            var modelFilesDir =
                FileUtils.createFolderIfNotExist(modelDir, it.id.toString())
            it.download = true
            it.progress = 100
            it.local= modelFilesDir.absolutePath
            it.cover?.let {cover->
                if(cover.isNotEmpty()&&!cover.isAbsUrl()){
                    it.cover = modelFilesDir.absolutePath+File.separator+cover
                }
            }
            appDb.localTTSDao.get(it.id)?.let { old->
                if(old.download){
                    appCtx.toastOnUi(R.string.file_exits)
                    return
                }
            }
            for (entry in zipFile.entries()) {
                if(entry.name=="md.config"){
                    continue
                }
                if (entry.isDirectory) {
                    File(modelFilesDir, entry.name).mkdirs()
                } else {
                    File(modelFilesDir, entry.name).outputStream().use { output ->
                        zipFile.getInputStream(entry).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            appDb.localTTSDao.insert(it)
            return
        }
        appCtx.toastOnUi(R.string.data_Illegal)

    }
    private fun unzipVoice(zipFile: ZipFile,config:String){
        var model = TTSSpeaker.fromJsonArray(config)
        model.getOrNull()?.let { items->
            items.forEach{
                it.download = true
                it.progress = 100
                it.cover?.let {cover->
                    if(cover.isNotEmpty()&&!cover.isAbsUrl()){
                        it.cover = speakerDir.absolutePath+File.separator+cover
                    }
                }
            }
            for (entry in zipFile.entries()) {
                if(entry.name=="vc.config"){
                    continue
                }
                if (entry.isDirectory) {
                    File(speakerDir, entry.name).mkdirs()
                } else {
                    File(speakerDir, entry.name).outputStream().use { output ->
                        zipFile.getInputStream(entry).use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            appDb.ttsSpeakerDao.insert(*items.toTypedArray())
            return
        }
        appCtx.toastOnUi(R.string.data_Illegal)

    }



}
