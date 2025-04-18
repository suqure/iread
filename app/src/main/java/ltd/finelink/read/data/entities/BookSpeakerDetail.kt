package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bookSpeakerDetail",
    indices = [(Index(value = ["bookUrl","detailId"], unique = true)),
    (Index(value = ["bookUrl","spkName"], unique = false))])
data class BookSpeakerDetail(
    @PrimaryKey
    val id: Long = 0b1,
    var bookUrl:String,
    var spkName: String,
    var text: String,
    var detailId:String,
    @ColumnInfo(defaultValue = "0")
    var chapter: Int=0,
    @ColumnInfo(defaultValue = "0")
    var pos: Int=0,
) : Parcelable {


}
