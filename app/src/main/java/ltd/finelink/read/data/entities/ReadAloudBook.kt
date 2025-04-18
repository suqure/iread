package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "readAloudBook")
data class ReadAloudBook(
    @PrimaryKey
    var bookUrl:String,
    var modelId: Long,
    @ColumnInfo(defaultValue = "0")
    var speakerId: Long=0,
    @ColumnInfo(defaultValue = "0")
    var dialogueId: Long=0,

    var totalChapterNum: Int=0,
    @ColumnInfo(defaultValue = "0")
    var durChapterIndex: Int=0,
    @ColumnInfo(defaultValue = "0")
    var durChapterPos: Int=0,
    @ColumnInfo(defaultValue = "0")
    var advanceMode:Boolean=false,
) : Parcelable {


}
