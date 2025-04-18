package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jayway.jsonpath.DocumentContext
import kotlinx.parcelize.Parcelize
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.jsonPath
import ltd.finelink.read.utils.readInt
import ltd.finelink.read.utils.readLong
import ltd.finelink.read.utils.readString

@Parcelize
@Entity(tableName = "localTTS")
data class LocalTTS(
    @PrimaryKey
    val id: Long = 0b1,
    var name: String? = "",
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    var cover: String? ="",
    var speaker: String? ="",
    var speakerId: Long? =0,
    var speakerName: String?="Default",
    var path: String? = "",
    var local: String? = "",
    var progress: Int = 0,
    var download: Boolean = false,
    var status: Int = 0,
    var speed: Float?=1.0f,
    var temperature :Float?=1.0f,
    var topK:Int?=5,
    var topP:Float?=1.0f,
    var refId:Long?= 0b1,
    var description:String?="",
    var supportLang:String="zh",
    var mainLang:String?="zh",
    var refineText:Boolean=false,
    @ColumnInfo(defaultValue = "0")
    var lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable {
    fun categroy():String{
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
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): Result<LocalTTS> {
            return kotlin.runCatching {

                LocalTTS(
                    id = doc.readLong("$.id") ?: System.currentTimeMillis(),
                    name = doc.readString("$.name")!!,
                    path = doc.readString("$.path")?:"",
                    type = doc.readInt("$.type")?:0,
                    cover = doc.readString("$.cover"),
                    refId = doc.readLong("$.refId"),
                    supportLang = doc.readString("$.supportLang")?:"zh",
                    mainLang = doc.readString("$.supportLang")?.let {
                        it.split(",")[0]
                    }?:"zh",
                    description = doc.readString("$.description")
                )
            }
        }

        fun fromJson(json: String): Result<LocalTTS> {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): Result<ArrayList<LocalTTS>> {
            return kotlin.runCatching {
                val sources = arrayListOf<LocalTTS>()
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
