package ltd.finelink.read.data.entities

import androidx.room.Entity
import androidx.room.Ignore
import ltd.finelink.read.utils.GSON
import ltd.finelink.read.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel


@Entity(
    tableName = "rssStars",
    primaryKeys = ["origin", "link"]
)
data class RssStar(
    override var origin: String = "",
    var sort: String = "",
    var title: String = "",
    var starTime: Long = 0,
    override var link: String = "",
    var pubDate: String? = null,
    var description: String? = null,
    var content: String? = null,
    var image: String? = null,
    override var variable: String? = null
) : BaseRssArticle {

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    fun toRssArticle() = RssArticle(
        origin = origin,
        sort = sort,
        title = title,
        link = link,
        pubDate = pubDate,
        description = description,
        content = content,
        image = image,
        variable = variable
    )
}
