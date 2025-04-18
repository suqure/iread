package ltd.finelink.read.help.tts

import ltd.finelink.read.help.tiktoken.internal.CoreBPE
import ltd.finelink.read.help.tiktoken.internal.TokenEncoder
import ltd.finelink.read.utils.EnTextUtils
import ltd.finelink.read.utils.TextUtils
import okio.ByteString
import org.pytorch.Tensor


class FishConfig @JvmOverloads constructor(
    private val ranks: Map<ByteString, Int>,
    private val specialTokens: Map<ByteString, Int>,
    val sampleRate: Int = 44100,
) {


    val pattern: Regex = Regex("""'s|'t|'re|'ve|'m|'ll|'d| ?\p{L}+| ?\p{N}+| ?[^\s\p{L}\p{N}]+|\s+(?!\S)|\s+""")

    private val tokenizer : ltd.finelink.read.help.tiktoken.Tokenizer by lazy{
        val coreBPE = CoreBPE.create(
            encoder = ranks,
            specialTokensEncoder = specialTokens,
            pattern = pattern
        )
        val specialTokensSet = specialTokens.keys.map { it.utf8() }.toSet()
        TokenEncoder(bpe = coreBPE, specialTokensSet = specialTokensSet)
    }



    fun getInput(text: String):Tensor {
        var tokens = tokenizer.encode(text,setOf("all"))
        val shape = longArrayOf(1, (tokens.size).toLong())
        val inputIds = IntArray(tokens.size)
        for(i in tokens.indices){
            inputIds[i] = tokens[i]
        }
        return Tensor.fromBlob(inputIds, shape)

    }

    fun textNormalizer(text: String,lang: String="zh"):String {
        val normText = when(lang){
            "zh" -> TextUtils.textNormalizer(text).trim()
            else -> EnTextUtils.textNormalizer(text).trim()
        }
        return normText
    }

    fun prepareGenerate(text: String):String{

        return "<|im_start|>user\n$text<|im_end|><|im_start|>assistant\n"
    }


}
