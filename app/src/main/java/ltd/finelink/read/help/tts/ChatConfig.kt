package ltd.finelink.read.help.tts

import ltd.finelink.read.base.Tokenizer
import ltd.finelink.read.utils.EnTextUtils
import ltd.finelink.read.utils.TextUtils
import org.pytorch.Tensor


class ChatConfig @JvmOverloads constructor(
    private val bertTokenMap: Map<String, Int>,
    val sampleRate: Int = 24000,
) {


    private val tokenizer : Tokenizer by lazy{
        Tokenizer(bertTokenMap)
    }
    private val symbols= mutableMapOf<String, String>(
        "…" to "...",
        "," to "，",
        "." to "。",
        "!" to "！",
        "?" to "？"
    )


    fun getInput(text: String): Array<Tensor> {
        var tokens = tokenizer.toTokens(text)
        val shape = longArrayOf(1, (tokens.size).toLong())
        val inputIds = IntArray(tokens.size)
        val attentionMasks = IntArray(tokens.size)
        for(i in tokens.indices){
            inputIds[i] = tokens[i].id!!
            attentionMasks[i] = 1
        }

        return arrayOf(
            Tensor.fromBlob(inputIds, shape),
            Tensor.fromBlob(attentionMasks, shape)
        )
    }

    fun textNormalizer(text: String,lang: String="zh"):String {
        val normText = when(lang){
            "zh" -> TextUtils.replaceByMap(TextUtils.textNormalizer(text).trim(),symbols)
            else -> EnTextUtils.textNormalizer(text).trim()
        }
        return normText
    }

    fun convertTokensToText(tokens: LongArray, lang: String="zh"):String {
        val texts = tokenizer.batchConvertByIds(tokens)
        var sb = StringBuffer()
        for(text in texts){
            sb.append(text)
            if(lang=="en"){
                sb.append(" ")
            }

        }
        return sb.toString().trim()
    }

    fun prepareRefineText(text: String,prompt:String="[oral_2][laugh_0][break_6]"):String{
        return "[Sbreak]$text[Pbreak]$prompt"
    }

    fun prepareGenerate(text: String,prompt:String="[speed_4]",spkPrompt:String?=null):String{
        spkPrompt?.let {
            return "[Stts][empty_spk]$it$prompt$text[Ptts]"
        }
        return "[Stts][spk_emb]$prompt$text[Ptts]"
    }


}
