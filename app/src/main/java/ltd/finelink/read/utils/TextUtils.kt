package ltd.finelink.read.utils

import java.util.regex.Pattern


object TextUtils {
    private val puncMap = mutableMapOf<String, String>("：" to ",",
        "；" to ",",
        ":" to ",",
        "，" to ",",
        "。" to ".",
        "！" to "!",
        "？" to "?",
        "\n" to ".",
        "·" to ",",
        "、" to ",",
        "..." to "…",
        "$" to ".",
        "“" to "",
        "”" to ",",
        "‘" to ",",
        "’" to ",",
        "（" to ",",
        "）" to ",",
        "(" to ",",
        ")" to ",",
        "《" to ",",
        "》" to ",",
        "【" to ",",
        "】" to ",",
        "[" to ",",
        "]" to ",",
        "—" to "-",
        "～" to "-",
        "~" to "-",
        "「" to ",",
        "」" to ",",
        "/" to "每",
        "①" to "一",
        "②" to "二",
        "③" to "三",
        "④" to "四",
        "⑤" to "五",
        "⑥" to "六",
        "⑦" to "七",
        "⑧" to "八",
        "⑨" to "九",
        "⑩" to "十",
        "α" to "阿尔法",
        "β" to "贝塔",
        "γ" to "伽玛",
        "Γ" to "伽玛",
        "δ" to "德尔塔",
        "Δ" to "德尔塔",
        "ε" to "艾普西龙",
        "ζ" to "捷塔",
        "η" to "依塔",
        "Θ" to "西塔",
        "ι" to "艾欧塔",
        "κ" to "喀帕",
        "λ" to "拉姆达",
        "Λ" to "拉姆达",
        "μ" to "缪",
        "ν" to "拗",
        "ξ" to "克西",
        "Ξ" to "克西",
        "ο" to "欧米克伦",
        "π" to "派",
        "Π" to "派",
        "ρ" to "肉",
        "ς" to "西格玛",
        "Σ" to "西格玛",
        "σ" to "西格玛",
        "τ" to "套",
        "υ" to "宇普西龙",
        "φ" to "服艾",
        "Φ" to "服艾",
        "χ" to "器",
        "ψ" to "普赛",
        "Ψ" to "普赛",
        "ω" to "欧米伽",
        "Ω" to "欧米伽")

    private val unitMap= mutableMapOf<Int, String>(1 to "十",2 to "百",3 to "千",4 to "万",8 to "亿")

    private val digitMap=mutableMapOf<String, String>(
        "0" to "零",
        "1" to "一",
        "2" to "二",
        "3" to "三",
        "4" to "四",
        "5" to "五",
        "6" to "六",
        "7" to "七",
        "8" to "八",
        "9" to "九",
    )

    private val measureMap=mutableMapOf<String, String>("cm2" to "平方厘米",
        "cm²" to "平方厘米",
        "cm3" to "立方厘米",
        "cm³" to "立方厘米",
        "cm" to "厘米",
        "db" to "分贝",
        "ds" to "毫秒",
        "kg" to "千克",
        "km" to "千米",
        "m2" to "平方米",
        "m²" to "平方米",
        "m³" to "立方米",
        "m3" to "立方米",
        "ml" to "毫升",
        "m" to "米",
        "mm" to "毫米",
        "s" to "秒")
    private var cPattern = Pattern.compile("(“[^”]*”)|(\"[^\"]*\")|(「[^」]*」)|(『[^』]*』)")
    private var sPattern = Pattern.compile("[!?…,'.]+")
    private var enPattern = Pattern.compile("[a-zA-Z]+([a-zA-Z!?…,.'\\s]+)*")
    private var cnPattern = Pattern.compile("[\\u4e00-\\u9fa5]+")
    private var iPattern = Pattern.compile("(-)(\\d+)");
    private var fPattern = Pattern.compile("(-?)(\\d+)/(\\d+)");
    private var pPattern = Pattern.compile("(-?)(\\d+(\\.\\d+)?)%");
    private var qPattern = Pattern.compile(
        "(\\d+)([多余几\\+])?(封|艘|把|目|套|段|人|所|朵|匹|张|座|回|场|尾|条|个|首|阙|阵|网|炮|顶|丘|棵|只|支|袭|辆|挑|担|颗|壳|窠|曲|墙|群|腔|砣|座|客|贯|扎|捆|刀|令|打|手|罗|坡|山|岭|江|溪|钟|队|单|双|对|出|口|头|脚|板|跳|枝|件|贴|针|线|管|名|位|身|堂|课|本|页|家|户|层|丝|毫|厘|分|钱|两|斤|担|铢|石|钧|锱|忽|(千|毫|微)克|毫|厘|(公)分|分|寸|尺|丈|里|寻|常|铺|程|(千|分|厘|毫|微)米|米|撮|勺|合|升|斗|石|盘|碗|碟|叠|桶|笼|盆|盒|杯|钟|斛|锅|簋|篮|盘|桶|罐|瓶|壶|卮|盏|箩|箱|煲|啖|袋|钵|年|月|日|季|刻|时|周|天|秒|分|小时|旬|纪|岁|世|更|夜|春|夏|秋|冬|代|伏|辈|丸|泡|粒|颗|幢|堆|条|根|支|道|面|片|张|颗|块|元|(亿|千万|百万|万|千|百)|(亿|千万|百万|万|千|百|美|)元|(亿|千万|百万|万|千|百|十|)吨|(亿|千万|百万|万|千|百|)块|角|毛|分)"
    );
    private var nPattern = Pattern.compile("(-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+))");
    private var rPattern = Pattern.compile(
        "((-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+)))(%|°C|℃|度|摄氏度|cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)[~]((-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+)))(%|°C|℃|度|摄氏度|cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)"
    );
    private var mPattern = Pattern.compile(
        "(-?)(\\d+(\\.\\d+)?)(cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)"
    );
    private var tPattern = Pattern.compile("(-?)(\\d+(\\.\\d+)?)(°C|℃|度|摄氏度)");
    private var dPattern = Pattern.compile("\\d{3}\\d*");
    private var decPattern = Pattern.compile("(-?)((\\d+)(\\.\\d+))|(\\.(\\d+))");
    private var datePattern = Pattern.compile("(\\d{4}|\\d{2})年((0?[1-9]|1[0-2])月)?(((0?[1-9])|((1|2)[0-9])|30|31)([日号]))?");
    private var date2Pattern = Pattern.compile("(\\d{4})([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])");
    private var timePattern= Pattern.compile("([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?");
    private var timeRPattern = Pattern.compile(
        "([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?(~|-)([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?"
    );
    private var phonePattern = Pattern.compile("(?<!\\d)((0(10|2[1-3]|[3-9]\\d{2})-?)?[1-9]\\d{6,7})(?!\\d)");
    private var mobilePattern = Pattern.compile("(?<!\\d)((\\+?86 ?)?1([38]\\d|5[0-35-9]|7[678]|9[89])\\d{8})(?!\\d)")



    fun textNormalizer(text: String): String {
        val normText = convertAn2Cn(text)
        return removeSpecial(replaceByMap(normText, puncMap))
    }

    private fun replaceMeasure(text: String): String {
        var matcher = mPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val measure = matcher.group(2)
            val unit = matcher.group(3)
            var value = ""
            if (sign != null&&sign.isNotEmpty()) {
                value = "负"
            }
            value += verbaizeDecimal(measure)
            value += measureMap[unit]
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceByMap(text: String, map: Map<String, String>?): String {
        val sb = StringBuffer(text)
        for (key in map!!.keys) {
            var index = sb.indexOf(key)
            val value = map!![key]
            while (index != -1) {
                sb.replace(index, index + key.length, value)
                index += value!!.length // Move to the end of the replacement
                index = sb.indexOf(key, index)
            }
        }
        return sb.toString()
    }

    fun spiltSentences(text: String, maxLength: Int,minSentence:Boolean=false): List<String> {
        val sentences: MutableList<String> = ArrayList()
        if(text.length<=maxLength&&!minSentence){
            sentences.add(text)
            return sentences
        }
        val texts = text.split("[：、，；。？！,;?.!”’「」『』\"]".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        var offset = 0
        var sb = StringBuffer()
        for (t in texts) {
            if (t.isEmpty()) {
                val symbol = text.substring(offset, offset + 1)
                sb.append(symbol)
                offset++
                continue
            }
            if (sb.isNotEmpty()) {
                if (sb.length + t.trim().length > maxLength) {
                    sentences.add(sb.toString())
                    sb.setLength(0)
                }
            }
            offset += t.length
            if (offset < text.length) {
                val symbol = text.substring(offset, offset + 1)
                val sentence = t.trim() + symbol
                sb.append(sentence)
                offset++
                if(minSentence&&symbol in "？！。?.!"&&t.length>2){
                    sentences.add(sb.toString())
                    sb.setLength(0)
                }
                continue
            }
            sb.append(t)
        }
        sentences.add(sb.toString())
        return sentences
    }


    fun spiltDialogue(content: String, maxLength: Int=30): List<String> {
        val sentences: MutableList<String> = ArrayList()
        var start = 0
        val matcher = cPattern.matcher(content)
        while (matcher.find()) {
            val dialogue = matcher.group()
            val text = content.substring(start, matcher.start())
            if (text.isNotBlank()) {
                sentences.addAll(spiltSentences(text,maxLength))
            }
            sentences.add(dialogue)
            start = matcher.end()
        }
        if (start < content.length) {
            val text = content.substring(start, content.length)
            if (text.isNotBlank()) {
                sentences.addAll(spiltSentences(text,maxLength))
            }
        }

        return sentences
    }

    fun findDialogue(content: String): List<String> {
        val sentences: MutableList<String> = ArrayList()
        val matcher = cPattern.matcher(content)
        while (matcher.find()) {
            val dialogue = matcher.group()
            sentences.add(dialogue)
        }
        return sentences
    }

    fun convertAn2Cn(text: String): String {
        var text = text
        text = replaceDate(text)
        text = replaceDate2(text)
        text = replaceTimeRange(text)
        text = replaceTime(text)
        text = replaceRange(text)
        text = replaceTemperature(text)
        text = replaceMeasure(text)
        text = replaceFrac(text)
        text = replacePercentage(text)
        text = replaceMobile(text)
        text = replacePhone(text)
        text = replaceInteger(text)
        text = replaceQuantifier(text)
        text = replaceDecimalNumber(text)
        text = replaceDefalutNumber(text)
        text = replaceNumber(text)
        return text.replace("嗯", "恩").replace("呣", "母")
    }
    private fun removeSpecial(text: String): String {

        return text.replace("[^\\u0800-\\u9fa5_a-zA-Z\\s!?…,.:'\\d-]+".toRegex(), "")
    }
    fun replaceFrac(text: String): String {
        var matcher = fPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val nominator = matcher.group(2)
            val denominator = matcher.group(3)
            var value = verbalizeNumber(denominator) + "分之" + verbalizeNumber(nominator)
            if (sign!=null&&sign.isNotEmpty()) {
                value = "负$value"
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replacePercentage(text: String): String {
        var matcher = pPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val percent = matcher.group(2)
            var value = "百分之" + verbalizeNumber(percent)
            if (sign!=null&&sign.isNotEmpty()) {
                value = "负$value"
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceMobile(text: String): String {
        var matcher = mobilePattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val number = matcher.group(0)
            matcher =
                matcher.appendReplacement(sb, covertAltOne(verbalizeDigit(number.replace("+", ""))))
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replacePhone(text: String): String {
        var matcher = phonePattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val number = matcher.group(0)
            matcher =
                matcher.appendReplacement(sb, covertAltOne(verbalizeDigit(number.replace("-", ""))))
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceInteger(text: String): String {
        var matcher = iPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val number = matcher.group(2)
            var value = verbalizeNumber(number)
            if (sign != null&&sign.isNotEmpty()) {
                value = "负$value"
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceNumber(text: String): String {
        var matcher = nPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val number = matcher.group(2)
            val decimal = matcher.group(5)
            var value = ""
            value = decimal?.let { verbaizeDecimal(it) } ?: verbalizeNumber(number)
            if (sign != null&&sign.isNotEmpty()) {
                value = "负$value"
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceDecimalNumber(text: String): String {
        var matcher = decPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val decimal = matcher.group(5)
            var value = ""
            value = verbaizeDecimal(decimal)
            if (sign != null&&sign.isNotEmpty()) {
                value = "负$value"
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceDefalutNumber(text: String): String {
        var matcher = dPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val number = matcher.group(0)
            matcher = matcher.appendReplacement(sb, verbalizeDigit(number))
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceQuantifier(text: String): String {
        var matcher = qPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val number = matcher.group(1)
            val match = matcher.group(2)
            val quantifiers = matcher.group(3)
            var value = ""
            if (match == "+") {
                value = "多"
            }
            value = verbaizeDecimal(number) + value + quantifiers
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceTemperature(text: String): String {
        var matcher = tPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val temperature = matcher.group(2)
            val unit = matcher.group(3)
            var value = ""
            if (sign != null&&sign.isNotEmpty()) {
                value = "零下"
            }
            value += verbaizeDecimal(temperature)
            value += if (unit != "摄氏度") {
                "度"
            } else {
                unit
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceDate(text: String): String {
        var matcher = datePattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val year = matcher.group(1)
            val month = matcher.group(3)
            val day = matcher.group(5)
            var value = ""
            if (year != null) {
                value += verbalizeDigit(year) + "年"
            }
            if (month != null) {
                value += verbalizeNumber(month) + "月"
            }
            if (day != null) {
                value += verbalizeNumber(day) + matcher.group(9)
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceDate2(text: String): String {
        var matcher = date2Pattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val year = matcher.group(1)
            val month = matcher.group(3)
            val day = matcher.group(4)
            var value = ""
            if (year != null) {
                value += verbalizeDigit(year) + "年"
            }
            if (month != null) {
                value += verbalizeNumber(month) + "月"
            }
            if (day != null) {
                value += verbalizeNumber(day) + matcher.group(9)
            }
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceTime(text: String): String {
        var matcher = timePattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val hour = matcher.group(1)
            val minute = matcher.group(2)
            val second = matcher.group(4)
            val value = converTime(hour, minute, second)
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceTimeRange(text: String): String {
        var matcher = timeRPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val hour = matcher.group(1)
            val minute = matcher.group(2)
            val second = matcher.group(4)
            val hour2 = matcher.group(6)
            val minute2 = matcher.group(7)
            val second2 = matcher.group(9)
            var value = converTime(hour, minute, second)
            value += "至" + converTime(hour2, minute2, second2)
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    fun replaceRange(text: String): String {
        var matcher = rPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val value = matcher.group(0).replace("~", "致")
            matcher = matcher.appendReplacement(sb, value)
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString()
        }
        return text
    }

    private fun converTime(hour: String, minute: String, second: String?): String {
        val sb = StringBuffer()
        sb.append(verbalizeNumber(hour) + "点")
        sb.append(verbalizeNumber(minute) + "分")
        if (second != null) {
            sb.append(verbalizeNumber(second) + "秒")
        }
        return sb.toString()
    }

    fun verbaizeDecimal(number: String): String {
        val numbers = number.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val sb = StringBuffer()
        for (n in numbers) {
            if (sb.isEmpty()) {
                sb.append(verbalizeNumber(n))
            } else {
                sb.append("点" + verbalizeDigit(n))
            }
        }
        return if (sb.isNotEmpty()) {
            sb.toString()
        } else number
    }

    fun verbalizeNumber(number: String): String {
        val sb = StringBuffer()
        for (i in number.indices) {
            val c = number[i]
            val n = c.toString()
            if (sb.isNotEmpty() && n == "0") {
                if (sb.lastIndexOf("零") != sb.length - 1) {
                    sb.append(digitMap!![n])
                }
            } else {
                sb.append(digitMap!![n])
            }
            val unit = getNumberUnit(number.length - i - 1)
            if (unit != null) {
                if (unit == "亿") {
                    if (sb.lastIndexOf("零") == sb.length - 1) {
                        sb.replace(sb.length - 1, sb.length, unit)
                    } else {
                        sb.append(unit)
                    }
                } else if (n != "0") {
                    sb.append(unit)
                }
            }
        }
        var result = sb.toString()
        result = result.replace("零零", "零").replace("零零", "零")
        if (result.endsWith("零") && result.length > 1) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    private fun getNumberUnit(number: Int): String? {
        var unit: String? = ""
        unit = if (number > 8) {
            getNumberUnit(number - 8)
        } else if (number in 5..7) {
            getNumberUnit(number - 4)
        } else {
            unitMap!![number]
        }
        return unit
    }

    fun verbalizeDigit(number: String): String {
        val sb = StringBuffer()
        for (element in number) {
            val c = element
            sb.append(digitMap!![c.toString()])
        }
        return sb.toString()
    }

    fun covertAltOne(number: String): String {
        return number.replace("一", "幺")
    }

    fun separateLetter(text: String): List<String> {
        val texts: MutableList<String> = ArrayList()
        val matcher = enPattern.matcher(text)
        var index = 0
        while (matcher.find(index)) {
            if (matcher.start() > index) {
                texts.add(text.substring(index, matcher.start()))
            }
            texts.add(formatText(matcher.group()))
            index = matcher.end()
            if (index == text.length) {
                break
            }
        }
        if (index < text.length) {
            texts.add(text.substring(index))
        }
        return texts
    }

    private fun formatText(text: String): String {
        val sb = StringBuffer()
        var matcher = sPattern.matcher(text)
        while (matcher.find()) {
            val value: String = matcher.group(0)
            matcher = matcher.appendReplacement(sb, " $value ")
        }
        if (sb.isNotEmpty()) {
            matcher.appendTail(sb)
            return sb.toString().trim { it <= ' ' }.replace("  ", " ")
        }
        return text
    }

    fun hasEnglish(text: String?): Boolean {
        val matcher= enPattern.matcher(text)
        return matcher.find()
    }

    fun hasChinese(text: String?): Boolean {
        val matcher= cnPattern.matcher(text)
        return matcher.find()
    }


}