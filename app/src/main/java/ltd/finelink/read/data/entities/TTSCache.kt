package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "TTSCache")
data class TTSCache(
    @PrimaryKey
    val id: Long = 0b1,
    var bookUrl:String,
    var bookName: String? = "",
    var modelId: Long,
    var speakerId: Long? =0,
    var text: String,
    var file: String,
    var chapterTitle:String?="",
    var chapterIndex:Int?=0,
    var pageIndex: Int?=0,
    var position: Int?=0,
    var subPosition: Int?=0,
    @ColumnInfo(defaultValue = "0")
    var lastUpdateTime: Long = System.currentTimeMillis()
) : Parcelable {


}
