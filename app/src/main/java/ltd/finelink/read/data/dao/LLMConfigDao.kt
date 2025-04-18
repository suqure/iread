package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.LLMConfig

@Dao
interface LLMConfigDao {

    @get:Query("select * from llmConfig order by name")
    val all: List<LLMConfig>

    @Query("select * from llmConfig order by name")
    fun flowAll(): Flow<List<LLMConfig>>

    @get:Query("select count(*) from llmConfig")
    val count: Int

    @Query("select * from llmConfig where id = :id")
    fun get(id: Long): LLMConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg llmConfig: LLMConfig)

    @Delete
    fun delete(vararg llmConfig: LLMConfig)

    @Update
    fun update(vararg llmConfig: LLMConfig)

    @Query("delete from llmConfig where id < 0")
    fun deleteDefault()

    @Query("SELECT * FROM llmConfig WHERE type = :type")
    fun flowByType(type: Int): Flow<List<LLMConfig>>

    @Query("select * from llmConfig where download = :download order by name")
    fun flowByDownload(download: Boolean): Flow<List<LLMConfig>>

    @Query("select * from llmConfig where download = :download order by name")
    fun findByDownload(download: Boolean): List<LLMConfig>
}