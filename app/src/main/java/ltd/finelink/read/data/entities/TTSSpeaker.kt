package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jayway.jsonpath.DocumentContext
import kotlinx.parcelize.Parcelize
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.readInt
import ltd.finelink.read.utils.readLong
import ltd.finelink.read.utils.readString

@Parcelize
@Entity(tableName = "ttsSpeaker")
data class TTSSpeaker(
    @PrimaryKey
    val id: Long = 0b1,
    var name: String? = "",
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    var cover: String? = "",
    var path: String? = "",
    var download : Boolean = false,
    var description: String? = "",
    var progress: Int = 0,
    var status: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable {
    fun categroy():String{
        if(type==0){
            return "GPT"
        }
        if(type==1){
            return "TTS"
        }
        if(type==3){
            return "CHAT"
        }
        if(type==4){
            return "CosyVoice"
        }
        if(type==5){
            return "FishSpeech"
        }
        return "Clone"
    }
    fun speakerFile():String{
        return "$id.speaker"
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): Result<TTSSpeaker> {
            return kotlin.runCatching {

                TTSSpeaker(
                    id = doc.readLong("$.id") ?: System.currentTimeMillis(),
                    name = doc.readString("$.name")!!,
                    path = doc.readString("$.path")?:"",
                    type = doc.readInt("$.type")?:0,
                    cover = doc.readString("$.cover"),
                    description = doc.readString("$.description")
                )
            }
        }

        fun fromJson(json: String): Result<TTSSpeaker> {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): Result<ArrayList<TTSSpeaker>> {
            return kotlin.runCatching {
                val sources = arrayListOf<TTSSpeaker>()
                val doc = jsonPath.parse(jsonArray).read<List<*>>("$")
                doc.forEach {
                    val jsonItem = jsonPath.parse(it)
                    fromJsonDoc(jsonItem).getOrThrow().let { source ->
                        sources.add(source)
                    }
                }
                return@runCatching sources
            }
        }

    }
}
