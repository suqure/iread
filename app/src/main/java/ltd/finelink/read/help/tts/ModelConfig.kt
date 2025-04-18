package ltd.finelink.read.help.tts

import androidx.annotation.Keep

@Keep
class ModelConfig {
    var symbols:  MutableList<String> = mutableListOf()
    var data: ConfigData? = null
    var version: Int = 1
}
