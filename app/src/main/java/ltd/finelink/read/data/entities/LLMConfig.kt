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
@Entity(tableName = "llmConfig")
data class LLMConfig(
    @PrimaryKey
    val id: Long = 0b1,
    var name: String? = "",
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    var cover: String? ="",
    var lib: String="",
    var path: String? = "",
    var local: String? = "",
    @ColumnInfo(defaultValue = "0")
    var progress: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var status: Int = 0,
    @ColumnInfo(defaultValue = "0")
    var download: Boolean = false,
    var description:String?="",
) : Parcelable {
    fun categroy():String{
        if(type==0){
            return "MLC"
        }
        if(type==1){
            return "API"
        }

        return "UNK"
    }
    @Suppress("MemberVisibilityCanBePrivate")
    companion object {

        fun fromJsonDoc(doc: DocumentContext): Result<LLMConfig> {
            return kotlin.runCatching {
                LLMConfig(
                    id = doc.readLong("$.id") ?: System.currentTimeMillis(),
                    name = doc.readString("$.name")!!,
                    path = doc.readString("$.path")!!,
                    type = doc.readInt("$.type")!!,
                    cover = doc.readString("$.cover"),
                    lib = doc.readString("$.lib")?:"",
                    description = doc.readString("$.description")
                )
            }
        }

        fun fromJson(json: String): Result<LLMConfig> {
            return fromJsonDoc(jsonPath.parse(json))
        }

        fun fromJsonArray(jsonArray: String): Result<ArrayList<LLMConfig>> {
            return kotlin.runCatching {
                val sources = arrayListOf<LLMConfig>()
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
