package ltd.finelink.read.help

import ltd.finelink.read.data.entities.ReplaceRule
import ltd.finelink.read.exception.NoStackTraceException
import ltd.finelink.read.utils.*
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonObject
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.readBool
import ltd.finelink.read.utils.readInt
import ltd.finelink.read.utils.readLong
import ltd.finelink.read.utils.readString

object ReplaceAnalyzer {

    fun jsonToReplaceRules(json: String): Result<MutableList<ReplaceRule>> {
        return kotlin.runCatching {
            val replaceRules = mutableListOf<ReplaceRule>()
            val items: List<Map<String, Any>> = jsonPath.parse(json).read("$")
            for (item in items) {
                val jsonItem = jsonPath.parse(item)
                jsonToReplaceRule(jsonItem.jsonString()).getOrThrow().let {
                    if (it.isValid()) {
                        replaceRules.add(it)
                    }
                }
            }
            replaceRules
        }
    }

    fun jsonToReplaceRule(json: String): Result<ReplaceRule> {
        return runCatching {
            val replaceRule: ReplaceRule? =
                GSON.fromJsonObject<ReplaceRule>(json.trim()).getOrNull()
            if (replaceRule == null || replaceRule.pattern.isEmpty()) {
                val jsonItem = jsonPath.parse(json.trim())
                val rule = ReplaceRule()
                rule.id = jsonItem.readLong("$.id") ?: System.currentTimeMillis()
                rule.pattern = jsonItem.readString("$.regex") ?: ""
                if (rule.pattern.isEmpty()) throw NoStackTraceException("格式不对")
                rule.name = jsonItem.readString("$.replaceSummary") ?: ""
                rule.replacement = jsonItem.readString("$.replacement") ?: ""
                rule.isRegex = jsonItem.readBool("$.isRegex") == true
                rule.scope = jsonItem.readString("$.useTo")
                rule.isEnabled = jsonItem.readBool("$.enable") == true
                rule.order = jsonItem.readInt("$.serialNumber") ?: 0
                return@runCatching rule
            }
            return@runCatching replaceRule
        }
    }

}