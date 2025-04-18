package ltd.finelink.read.help.source

import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.BookSourcePart
import ltd.finelink.read.data.entities.rule.ExploreKind
import ltd.finelink.read.utils.ACache
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.MD5Utils
import ltd.finelink.read.utils.fromJsonArray
import ltd.finelink.read.utils.isJsonArray
import ltd.finelink.read.utils.printOnDebug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set

/**
 * 采用md5作为key可以在分类修改后自动重新计算,不需要手动刷新
 */

private val mutexMap by lazy { hashMapOf<String, Mutex>() }
private val exploreKindsMap by lazy { ConcurrentHashMap<String, List<ExploreKind>>() }
private val aCache by lazy { ACache.get("explore") }

private fun BookSource.getExploreKindsKey(): String {
    return ltd.finelink.read.utils.MD5Utils.md5Encode(bookSourceUrl + exploreUrl)
}

private fun BookSourcePart.getExploreKindsKey(): String {
    return getBookSource()!!.getExploreKindsKey()
}

suspend fun BookSourcePart.exploreKinds(): List<ExploreKind> {
    return getBookSource()!!.exploreKinds()
}

suspend fun BookSource.exploreKinds(): List<ExploreKind> {
    val exploreKindsKey = getExploreKindsKey()
    ltd.finelink.read.help.source.exploreKindsMap[exploreKindsKey]?.let { return it }
    val exploreUrl = exploreUrl
    if (exploreUrl.isNullOrBlank()) {
        return emptyList()
    }
    val mutex = ltd.finelink.read.help.source.mutexMap[bookSourceUrl] ?: Mutex().apply { ltd.finelink.read.help.source.mutexMap[bookSourceUrl] = this }
    mutex.withLock {
        ltd.finelink.read.help.source.exploreKindsMap[exploreKindsKey]?.let { return it }
        val kinds = arrayListOf<ExploreKind>()
        withContext(Dispatchers.IO) {
            kotlin.runCatching {
                var ruleStr = exploreUrl
                if (exploreUrl.startsWith("<js>", true)
                    || exploreUrl.startsWith("@js:", true)
                ) {
                    ruleStr = ltd.finelink.read.help.source.aCache.getAsString(exploreKindsKey)
                    if (ruleStr.isNullOrBlank()) {
                        val jsStr = if (exploreUrl.startsWith("@")) {
                            exploreUrl.substring(4)
                        } else {
                            exploreUrl.substring(4, exploreUrl.lastIndexOf("<"))
                        }
                        ruleStr = evalJS(jsStr).toString().trim()
                        ltd.finelink.read.help.source.aCache.put(exploreKindsKey, ruleStr)
                    }
                }
                if (ruleStr.isJsonArray()) {
                    ltd.finelink.read.utils.GSON.fromJsonArray<ExploreKind?>(ruleStr).getOrThrow().let {
                        kinds.addAll(it.filterNotNull())
                    }
                } else {
                    ruleStr.split("(&&|\n)+".toRegex()).forEach { kindStr ->
                        val kindCfg = kindStr.split("::")
                        kinds.add(ExploreKind(kindCfg.first(), kindCfg.getOrNull(1)))
                    }
                }
            }.onFailure {
                kinds.add(ExploreKind("ERROR:${it.localizedMessage}", it.stackTraceToString()))
                it.printOnDebug()
            }
        }
        ltd.finelink.read.help.source.exploreKindsMap[exploreKindsKey] = kinds
        return kinds
    }
}

suspend fun BookSourcePart.clearExploreKindsCache() {
    withContext(Dispatchers.IO) {
        val exploreKindsKey = getExploreKindsKey()
        aCache.remove(exploreKindsKey)
        exploreKindsMap.remove(exploreKindsKey)
    }
}

fun BookSource.contains(word: String?): Boolean {
    if (word.isNullOrEmpty()) {
        return true
    }
    return bookSourceName.contains(word)
            || bookSourceUrl.contains(word)
            || bookSourceGroup?.contains(word) == true
            || bookSourceComment?.contains(word) == true
}