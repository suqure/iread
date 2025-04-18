package ltd.finelink.read.ui.association

import android.app.Application
import androidx.lifecycle.MutableLiveData
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.constant.AppConst
import ltd.finelink.read.constant.AppLog
import ltd.finelink.read.constant.AppPattern
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.ReplaceRule
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.help.ReplaceAnalyzer
import ltd.finelink.read.help.http.newCallResponseBody
import ltd.finelink.read.help.http.okHttpClient
import ltd.finelink.read.help.http.text
import ltd.finelink.read.utils.isAbsUrl
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.isJsonObject
import ltd.finelink.read.utils.splitNotBlank

class ImportReplaceRuleViewModel(app: Application) : BaseViewModel(app) {
    var isAddGroup = false
    var groupName: String? = null
    val errorLiveData = MutableLiveData<String>()
    val successLiveData = MutableLiveData<Int>()

    val allRules = arrayListOf<ReplaceRule>()
    val checkRules = arrayListOf<ReplaceRule?>()
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
            val selectRules = arrayListOf<ReplaceRule>()
            selectStatus.forEachIndexed { index, b ->
                if (b) {
                    val rule = allRules[index]
                    if (!group.isNullOrEmpty()) {
                        if (isAddGroup) {
                            val groups = linkedSetOf<String>()
                            rule.group?.splitNotBlank(AppPattern.splitGroupRegex)?.let {
                                groups.addAll(it)
                            }
                            groups.add(group)
                            rule.group = groups.joinToString(",")
                        } else {
                            rule.group = group
                        }
                    }
                    selectRules.add(rule)
                }
            }
            appDb.replaceRuleDao.insert(*selectRules.toTypedArray())
        }.onFinally {
            finally.invoke()
        }
    }

    fun import(text: String) {
        execute {
            importAwait(text.trim())
        }.onError {
            errorLiveData.postValue("ImportError:${it.localizedMessage}")
            AppLog.put("ImportError:${it.localizedMessage}", it)
        }.onSuccess {
            comparisonSource()
        }
    }

    private suspend fun importAwait(text: String) {
        when {
            text.isAbsUrl() -> importUrl(text)
            text.isJsonArray() -> {
                val rules = ReplaceAnalyzer.jsonToReplaceRules(text).getOrThrow()
                allRules.addAll(rules)
            }
            text.isJsonObject() -> {
                val rule = ReplaceAnalyzer.jsonToReplaceRule(text).getOrThrow()
                allRules.add(rule)
            }
            else -> throw NoStackTraceException("格式不对")
        }
    }

    private suspend fun importUrl(url: String) {
        okHttpClient.newCallResponseBody {
            if (url.endsWith("#requestWithoutUA")) {
                url(url.substringBeforeLast("#requestWithoutUA"))
                header(AppConst.UA_NAME, "null")
            } else {
                url(url)
            }
        }.text("utf-8").let {
            importAwait(it)
        }
    }

    private fun comparisonSource() {
        execute {
            allRules.forEach {
                val rule = appDb.replaceRuleDao.findById(it.id)
                checkRules.add(rule)
                selectStatus.add(rule == null)
            }
        }.onSuccess {
            successLiveData.postValue(allRules.size)
        }
    }
}