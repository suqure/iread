package ltd.finelink.read.help.tiktoken

import ltd.finelink.read.help.tiktoken.internal.CoreBPE
import ltd.finelink.read.help.tiktoken.internal.TokenEncoder
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * A public interface for tokenization and de-tokenization tasks, especially tailored for handling text encoding and decoding.
 * The primary operations include [encode] to convert text to a sequence of integers (tokens), and [decode] to convert a sequence of integers back to text.
 *
 * The companion object provides methods to obtain an instance of [Tokenizer] with specified encodings, either by encoding name or model name.
 */
interface Tokenizer {

    /**
     *  Encodes a string into tokens.
     *
     *  Special tokens are artificial tokens used to unlock capabilities from a model,
     *  such as fill-in-the-middle. So we want to be careful about accidentally encoding special
     *  tokens, since they can be used to trick a model into doing something we don't want it to do.
     *
     *  Hence, by default, encode will raise an error if it encounters text that corresponds
     *  to a special token. This can be controlled on a per-token level using the [allowedSpecial]
     *  and `disallowed_special` parameters. In particular:
     *  - Setting [disallowedSpecial] to empty set will prevent this function from raising exceptions and
     *    cause all text corresponding to special tokens to be encoded as natural text.
     *  - Setting [allowedSpecial] to "all" will cause this function to treat all text
     *    corresponding to special tokens to be encoded as special tokens.
     *
     *  ```
     *  >>> tokenizer.encode("hello world")
     *  [31373, 995]
     *  >>> tokenizer.encode("<|endoftext|>", allowedSpecial = setOf("<|endoftext|>"))
     *  [50256]
     *  >>> tokenizer.encode("<|endoftext|>", allowedSpecial= setOf("all"))
     *  [50256]
     *  >>> tokenizer.encode("<|endoftext|>")
     *  # Raises exception
     *  >>> tokenizer.encode("<|endoftext|>", disallowedSpecial= emptySet())
     *  [27, 91, 437, 1659, 5239, 91, 29]
     *  ```
     *
     * @param text The text to be encoded.
     * @param allowedSpecial A set of special tokens that are permissible during the encoding process.
     * @param disallowedSpecial A set of special tokens that are not allowed during the encoding process.
     * @return An array of integers representing the encoded text.
     */
    fun encode(
        text: String,
        allowedSpecial: Set<String> = emptySet(),
        disallowedSpecial: Set<String> = setOf("all"),
    ): List<Int>

    /**
     * Encodes text corresponding to a single token to its token value.
     *
     * NOTE: this will encode all special tokens.
     *
     * Raises an exception if the token is not in the vocabulary.
     *
     * ```
     * >>> tokenizer.encodeSingleToken("hello")
     * 31373
     * ```
     */
    fun encodeSingleToken(text: String): Int

    /**
     * Decodes the given sequence of integers (tokens) back into text based on the underlying encoding scheme.
     *
     * @param tokens The array of integers to be decoded.
     * @return The decoded text.
     */
    fun decode(tokens: List<Int>): String

    /**
     * Decodes a token into bytes.
     *
     * NOTE: this will decode all special tokens.
     *
     * Raises an exception if the token is not in the vocabulary.
     */
    fun decode(token: Int): String

    companion object {

        private val decodeMap = mutableMapOf<Char,Int>()
        private val encodeMap = mutableMapOf<Int,Char>()
        /**
         * Builds and returns an instance of [Tokenizer] based on the specified [EncodingConfig].
         *
         * @param config The [EncodingConfig] object representing the encoding scheme to be used.
         * @return An instance of [Tokenizer].
         */
        fun from(config: EncodingConfig): Tokenizer {
            val coreBPE = CoreBPE.create(
                encoder = config.mergeableRanks,
                specialTokensEncoder = config.specialTokens,
                pattern = config.pattern
            )
            val specialTokensSet = config.specialTokens.keys.map { it.utf8() }.toSet()
            return TokenEncoder(bpe = coreBPE, specialTokensSet = specialTokensSet)
        }
        fun coverVocabToByString(vocab:String): ByteString {
            initMap()
            val bytes = ByteArray(vocab.length)
            var i = 0
            for(char in vocab){
                decodeMap[char]?.let {
                    bytes[i] = it.toByte()
                }
                i++
            }
            return bytes.toByteString()

        }

        private fun initMap(){
            if(decodeMap.isNotEmpty()&& encodeMap.isNotEmpty()){
                return
            }
            for(i in '!'..'~'){
                decodeMap[i] = i.code
                encodeMap[i.code]= i
            }
            for(i in '¡'..'¬'){
                decodeMap[i] = i.code
                encodeMap[i.code]= i
            }
            for(i in '®'..'ÿ'){
                decodeMap[i] = i.code
                encodeMap[i.code]= i
            }
            var n = 256
            for(i in 0 until 256){
                if(encodeMap[i]==null){
                    val char = n.toChar()
                    encodeMap[i] = char
                    decodeMap[char] = i
                    n++
                }
            }
        }
    }
}
