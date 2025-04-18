package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class Speaker {
    lateinit var bert: FloatArray
    lateinit var phones: IntArray
    lateinit var bertShape: LongArray
    lateinit var feat: FloatArray
    lateinit var spec: FloatArray
    lateinit var featShape: LongArray
    lateinit var specShape: LongArray
}
