package ltd.finelink.read.utils

import java.text.DecimalFormat
import java.util.regex.Pattern


object EnTextUtils {
    private val puncMap = mutableMapOf<String, String>(
        "：" to ",",
        ":" to ",",
        "；" to ",",
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
        "/" to " per",
        "①" to " one",
        "②" to " two",
        "③" to " three",
        "④" to " four",
        "⑤" to " five",
        "⑥" to " six",
        "⑦" to " seven",
        "⑧" to " eight",
        "⑨" to " night",
        "⑩" to " ten",
        "α" to " alpha",
        "β" to " beta",
        "γ" to " gamma",
        "Γ" to " gamma",
        "δ" to " delta",
        "Δ" to " delta",
        "ε" to " epsilon",
        "ζ" to " zeta",
        "η" to " etah",
        "Θ" to " theta",
        "ι" to " iota",
        "κ" to " kappa",
        "λ" to " lambda",
        "Λ" to " lambda",
        "μ" to " mu",
        "ν" to " nu",
        "ξ" to " xi",
        "Ξ" to " xi",
        "ο" to " omicron",
        "π" to " pi",
        "Π" to " pi",
        "ρ" to " Rho",
        "ς" to " sigma",
        "Σ" to " sigma",
        "σ" to " sigma",
        "τ" to " tau",
        "υ" to " nu",
        "φ" to " phi",
        "Φ" to " phi",
        "χ" to " chi",
        "ψ" to " psi",
        "Ψ" to " psi",
        "ω" to " omega",
        "Ω" to " omega")


    private val digitMap=mutableMapOf<String, String>(
        "0" to " zero",
        "1" to " one",
        "2" to " two",
        "3" to " three",
        "4" to " four",
        "5" to " five",
        "6" to " six",
        "7" to " seven",
        "8" to " eight",
        "9" to " night",
    )
    private val tensNames = arrayOf(
        "", " ten", " twenty", " thirty", " forty", " fifty", " sixty",
        " seventy", " eighty", " ninety"
    )

    private val numNames = arrayOf(
        "",
        " one",
        " two",
        " three",
        " four",
        " five",
        " six",
        " seven",
        " eight",
        " nine",
        " ten",
        " eleven",
        " twelve",
        " thirteen",
        " fourteen",
        " fifteen",
        " sixteen",
        " seventeen",
        " eighteen",
        " nineteen"
    )
    private val monthNames = arrayOf(
        " January", " February", " March", " April", " May", " June", " July", " August",
        " September", " October", " November", " December"
    )
    private val measureMap=mutableMapOf<String, String>("cm2" to " square centimetre",
        "cm²" to " square centimetre ",
        "cm3" to " cubic centimeter ",
        "cm³" to " cubic centimeter ",
        "cm" to " centimetre ",
        "db" to " decibel ",
        "ds" to " millisecond ",
        "kg" to " kilogram ",
        "km" to " kilometer ",
        "m2" to " square meter ",
        "m²" to " square meter ",
        "m³" to " cubic meter ",
        "m3" to " cubic meter ",
        "ml" to " milliliter ",
        "m" to " meter ",
        "mm" to " millimeter ",
        "s" to " second ")
    private var sPattern = Pattern.compile("[!?…,'.]+")
    private var enPattern = Pattern.compile("[a-zA-Z]+([a-zA-Z!?…,.'\\s]+)*")
    private var iPattern = Pattern.compile("(-)(\\d+)");
    private var fPattern = Pattern.compile("(-?)(\\d+)/(\\d+)");
    private var pPattern = Pattern.compile("(-?)(\\d+(\\.\\d+)?)%");

    private var nPattern = Pattern.compile("(-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+))");
    private var rPattern = Pattern.compile(
        "((-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+)))(%|°C|℃|cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)[~]((-?)((\\d+)(\\.\\d+)?)|(\\.(\\d+)))(%|°C|℃|cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)"
    );
    private var mPattern = Pattern.compile(
        "(-?)(\\d+(\\.\\d+)?)(cm2|cm²|cm3|cm³|cm|db|ds|kg|km|m2|m²|m³|m3|ml|m|mm|s)"
    );
    private var tPattern = Pattern.compile("(-?)(\\d+(\\.\\d+)?)(°C|℃)");
    private var dPattern = Pattern.compile("\\d{3}\\d*");
    private var decPattern = Pattern.compile("(-?)((\\d+)(\\.\\d+))|(\\.(\\d+))");
    private var date2Pattern = Pattern.compile("(\\d{4})([- /.])(0[1-9]|1[012])\\2(0[1-9]|[12][0-9]|3[01])");
    private var timePattern= Pattern.compile("([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?");
    private var timeRPattern = Pattern.compile(
        "([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?(~|-)([0-1]?[0-9]|2[0-3]):([0-5][0-9])(:([0-5][0-9]))?"
    );
    private var phonePattern = Pattern.compile("(?<!\\d)((0(10|2[1-3]|[3-9]\\d{2})-?)?[1-9]\\d{6,7})(?!\\d)");
    private var mobilePattern = Pattern.compile("(?<!\\d)((\\+?86 ?)?1([38]\\d|5[0-35-9]|7[678]|9[89])\\d{8})(?!\\d)")



    fun textNormalizer(text: String): String {
        val normText = convertAn2En(text)
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
                value = " minus"
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

    private fun replaceByMap(text: String, map: Map<String, String>?): String {
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

    fun convertAn2En(text: String): String {
        var text = text
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
        text = replaceDecimalNumber(text)
        text = replaceDefalutNumber(text)
        text = replaceNumber(text)
        return text.replace("  ", " ").replace("  ", " ");
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
            var value = verbalizeNumber(denominator) + " " + verbalizeNumber(nominator)
            if (sign!=null&&sign.isNotEmpty()) {
                value = " minus $value"
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
            var value = verbalizeNumber(percent)+" percent"
            if (sign!=null&&sign.isNotEmpty()) {
                value = " minus $value"
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
                matcher.appendReplacement(sb, verbalizeDigit(number.replace("+", "")))
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
                matcher.appendReplacement(sb, verbalizeDigit(number.replace("-", "")))
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
                value = " minus $value"
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
            val decimal = matcher.group(4)
            var value = ""
            value = decimal?.let { verbaizeDecimal(number) } ?: verbalizeNumber(number)
            if (sign != null&&sign.isNotEmpty()) {
                value = " minus $value"
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
                value = " minus $value"
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



    fun replaceTemperature(text: String): String {
        var matcher = tPattern!!.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val sign = matcher.group(1)
            val temperature = matcher.group(2)
            val unit = matcher.group(3)
            var value = ""
            if (sign != null&&sign.isNotEmpty()) {
                value = " minus"
            }
            value += verbaizeDecimal(temperature)
            value += unit
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
                value += verbalizeDigit(year) + " year"
            }
            if (month != null) {
                value += monthNames[Integer.valueOf(month)]
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
            value += " between" + converTime(hour2, minute2, second2)
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
            val value = matcher.group(0).replace("~", " between ")
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
        sb.append(verbalizeNumber(hour) + " hour")
        sb.append(verbalizeNumber(minute) + " minute")
        if (second != null) {
            sb.append(verbalizeNumber(second) + " second")
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
                sb.append(" point" + verbalizeDigit(n))
            }
        }
        return if (sb.isNotEmpty()) {
            sb.toString()
        } else number
    }

    fun verbalizeNumber(number: String): String {
        val num = number.toLong()
        if (num == 0L) {
            return "zero"
        }
        val mask = "000000000000"
        val df = DecimalFormat(mask)
        val snumber: String = df.format(num)
        val billions = snumber.substring(0, 3).toInt()
        val millions = snumber.substring(3, 6).toInt()
        val hundredThousands = snumber.substring(6, 9).toInt()
        val thousands = snumber.substring(9, 12).toInt()
        val tradBillions: String = when (billions) {
            0 -> ""
            1 -> convertLessThanOneThousand(billions) + " billion "
            else -> convertLessThanOneThousand(billions) + " billion "
        }
        var result = tradBillions
        val tradMillions: String = when (millions) {
            0 -> ""
            1 -> convertLessThanOneThousand(millions) + " million "
            else -> convertLessThanOneThousand(millions) + " million "
        }
        result += tradMillions
        val tradHundredThousands: String = when (hundredThousands) {
            0 -> ""
            1 -> "one thousand "
            else -> convertLessThanOneThousand(hundredThousands) + " thousand "
        }
        result += tradHundredThousands
        val tradThousand: String = convertLessThanOneThousand(thousands)
        result += tradThousand
        return result.replace("^\\s+".toRegex(), "").replace("\\b\\s{2,}\\b".toRegex(), " ")
    }

    fun verbalizeDigit(number: String): String {
        val sb = StringBuffer()
        for (element in number) {
            val c = element
            sb.append(digitMap!![c.toString()])
        }
        return sb.toString()
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


    private fun convertLessThanOneThousand(number: Int): String {
        var number = number
        var soFar: String
        if (number % 100 < 20) {
            soFar = numNames[number % 100]
            number /= 100
        } else {
            soFar = numNames[number % 10]
            number /= 10
            soFar = tensNames[number % 10] + soFar
            number /= 10
        }
        return if (number == 0) soFar else numNames[number] + " hundred" + soFar
    }

}