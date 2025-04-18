package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.RssReadAloudRule

@Dao
interface RssReadAloudRuleDao {

    @get:Query("select * from rssReadAloudRule ")
    val all: List<RssReadAloudRule>

    @Query("select * from rssReadAloudRule ")
    fun flowAll(): Flow<List<RssReadAloudRule>>



    @get:Query("select count(*) from rssReadAloudRule")
    val count: Int

    @Query("select * from rssReadAloudRule where url = :url")
    fun get(url: String): RssReadAloudRule?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg rssReadAloudRule: RssReadAloudRule)

    @Delete
    fun delete(vararg rssReadAloudRule: RssReadAloudRule)

    @Update
    fun update(vararg rssReadAloudRule: RssReadAloudRule)


    @Query("delete from rssReadAloudRule where url=:url")
    fun delete(url:String)


}