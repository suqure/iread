package ltd.finelink.read.data.entities

import ltd.finelink.read.help.RuleBigDataHelp
import ltd.finelink.read.model.analyzeRule.RuleDataInterface
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.splitNotBlank

interface BaseBook : RuleDataInterface {
    var name: String
    var author: String
    var bookUrl: String
    var kind: String?
    var wordCount: String?
    var variable: String?

    var infoHtml: String?
    var tocHtml: String?

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    fun putCustomVariable(value: String?) {
        putVariable("custom", value)
    }

    fun getCustomVariable(): String {
        return getVariable("custom")
    }

    override fun putBigVariable(key: String, value: String?) {
        RuleBigDataHelp.putBookVariable(bookUrl, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return RuleBigDataHelp.getBookVariable(bookUrl, key)
    }

    fun getKindList(): List<String> {
        val kindList = arrayListOf<String>()
        wordCount?.let {
            if (it.isNotBlank()) kindList.add(it)
        }
        kind?.let {
            val kinds = it.splitNotBlank(",", "\n")
            kindList.addAll(kinds)
        }
        return kindList
    }
}