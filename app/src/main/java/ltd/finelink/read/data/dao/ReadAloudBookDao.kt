package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.ReadAloudBook

@Dao
interface ReadAloudBookDao {

    @get:Query("select * from readAloudBook ")
    val all: List<ReadAloudBook>

    @Query("select * from readAloudBook ")
    fun flowAll(): Flow<List<ReadAloudBook>>

    @get:Query("select count(*) from readAloudBook")
    val count: Int

    @Query("select * from readAloudBook where bookUrl = :id")
    fun get(id: String): ReadAloudBook?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg readAloudBook: ReadAloudBook)

    @Delete
    fun delete(vararg readAloudBook: ReadAloudBook)

    @Update
    fun update(vararg readAloudBook: ReadAloudBook)


    @Query("delete from readAloudBook where bookUrl=:id")
    fun delete(id:String)





}