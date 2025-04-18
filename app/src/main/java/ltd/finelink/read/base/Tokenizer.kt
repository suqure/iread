package ltd.finelink.read.base

import java.util.Arrays
import java.util.regex.Pattern
import kotlin.math.min


class Tokenizer(vocab: Map<String, Int>?) {
    private var vocab: Map<String, Int> = HashMap()
    private val idMap: MutableMap<Int?, String> = HashMap()
    private val tokens: MutableMap<String, MutableList<String>> = HashMap()
    private var MAX_TOKEN_LEN = 1
    private val UNK = "[UNK]"
    private val TOKEN_PREFIX = "##"
    private val spPattern = Pattern.compile("^(\\[[a-zA-Z_\\d]{1,10}\\])")

    init {
        if (!vocab.isNullOrEmpty()) {
            this.vocab = vocab
            init()
        }
    }

    private fun init() {
        val keySet = vocab.keys
        val keyArray = keySet.toTypedArray<String>()
        Arrays.sort(keyArray)
        var pre = ""
        for (k in keyArray) {
            idMap[vocab[k]] = k
            if (k.length > MAX_TOKEN_LEN) {
                MAX_TOKEN_LEN = k.length
            }
            if (isNotSpacial(k)) {
                if (pre.isEmpty() || !k.contains(pre)) {
                    pre = k
                } else {
                    var next = tokens[pre]
                    if (next == null) {
                        next = ArrayList()
                        tokens[pre] = next
                    }
                    next.add(k)
                }
            }
        }
    }

    fun toTokens(text: String): List<BertToken> {
        val tokens: MutableList<BertToken> = ArrayList<BertToken>()
        for (t in spiltSymbol(text)) {
            for (word in t.split("[ 	]".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()) {
                tokens.addAll(parseToken(word, true))
            }
        }
        return tokens
    }

    private fun splitToken(text: String, first: Boolean): BertToken {
        val matcher = spPattern.matcher(text)
        val token = BertToken()
        if (matcher.find()) {
            val sp = matcher.group(1)
            token.id = vocab[sp]
            if(token.id!=null){
                token.source = sp
                token.token = sp
                token.special = true
                return token
            }

        }
        val size = min(text.length.toDouble(), MAX_TOKEN_LEN.toDouble()).toInt()
        token.source = text
        token.token =UNK
        token.id = vocab[UNK]
        if (!first && text.matches("[a-zA-Z]+.*".toRegex())) {
            for (i in 0 until size) {
                val value = TOKEN_PREFIX + text.substring(0, size - i)
                if (handleToken(TOKEN_PREFIX + text, value, token)) {
                    return token
                }
            }

        }
        for (i in 0 until size) {
            val value = text.substring(0, i + 1)
            if (handleToken(text, value, token)) {
                return token
            }
        }
        return token
    }
    private fun handleToken(text: String, value: String, token: BertToken): Boolean {
        val id = vocab[value.lowercase()]
        if (id != null) {
            token.id = id
            token.token = value
            if (value.startsWith(TOKEN_PREFIX)) {
                token.source = value.substring(2)
            } else {
                token.source = value
            }
            val keys: List<String>? = tokens[value.lowercase()]
            if (keys != null) {
                for (key in keys) {
                    if (text.startsWith(key,true)) {
                        token.id=vocab[key]
                        token.token = key
                        if (value.startsWith(TOKEN_PREFIX)) {
                            token.source = key.substring(2)
                        } else {
                            token.source = key
                        }
                    }
                }
            }
            return true
        }
        return false
    }

    fun getTokenId(token: String): Int? {
        return vocab[token]
    }

    private fun parseToken(text: String, first: Boolean): List<BertToken> {
        val tokens: MutableList<BertToken> = ArrayList<BertToken>()
        val token: BertToken = splitToken(text, first)
        tokens.add(token)
        if (token.source.equals(text,true)) {
            return tokens
        }
        tokens.addAll(parseToken(text.substring(token.source?.length!!), token.special))
        return tokens
    }

    fun covertById(id:Int):String{
        return idMap[id]?:""
    }

    fun batchConvertByIds(ids:LongArray):List<String>{
        val list :MutableList<String> = ArrayList<String>()
        for(id in ids){
            list.add(covertById(id.toInt()))
        }
        return list
    }



    private fun spiltSymbol(text: String): List<String> {
        val sentences: MutableList<String> = ArrayList()
        val sentence = text.trim { it <= ' ' }
        if (sentence.isNotEmpty()) {
            val texts = text.trim { it <= ' ' }.split("[：、，；。？！,;?.!”’']".toRegex())
                .dropLastWhile { it.isEmpty() }
                .toTypedArray()
            var offset = 0
            for (t in texts) {
                if (t.isEmpty()) {
                    val symbol = sentence.substring(offset, offset + 1)
                    sentences.add(symbol)
                    offset++
                    continue
                }
                sentences.add(t.trim { it <= ' ' })
                offset += t.length
                if (offset < sentence.length) {
                    val symbol = sentence.substring(offset, offset + 1)
                    sentences.add(symbol)
                    offset++
                }
            }
        }
        return sentences
    }

    private fun isNotSpacial(token: String): Boolean {
        return !(token.startsWith("[") && token.endsWith("]"))
    }
}
