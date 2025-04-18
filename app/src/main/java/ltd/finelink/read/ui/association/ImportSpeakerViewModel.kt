package ltd.finelink.read.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.text
import ltd.finelink.read.utils.FileUtils
import ltd.finelink.read.utils.externalFiles
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import splitties.init.appCtx
import java.io.File

class ImportSpeakerViewModel(app: Application) : BaseViewModel(app) {

    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<TTSSpeaker>()
    val checkSources = arrayListOf<TTSSpeaker?>()
    val selectStatus = arrayListOf<Boolean>()

    private val speakerFolderPath: String by lazy {

        appCtx.externalFiles.absolutePath + File.separator + "speaker" + File.separator
    }

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val selectCount: Int
        get() {
            var count = 0
            selectStatus.forEach {
                if (it) {
                    count++
                }
            }
            return count
        }

    fun importSelect(finally: () -> Unit) {
        execute {
            val selectSource = arrayListOf<TTSSpeaker>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    selectSource.add(allSources[index])
                }
            }
            appDb.ttsSpeakerDao.insert(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            importSourceAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importSourceAwait(text: String) {
        when {
            text.isJsonObject() -> {
                TTSSpeaker.fromJson(text).getOrThrow().let {
                    allSources.add(it)
                }
            }
            text.isJsonArray() -> TTSSpeaker.fromJsonArray(text).getOrThrow().let { items ->
                allSources.addAll(items)
            }
            text.isAbsUrl() -> {
                importSourceUrl(text)
            }
            else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
        }
    }

    private suspend fun importSourceUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.text().let {
            importSourceAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.ttsSpeakerDao.get(it.id)
                source?.let { old->
                    if(it.path ==old.path){
                        it.download = old.download
                        it.progress = old.progress
                    }else{
                        if(old.download){
                            var speakerDir = FileUtils.createFolderIfNotExist(speakerFolderPath);
                            var name = old.speakerFile()
                            var file = File("${speakerDir}/${name}")
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    }
                }
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}