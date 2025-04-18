package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.TTSSpeaker

@Dao
interface TTSSpeakerDao {

    @get:Query("select * from ttsSpeaker order by name")
    val all: List<TTSSpeaker>

    @Query("select * from ttsSpeaker order by name")
    fun flowAll(): Flow<List<TTSSpeaker>>

    @get:Query("select count(*) from ttsSpeaker")
    val count: Int

    @Query("select * from ttsSpeaker where id = :id")
    fun get(id: Long): TTSSpeaker?

    @Query("select name from ttsSpeaker where id = :id")
    fun getName(id: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg ttsSpeaker: TTSSpeaker)

    @Delete
    fun delete(vararg ttsSpeaker: TTSSpeaker)

    @Update
    fun update(vararg ttsSpeaker: TTSSpeaker)

    @Query("delete from ttsSpeaker where id < 0")
    fun deleteDefault()

    @Query("SELECT * FROM ttsSpeaker WHERE type = :type")
    fun flowByType(type: Int): Flow<List<TTSSpeaker>>


    @Query("SELECT * FROM ttsSpeaker WHERE type = :type and  download = :download ")
    fun findByTypeAndDownload(type: Int,download: Boolean): List<TTSSpeaker>

    @Query("select * from ttsSpeaker where download = :download order by name")
    fun flowByDownload(download: Boolean): Flow<List<TTSSpeaker>>
}