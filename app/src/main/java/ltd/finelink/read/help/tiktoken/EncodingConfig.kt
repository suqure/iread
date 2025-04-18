package ltd.finelink.read.help.tiktoken

import okio.ByteString

/**
 * Manages configurations for token encoding, providing the settings and mappings needed to perform
 * byte pair encoding (BPE) and handle special tokens.
 */
class EncodingConfig(

    /**
     * A regex pattern string that is used to split the input text.
     */
    val pattern: Regex,

    /**
     * A dictionary mapping mergeable token bytes to their ranks. The ranks must correspond to merge priority.
     */
    val mergeableRanks: Map<ByteString, Int>,

    /**
     * A dictionary mapping special token strings to their token values.
     */
    val specialTokens: Map<ByteString, Int>,

    /**
     * The number of tokens in the vocabulary.
     * If provided, it is checked that the number of mergeable tokens and special tokens is equal to this number.
     */
    val explicitNVocab: Int? = null,
) {
    init {
        if (explicitNVocab != null) {
            val totalCount = mergeableRanks.size + specialTokens.size
            require(totalCount == explicitNVocab) { "the expected number of tokens in the vocabulary is incorrect, expected: $explicitNVocab, actual: $totalCount" }
        }
    }

}
