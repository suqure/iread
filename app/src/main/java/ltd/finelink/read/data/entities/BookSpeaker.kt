package ltd.finelink.read.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "bookSpeaker",indices = [(Index(value = ["bookUrl","spkName"], unique = true))])
data class BookSpeaker(
    @PrimaryKey
    val id: Long = 0b1,
    var bookUrl:String,
    var modelId: Long=0,
    var speakerId: Long=0,
    var spkName: String
) : Parcelable {


}
