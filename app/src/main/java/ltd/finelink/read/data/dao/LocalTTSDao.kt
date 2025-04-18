package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.LocalTTS

@Dao
interface LocalTTSDao {

    @get:Query("select * from localTTS order by name")
    val all: List<LocalTTS>

    @Query("select * from localTTS order by name")
    fun flowAll(): Flow<List<LocalTTS>>

    @get:Query("select count(*) from localTTS")
    val count: Int

    @Query("select * from localTTS where id = :id")
    fun get(id: Long): LocalTTS?

    @Query("select name from localTTS where id = :id")
    fun getName(id: Long): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg localTTS: LocalTTS)

    @Delete
    fun delete(vararg localTTS: LocalTTS)

    @Update
    fun update(vararg localTTS: LocalTTS)

    @Query("delete from localTTS where id < 0")
    fun deleteDefault()

    @Query("SELECT * FROM localTTS WHERE type = :type")
    fun flowByType(type: Int): Flow<List<LocalTTS>>

    @Query("SELECT * FROM localTTS WHERE type = :type")
    fun fineByType(type: Int): List<LocalTTS>

    @Query("select * from localTTS where download = :download order by name")
    fun flowByDownload(download: Boolean): Flow<List<LocalTTS>>


    @Query("SELECT * FROM localTTS WHERE type != 2 and  download = true ")
    fun findReferModel(): List<LocalTTS>

    @Query("SELECT * FROM localTTS WHERE type != 1 and  download = true ")
    fun findMultiSpeakerModel(): List<LocalTTS>

    @Query("select * from localTTS where download = :download order by name")
    fun findByDownload(download: Boolean): List<LocalTTS>
}