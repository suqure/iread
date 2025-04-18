package ltd.finelink.read.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rssReadAloudRule")
data class RssReadAloudRule(
    @PrimaryKey
    var url: String,
    var ignoreTags:String="",
    var acceptTags:String="",
    var ignoreClass: String="",
    var acceptClass: String="",
    var acceptIds:String="",
    var ignoreIds: String=""
)