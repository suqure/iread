package ltd.finelink.read.help.tts

import com.github.houbb.pinyin.constant.enums.PinyinStyleEnum
import com.github.houbb.pinyin.util.PinyinHelper
import dev.esnault.wanakana.core.Wanakana
import ltd.finelink.read.utils.EnTextUtils
import ltd.finelink.read.utils.TextUtils
import org.pytorch.Tensor
import java.util.Locale

class T2sConfig @JvmOverloads constructor(
    private val symbolMap: Map<String, Int>,
    private val bertTokenMap: Map<String, Int>,
    private val pinyinMap: Map<String, String>,
    private val phoneticMap: Map<String, String>,
    val sampleRate: Int = 44100,
    private val addBlank: Boolean = false,
) {
    private val punctuation = "!?…,'.-:|_"
    private val symbols = mutableMapOf<String, String>(
        "uu" to "U",
        "aa" to "AA",
        "ee" to "EE",
        "oo" to "OO",
        "by" to "by",
        "dy" to "dy",
        "gy" to "gy",
        "hy" to "hy",
        "ky" to "ky",
        "my" to "my",
        "ny" to "ny",
        "py" to "py",
        "ry" to "ry",
        "ch" to "ch",
        "sh" to "sh",
        "ts" to "ts",
    )

    fun getBertInput(text: String): Array<Tensor> {
        val shape = longArrayOf(1, (text.length + 2).toLong())
        val inputIds = IntArray(text.length + 2)
        inputIds[0] = bertTokenMap["[CLS]"]!!
        inputIds[inputIds.size - 1] = bertTokenMap["[SEP]"]!!
        val tokenTypes = IntArray(text.length + 2)
        tokenTypes[0] = 0
        tokenTypes[inputIds.size - 1] = 0
        val attentionMasks = IntArray(text.length + 2)
        attentionMasks[0] = 1
        attentionMasks[inputIds.size - 1] = 1
        for (i in text.indices) {
            val c = text[i]
            val key = c.toString()
            val id = bertTokenMap[key]
            if (id != null) {
                inputIds[i + 1] = id
            } else {
                inputIds[i + 1] = bertTokenMap["[UNK]"]!!
            }
            tokenTypes[i + 1] = 0
            attentionMasks[i + 1] = 1
        }
        return arrayOf<Tensor>(
            Tensor.fromBlob(inputIds, shape),
            Tensor.fromBlob(tokenTypes, shape),
            Tensor.fromBlob(attentionMasks, shape)
        )
    }

    fun getTtsInferTensor(g2p: G2PData): Array<Tensor> {
        val seq = mutableListOf<Int?>()
        val langIds = mutableListOf<Int?>()
        for (phone in g2p.phoneList) {
            seq.add(symbolMap[phone])
            if(g2p.lang=="zh"){
                langIds.add(0)
            }else if(g2p.lang=="jp"){
                langIds.add(1)
            }else if(g2p.lang=="kr"){
                langIds.add(4)
            }else{
                langIds.add(2)
            }
        }
        if (addBlank) {
            addBlank(seq)
            addBlank(langIds)
            addBlank(g2p.tonesList)
            for (i in g2p.word2ph.indices) {
                val value = g2p.word2ph[i]
                g2p.word2ph[i] = value!! * 2
            }
            g2p.word2ph[0] = g2p.word2ph[0]!! + 1
        }
        val shape = longArrayOf(seq.size.toLong())
        val tonesShape = longArrayOf(g2p.tonesList.size.toLong())
        val langShape = longArrayOf(langIds.size.toLong())
        val word2phShape = longArrayOf(g2p.word2ph.size.toLong())
        return arrayOf<Tensor>(
            Tensor.fromBlob(toArray(seq), shape),
            Tensor.fromBlob(toArray(g2p.tonesList), tonesShape),
            Tensor.fromBlob(toArray(langIds), langShape),
            Tensor.fromBlob(toArray(g2p.word2ph), word2phShape)
        )
    }

    fun getInferBert(bert: Tensor): Array<Tensor> {
        val seq: MutableList<Int> = ArrayList()
        var size = when (bert.shape()[0]) {
            1024L -> 768L
            else -> 1024L
        }
        for (i in 0 until bert.shape()[1] * size) {
            seq.add(0)
        }
        val bertShape = longArrayOf(size, bert.shape()[1])
        var jaBert = Tensor.fromBlob(toArray(seq), bertShape)
        if (bert.shape()[0] > jaBert.shape()[0]) {
            return arrayOf<Tensor>(bert, jaBert)
        } else {
            return arrayOf<Tensor>(jaBert, bert)
        }
    }

    fun getEmptyBert(size:Long): Array<Tensor> {
        val seq: MutableList<Int> = ArrayList()
        val ja: MutableList<Int> = ArrayList()
        for (i in 0 until 1024 * size) {
            seq.add(0)
        }
        for (i in 0 until 768 * size) {
            ja.add(0)
        }
        val bertShape = longArrayOf(1024,size)
        val jaShape = longArrayOf(768,size)
        var bert = Tensor.fromBlob(toArray(seq), bertShape)
        var jaBert = Tensor.fromBlob(toArray(ja), jaShape)
        return arrayOf<Tensor>(bert, jaBert)
    }


    private fun toArray(data: List<Int?>): IntArray {
        val result = IntArray(data.size)
        for (i in data.indices) {
            result[i] = data[i]!!
        }
        return result
    }

    private fun addBlank(data: MutableList<Int?>) {
        val blanks = data.size + 1
        for (i in 0 until blanks) {
            data.add(2 * i, 0)
        }
    }

    fun g2pMix(text: String): G2PData {
        val normText: String = TextUtils.textNormalizer(text).trim()
        return parseG2p(normText,"zh")
    }

    fun g2pEnMix(text: String): G2PData {
        val normText: String = EnTextUtils.textNormalizer(text).trim()
        return parseG2p(normText,"en")
    }





    private fun parseG2p(normText: String, lang: String = "zh"): G2PData {
        val g2p = G2PData()
        var sb = StringBuffer()
        for (seg in TextUtils.separateLetter(normText)) {
            if (seg.isEmpty()) {
                continue
            }
            if (TextUtils.hasEnglish(seg)) {
                sb.append(handleEnglish(seg,g2p))
            }else if (lang=="zh") {
                sb.append(handleChinese(seg,g2p))
            }
        }
        g2p.lang = lang
        g2p.text = sb.toString().trim()
        g2p.phoneList.add(0, "_")
        g2p.phoneList.add("_")
        g2p.tonesList.add(0, 0)
        g2p.tonesList.add(0)
        g2p.word2ph.add(0, 1)
        g2p.word2ph.add(1)
        return g2p
    }

    private fun handleEnglish(seg:String,g2p:G2PData):String{
        var phonetics = parseToPhonetic(seg)
        var phonetic: String = phonetics[1]
        for (phone in phonetic.split("  ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            if (!punctuation.contains(phone)) {
                var wh = 0
                for (p in phone.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()) {
                    if (p.matches("[a-zA-Z]+\\d$".toRegex())) {
                        var tone = p.substring(p.length - 1)
                        g2p.tonesList.add(tone.toInt())
                        g2p.phoneList.add(p.substring(0, p.length - 1).lowercase())
                    } else {
                        g2p.tonesList.add(0)
                        g2p.phoneList.add(p.lowercase())
                    }
                    wh++
                }
                g2p.word2ph.add(wh)
            } else {
                g2p.word2ph.add(1)
                g2p.tonesList.add(0)
                g2p.phoneList.add(phone)
            }
        }
        return phonetics[0]
    }

    private fun handleChinese(seg:String,g2p:G2PData):String{
        val pinyin: String =
            PinyinHelper.toPinyin(seg, PinyinStyleEnum.NUM_LAST).replace("ü", "v");
        for (p in pinyin.split(" ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()) {
            if(p.trim().isEmpty()){
                continue
            }
            if (!punctuation.contains(p)) {
                val data = transferPinyin(p)
                g2p.tonesList.add(data[2].toInt())
                g2p.tonesList.add(data[2].toInt())
                g2p.phoneList.add(data[0])
                g2p.phoneList.add(data[1])
                g2p.word2ph.add(2)
            } else {
                g2p.word2ph.add(1)
                g2p.tonesList.add(0)
                g2p.phoneList.add(p)
            }
        }
        return seg
    }


    private fun parseToPhonetic(text: String): Array<String> {
        val phonetic = StringBuffer()
        val words = StringBuffer()
        for (word in text.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            var phone = phoneticMap[word.uppercase(Locale.getDefault())]
            if (phone != null) {
                phonetic.append("$phone  ")
                words.append("$word ")
            } else {
                if (word.length > 1) {
                    for (w in word.split("\\s*".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()) {
                        if (w.isNotEmpty()) {
                            phone = phoneticMap[w.uppercase(Locale.getDefault())]
                            if (phone != null) {
                                phonetic.append("$phone  ")
                            } else {
                                phonetic.append("$w  ")
                            }
                            words.append("$w ")
                        }
                    }
                } else {
                    phonetic.append("$word  ")
                    words.append("$word ")
                }
            }
        }
        return arrayOf<String>(words.toString().trim(), phonetic.toString().trim())
    }


    private fun transferPinyin(pinyin: String): Array<String> {
        val tone = pinyin.substring(pinyin.length - 1)
        val py = pinyin.substring(0, pinyin.length - 1)
        val value =
            pinyinMap[py]!!.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return arrayOf(value[0], value[1], tone)
    }

}
