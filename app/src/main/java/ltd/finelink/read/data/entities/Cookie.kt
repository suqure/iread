package ltd.finelink.read.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cookies", indices = [(Index(value = ["url"], unique = true))])
data class Cookie(
    @PrimaryKey
    var url: String = "",
    var cookie: String = ""
)