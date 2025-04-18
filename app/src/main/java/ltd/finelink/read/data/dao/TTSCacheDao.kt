package ltd.finelink.read.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ltd.finelink.read.data.entities.TTSCache

@Dao
interface TTSCacheDao {

    @get:Query("select * from TTSCache order by bookName,chapterIndex")
    val all: List<TTSCache>

    @Query("select * from TTSCache order by bookName,chapterIndex")
    fun flowAll(): Flow<List<TTSCache>>

    @get:Query("select count(*) from TTSCache")
    val count: Int

    @Query("select * from TTSCache where id = :id")
    fun get(id: Long): TTSCache?

    @Query("select * from TTSCache where bookUrl= :bookUrl order by modelId,chapterIndex")
    fun flowBook(bookUrl:String): Flow<List<TTSCache>>

    @Query("select * from TTSCache where modelId= :modelId order by bookName,modelId,chapterIndex")
    fun flowModel(modelId:Long): Flow<List<TTSCache>>

    @Query("select * from TTSCache where bookUrl= :bookUrl order by modelId,chapterIndex")
    fun searchByBook(bookUrl:String): List<TTSCache>

    @Query("select * from TTSCache where modelId= :modelId order by bookName,modelId,chapterIndex")
    fun searchByModel(modelId:Long): List<TTSCache>

    @Query("select * from TTSCache where speakerId= :speakerId order by bookName,modelId,chapterIndex")
    fun flowSpeaker(speakerId:Long): Flow<List<TTSCache>>

    @Query("select * from TTSCache where speakerId= :speakerId order by bookName,modelId,chapterIndex")
    fun searchBySpeaker(speakerId:Long): List<TTSCache>

    @Query("delete from TTSCache where modelId=:modelId")
    fun deleteByModel(modelId: Long)

    @Query("delete from TTSCache where bookUrl=:bookUrl")
    fun deleteByBook(bookUrl: String)

    @Query("delete from TTSCache where speakerId=:speakerId")
    fun deleteBySpeaker(speakerId: Long)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg ttsCache: TTSCache)

    @Delete
    fun delete(vararg ttsCache: TTSCache)

    @Update
    fun update(vararg ttsCache: TTSCache)


    @Query("delete from TTSCache ")
    fun deleteAll()

    @Query("select * from TTSCache where bookName like '%' ||:keyword || '%' or chapterTitle like  '%' ||:keyword || '%' or text like '%' ||:keyword|| '%' order by bookName,modelId,chapterIndex")
    fun flowSearch(keyword: String): Flow<List<TTSCache>>




}