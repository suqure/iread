package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.BookSpeaker

@Dao
interface BookSpeakerDao {

    @get:Query("select * from bookSpeaker ")
    val all: List<BookSpeaker>

    @Query("select * from bookSpeaker ")
    fun flowAll(): Flow<List<BookSpeaker>>

    @Query("select * from bookSpeaker where bookUrl = :bookUrl")
    fun flowByBook(bookUrl:String): Flow<List<BookSpeaker>>

    @Query("select * from bookSpeaker where bookUrl = :bookUrl")
    fun findByBook(bookUrl:String): List<BookSpeaker>

    @get:Query("select count(*) from bookSpeaker")
    val count: Int

    @Query("select * from bookSpeaker where id = :id")
    fun get(id: Long): BookSpeaker?

    @Query("select * from bookSpeaker where bookUrl = :bookUrl and spkName=:spkName")
    fun get(bookUrl: String,spkName:String): BookSpeaker?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg bookSpeaker: BookSpeaker)

    @Delete
    fun delete(vararg bookSpeaker: BookSpeaker)

    @Update
    fun update(vararg bookSpeaker: BookSpeaker)


    @Query("delete from bookSpeaker where id=:id")
    fun delete(id:Long)

    @Query("delete from bookSpeaker where bookUrl=:bookUrl")
    fun deleteByBook(bookUrl: String)

    @Query("delete from bookSpeaker where bookUrl=:bookUrl and spkName=:spkName")
    fun deleteByBookAndSpeaker(bookUrl: String,spkName: String)





}