package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.BookSpeakerDetail

@Dao
interface BookSpeakerDetailDao {

    @get:Query("select * from bookSpeakerDetail ")
    val all: List<BookSpeakerDetail>

    @Query("select * from bookSpeakerDetail ")
    fun flowAll(): Flow<List<BookSpeakerDetail>>

    @Query("select * from bookSpeakerDetail where bookUrl = :bookUrl  order by chapter,pos")
    fun flowByBook(bookUrl:String): Flow<List<BookSpeakerDetail>>


    @Query("select * from bookSpeakerDetail where bookUrl = :bookUrl and  text like '%' ||:keyword || '%' or spkName like  '%' ||:keyword || '%' order by chapter,pos")
    fun flowSearch(bookUrl:String,keyword: String): Flow<List<BookSpeakerDetail>>

    @Query("select * from bookSpeakerDetail where bookUrl = :bookUrl")
    fun findByBook(bookUrl:String): List<BookSpeakerDetail>

    @get:Query("select count(*) from bookSpeaker")
    val count: Int

    @Query("select count(*) from bookSpeakerDetail where bookUrl = :bookUrl and spkName = :spkName")
    fun countSpeaker(bookUrl:String,spkName:String): Int

    @Query("select * from bookSpeakerDetail where id = :id")
    fun get(id: Long): BookSpeakerDetail?

    @Query("select * from bookSpeakerDetail where bookUrl = :bookUrl and detailId=:detailId")
    fun get(bookUrl: String,detailId:String): BookSpeakerDetail?


    @Query("select spkName from bookSpeakerDetail where bookUrl = :bookUrl group by spkName")
    fun groupSpkName(bookUrl: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookSpeaker: BookSpeakerDetail)

    @Delete
    fun delete(vararg bookSpeaker: BookSpeakerDetail)

    @Update
    fun update(vararg bookSpeaker: BookSpeakerDetail)


    @Query("delete from bookSpeakerDetail where id=:id")
    fun delete(id:Long)


    @Query("delete from bookSpeakerDetail where bookUrl=:bookUrl")
    fun deleteByBook(bookUrl: String)





}