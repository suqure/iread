package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class ChatFeature {
    lateinit var source: FloatArray
    lateinit var shape: LongArray
    var prompt: String?=null
}
