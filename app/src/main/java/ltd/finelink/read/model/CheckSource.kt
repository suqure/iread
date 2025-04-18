package ltd.finelink.read.model

import android.content.Context
import ltd.finelink.read.R
import ltd.finelink.read.constant.IntentAction
import ltd.finelink.read.data.entities.BookSourcePart
import ltd.finelink.read.help.CacheManager
import ltd.finelink.read.help.IntentData
import ltd.finelink.read.service.CheckSourceService
import ltd.finelink.read.utils.startService
import splitties.init.appCtx

object CheckSource {
    var keyword = "我的"

    //校验设置
    var timeout = CacheManager.getLong("checkSourceTimeout") ?: 180000L
    var checkSearch = CacheManager.get("checkSearch")?.toBoolean() ?: true
    var checkDiscovery = CacheManager.get("checkDiscovery")?.toBoolean() ?: true
    var checkInfo = CacheManager.get("checkInfo")?.toBoolean() ?: true
    var checkCategory = CacheManager.get("checkCategory")?.toBoolean() ?: true
    var checkContent = CacheManager.get("checkContent")?.toBoolean() ?: true
    val summary get() = upSummary()

    fun start(context: Context, sources: List<BookSourcePart>) {
        val selectedIds = sources.map {
            it.bookSourceUrl
        }
        IntentData.put("checkSourceSelectedIds", selectedIds)
        context.startService<CheckSourceService> {
            action = IntentAction.start
        }
    }

    fun stop(context: Context) {
        context.startService<CheckSourceService> {
            action = IntentAction.stop
        }
    }

    fun resume(context: Context) {
        context.startService<CheckSourceService> {
            action = IntentAction.resume
        }
    }

    fun putConfig() {
        CacheManager.put("checkSourceTimeout", timeout)
        CacheManager.put("checkSearch", checkSearch)
        CacheManager.put("checkDiscovery", checkDiscovery)
        CacheManager.put("checkInfo", checkInfo)
        CacheManager.put("checkCategory", checkCategory)
        CacheManager.put("checkContent", checkContent)
    }

    private fun upSummary(): String {
        var checkItem = ""
        if (checkSearch) checkItem = "$checkItem ${appCtx.getString(R.string.search)}"
        if (checkDiscovery) checkItem = "$checkItem ${appCtx.getString(R.string.discovery)}"
        if (checkInfo) checkItem = "$checkItem ${appCtx.getString(R.string.source_tab_info)}"
        if (checkCategory) checkItem = "$checkItem ${appCtx.getString(R.string.chapter_list)}"
        if (checkContent) checkItem = "$checkItem ${appCtx.getString(R.string.main_body)}"
        return appCtx.getString(
            R.string.check_source_config_summary,
            (timeout / 1000).toString(),
            checkItem
        )
    }
}