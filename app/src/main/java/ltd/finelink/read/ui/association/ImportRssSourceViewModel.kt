package ltd.finelink.read.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.jayway.jsonpath.JsonPath
import ltd.finelink.read.R
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.config.AppConfig
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.unCompress
import ltd.finelink.read.help.source.SourceHelp
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonArray
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.splitNotBlank

class ImportRssSourceViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allSources = arrayListOf<RssSource>()
    val checkSources = arrayListOf<RssSource?>()
    val selectStatus = arrayListOf<Boolean>()

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
            val group = groupName?.trim()
            val keepName = AppConfig.importKeepName
            val keepGroup = AppConfig.importKeepGroup
            val keepEnable = AppConfig.importKeepEnable
            val selectSource = arrayListOf<RssSource>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val source = allSources[index]
                    checkSources[index]?.let {
                        if (keepName) {
                            source.sourceName = it.sourceName
                        }
                        if (keepGroup) {
                            source.sourceGroup = it.sourceGroup
                        }
                        if (keepEnable) {
                            source.enabled = it.enabled
                        }
                        source.customOrder = it.customOrder
                    }
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            source.sourceGroup?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            source.sourceGroup = groups.joinToString(",")
                        } else {
                            source.sourceGroup = group
                        }
                    }
                    selectSource.add(source)
                }
            }
            SourceHelp.insertRssSource(*selectSource.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun importSource(text: String) {
        execute {
            val mText = text.trim()
            when {
                mText.isJsonObject() -> kotlin.runCatching {
                    val json = JsonPath.parse(mText)
                    val urls = json.read<List<String>>("$.sourceUrls")
                    if (!urls.isNullOrEmpty()) {
                        urls.forEach {
                            importSourceUrl(it)
                        }
                    }
                }.onFailure {
                    GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                        val source = it.firstOrNull() ?: return@let
                        if (source.sourceUrl.isEmpty()) {
                            throw NoStackTraceException("不是订阅源")
                        }
                        allSources.addAll(it)
                    }
                }

                mText.isJsonArray() -> {
                    GSON.fromJsonArray<RssSource>(mText).getOrThrow().let {
                        val source = it.firstOrNull() ?: return@let
                        if (source.sourceUrl.isEmpty()) {
                            throw NoStackTraceException("不是订阅源")
                        }
                        allSources.addAll(it)
                    }
                }

                mText.isAbsUrl() -> {
                    importSourceUrl(mText)
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
        }.unCompress { body ->
            val items: List<Map<String, Any>> = jsonPath.parse(body).read("$")
            for (item in items) {
                if (!item.containsKey("sourceUrl")) {
                    throw NoStackTraceException("不是订阅源")
                }
                val jsonItem = jsonPath.parse(item)
                GSON.fromJsonObject<RssSource>(jsonItem.jsonString()).getOrThrow().let { source ->
                    allSources.add(source)
                }
            }
        }
    }

    private fun comparisonSource() {
        execute {
            allSources.forEach {
                val has = appDb.rssSourceDao.getByKey(it.sourceUrl)
                checkSources.add(has)
                selectStatus.add(has == null)
            }
            successLiveData.postValue(allSources.size)
        }
    }

}