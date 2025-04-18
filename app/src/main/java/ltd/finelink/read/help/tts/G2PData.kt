package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class G2PData {
    @JvmField
    var text: String? = null
    @JvmField
    var lang: String? = null
    @JvmField
    var phoneList: MutableList<String> = mutableListOf()
    @JvmField
    var tonesList: MutableList<Int?> = mutableListOf()
    @JvmField
    var word2ph: MutableList<Int?> = mutableListOf()
}
