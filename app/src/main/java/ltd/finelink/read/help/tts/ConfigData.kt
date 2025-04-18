package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class ConfigData {
    var samplingRate: Int? = null
    var filterLength: Int? = null
    var hopLength: Int? = null
    var addBlank: Boolean? = null
    private var nSpeakers: Int? = null
    var spk2id: Map<String, Int>? = null
    fun getNSpeakers(): Int? {
        return nSpeakers
    }

    fun setNSpeakers(nSpeakers: Int?) {
        this.nSpeakers = nSpeakers
    }
}
