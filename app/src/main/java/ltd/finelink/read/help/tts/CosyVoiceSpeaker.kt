package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class CosyVoiceSpeaker {
    lateinit var embedding: FloatArray
    lateinit var shape: LongArray
    lateinit var prompt: IntArray
    lateinit var promptShape: LongArray
    lateinit var speech: IntArray
    lateinit var speechShape: LongArray
    lateinit var feat: FloatArray
    lateinit var featShape: LongArray
}
