package ltd.finelink.read.ui.association

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.book.ContentProcessor
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.unCompress
import ltd.finelink.read.help.source.SourceHelp
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonArray
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.inputStream
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.isUri
import ltd.finelink.read.utils.splitNotBlank


class ImportBookSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<BookSource>()
    val checkSources = arrayListOf<BookSource?>()
    val selectStatus = arrayListOf<Boolean>()
    val newSourceStatus = arrayListOf<Boolean>()
    val updateSourceStatus = arrayListOf<Boolean>()

    val isSelectAll: Boolean
        get() {
            selectStatus.forEach {
                if (!it) {
                    return false
                }
            }
            return true
        }

    val isSelectAllNew: Boolean
        get() {
            newSourceStatus.forEachIndexed { index, b ->
                if (b && !selectStatus[index]) {
                    return false
                }
            }
            return true
        }

    val isSelectAllUpdate: Boolean
        get() {
            updateSourceStatus.forEachIndexed { index, b ->
                if (b && !selectStatus[index]) {
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
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val keepGroup = AppConfig.importKeepGroup
            val keepEnable = AppConfig.importKeepEnable
            val selectSource = arrayListOf<BookSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    checkSources[index]?.let {
                        if (keepName) {
                            source.bookSourceName = it.bookSourceName
                        }
                        if (keepGroup) {
                            source.bookSourceGroup = it.bookSourceGroup
                        }
                        if (keepEnable) {
                            source.enabled = it.enabled
                            source.enabledExplore = it.enabledExplore
                        }
                        source.customOrder = it.customOrder
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.bookSourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.bookSourceGroup = groups.joinToString(",")
                        } else {
                            source.bookSourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertBookSource(*selectSource.toTypedArray())
            ContentProcessor.upReplaceRules()
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> {
                    kotlin.runCatching {
                        val json = JsonPath.parse(mText)
                        json.read<List<String>>("$.sourceUrls")
                    }.onSuccess { listUrl ->
                        listUrl.forEach {
                            importSourceUrl(it)
                        }
                    }.onFailure {
                        GSON.fromJsonObject<BookSource>(mText).getOrThrow().let {
                            if (it.bookSourceUrl.isEmpty()) {
                                throw NoStackTraceException("不是书源")
                            }
                            allSources.add(it)
                        }
                    }
                }

                mText.isJsonArray() -> GSON.fromJsonArray<BookSource>(mText).getOrThrow()
                    .let { items ->
                        val source = items.firstOrNull() ?: return@let
                        if (source.bookSourceUrl.isEmpty()) {
                            throw NoStackTraceException("不是书源")
                        }
                        allSources.addAll(items)
                    }

                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
                }

                mText.isUri() -> {
                    val uri = Uri.parse(mText)
                    uri.inputStream(context).getOrThrow().use { inputS ->
                        GSON.fromJsonArray<BookSource>(inputS).getOrThrow().let {
                            val source = it.firstOrNull() ?: return@let
                            if (source.bookSourceUrl.isEmpty()) {
                                throw NoStackTraceException("不是书源")
                            }
                            allSources.addAll(it)
                        }
                    }
                }

                else -> throw NoStackTraceException(context.getString(R.string.wrong_format))
            }
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
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
        }.unCompress {
            GSON.fromJsonArray<BookSource>(it).getOrThrow().let { list ->
                val source = list.firstOrNull() ?: return@let
                if (source.bookSourceUrl.isEmpty()) {
                    throw NoStackTraceException("不是书源")
                }
                allSources.addAll(list)
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val source = appDb.bookSourceDao.getBookSource(it.bookSourceUrl)
                checkSources.add(source)
                selectStatus.add(source == null || source.lastUpdateTime < it.lastUpdateTime)
                newSourceStatus.add(source == null)
                updateSourceStatus.add(source != null && source.lastUpdateTime < it.lastUpdateTime)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}