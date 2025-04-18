package ltd.finelink.read.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import ltd.finelink.read.data.dao.BookChapterDao
import ltd.finelink.read.data.dao.BookDao
import ltd.finelink.read.data.dao.BookGroupDao
import ltd.finelink.read.data.dao.BookSourceDao
import ltd.finelink.read.data.dao.BookSpeakerDao
import ltd.finelink.read.data.dao.BookSpeakerDetailDao
import ltd.finelink.read.data.dao.BookmarkDao
import ltd.finelink.read.data.dao.CacheDao
import ltd.finelink.read.data.dao.CookieDao
import ltd.finelink.read.data.dao.DictRuleDao
import ltd.finelink.read.data.dao.KeyboardAssistsDao
import ltd.finelink.read.data.dao.LLMConfigDao
import ltd.finelink.read.data.dao.LocalTTSDao
import ltd.finelink.read.data.dao.ReadAloudBookDao
import ltd.finelink.read.data.dao.ReadRecordDao
import ltd.finelink.read.data.dao.ReplaceRuleDao
import ltd.finelink.read.data.dao.RssArticleDao
import ltd.finelink.read.data.dao.RssReadAloudRuleDao
import ltd.finelink.read.data.dao.RssSourceDao
import ltd.finelink.read.data.dao.RssStarDao
import ltd.finelink.read.data.dao.RuleSubDao
import ltd.finelink.read.data.dao.SearchBookDao
import ltd.finelink.read.data.dao.SearchKeywordDao
import ltd.finelink.read.data.dao.ServerDao
import ltd.finelink.read.data.dao.TTSCacheDao
import ltd.finelink.read.data.dao.TTSSpeakerDao
import ltd.finelink.read.data.dao.TxtTocRuleDao
import ltd.finelink.read.data.entities.Book
import ltd.finelink.read.data.entities.BookChapter
import ltd.finelink.read.data.entities.BookGroup
import ltd.finelink.read.data.entities.BookSource
import ltd.finelink.read.data.entities.BookSpeaker
import ltd.finelink.read.data.entities.BookSpeakerDetail
import ltd.finelink.read.data.entities.Bookmark
import ltd.finelink.read.data.entities.Cache
import ltd.finelink.read.data.entities.Cookie
import ltd.finelink.read.data.entities.DictRule
import ltd.finelink.read.data.entities.KeyboardAssist
import ltd.finelink.read.data.entities.LLMConfig
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.data.entities.ReadAloudBook
import ltd.finelink.read.data.entities.ReadRecord
import ltd.finelink.read.data.entities.ReplaceRule
import ltd.finelink.read.data.entities.RssArticle
import ltd.finelink.read.data.entities.RssReadAloudRule
import ltd.finelink.read.data.entities.RssReadRecord
import ltd.finelink.read.data.entities.RssSource
import ltd.finelink.read.data.entities.RssStar
import ltd.finelink.read.data.entities.RuleSub
import ltd.finelink.read.data.entities.SearchBook
import ltd.finelink.read.data.entities.SearchKeyword
import ltd.finelink.read.data.entities.Server
import ltd.finelink.read.data.entities.TTSCache
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.data.entities.TxtTocRule
import ltd.finelink.read.help.DefaultData
import org.intellij.lang.annotations.Language
import splitties.init.appCtx
import java.util.Locale

val appDb by lazy {
    Room.databaseBuilder(appCtx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
        .fallbackToDestructiveMigrationFrom(1)
        .allowMainThreadQueries()
        .addMigrations(*DatabaseMigrations.migrations)
        .addCallback(AppDatabase.dbCallback)
        .build()
}

@Database(
    version = 4,
    exportSchema = true,
    entities = [Book::class, BookGroup::class, BookSource::class, BookChapter::class,
        ReplaceRule::class, SearchBook::class, SearchKeyword::class, Cookie::class,
        RssSource::class, Bookmark::class, RssArticle::class, RssReadRecord::class,
        RssStar::class, TxtTocRule::class, ReadRecord::class, Cache::class,
        RuleSub::class, DictRule::class, KeyboardAssist::class, Server::class,
        LocalTTS::class,TTSSpeaker::class,TTSCache::class,LLMConfig::class,
        ReadAloudBook::class,BookSpeaker::class,BookSpeakerDetail::class,RssReadAloudRule::class],
    autoMigrations = []
)
abstract class AppDatabase : RoomDatabase() {

    abstract val bookDao: BookDao
    abstract val bookGroupDao: BookGroupDao
    abstract val bookSourceDao: BookSourceDao
    abstract val bookChapterDao: BookChapterDao
    abstract val replaceRuleDao: ReplaceRuleDao
    abstract val searchBookDao: SearchBookDao
    abstract val searchKeywordDao: SearchKeywordDao
    abstract val rssSourceDao: RssSourceDao
    abstract val bookmarkDao: BookmarkDao
    abstract val rssArticleDao: RssArticleDao
    abstract val rssStarDao: RssStarDao
    abstract val cookieDao: CookieDao
    abstract val txtTocRuleDao: TxtTocRuleDao
    abstract val readRecordDao: ReadRecordDao
    abstract val cacheDao: CacheDao
    abstract val ruleSubDao: RuleSubDao
    abstract val dictRuleDao: DictRuleDao
    abstract val keyboardAssistsDao: KeyboardAssistsDao
    abstract val serverDao: ServerDao
    abstract val localTTSDao: LocalTTSDao
    abstract val ttsSpeakerDao: TTSSpeakerDao
    abstract val ttsCacheDao: TTSCacheDao
    abstract val llmConfigDao: LLMConfigDao
    abstract val readAloudBookDao: ReadAloudBookDao
    abstract val bookSpeakerDao: BookSpeakerDao
    abstract val bookSpeakerDetailDao: BookSpeakerDetailDao
    abstract val rssReadAloudRuleDao: RssReadAloudRuleDao

    companion object {

        const val DATABASE_NAME = "IRead.db"

        val dbCallback = object : Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                db.setLocale(Locale.CHINESE)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                @Language("sql")
                val insertBookGroupAllSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAll}, '全部', -10, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAll})
                """.trimIndent()
                db.execSQL(insertBookGroupAllSql)
                @Language("sql")
                val insertBookGroupLocalSql = """
                    insert into book_groups(groupId, groupName, 'order', enableRefresh, show) 
                    select ${BookGroup.IdLocal}, '本地', -9, 0, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocal})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalSql)
                @Language("sql")
                val insertBookGroupMusicSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdAudio}, '音频', -8, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdAudio})
                """.trimIndent()
                db.execSQL(insertBookGroupMusicSql)
                @Language("sql")
                val insertBookGroupNetNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdNetNone}, '网络未分组', -7, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdNetNone})
                """.trimIndent()
                db.execSQL(insertBookGroupNetNoneGroupSql)
                @Language("sql")
                val insertBookGroupLocalNoneGroupSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdLocalNone}, '本地未分组', -6, 0
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdLocalNone})
                """.trimIndent()
                db.execSQL(insertBookGroupLocalNoneGroupSql)
                @Language("sql")
                val insertBookGroupErrorSql = """
                    insert into book_groups(groupId, groupName, 'order', show) 
                    select ${BookGroup.IdError}, '更新失败', -1, 1
                    where not exists (select * from book_groups where groupId = ${BookGroup.IdError})
                """.trimIndent()
                db.execSQL(insertBookGroupErrorSql)
                @Language("sql")
                val upBookSourceLoginUiSql =
                    "update book_sources set loginUi = null where loginUi = 'null'"
                db.execSQL(upBookSourceLoginUiSql)
                @Language("sql")
                val upRssSourceLoginUiSql =
                    "update rssSources set loginUi = null where loginUi = 'null'"
                db.execSQL(upRssSourceLoginUiSql)
                db.query("select * from keyboardAssists order by serialNo").use {
                    if (it.count == 0) {
                        DefaultData.keyboardAssists.forEach { keyboardAssist ->
                            val contentValues = ContentValues().apply {
                                put("type", keyboardAssist.type)
                                put("key", keyboardAssist.key)
                                put("value", keyboardAssist.value)
                                put("serialNo", keyboardAssist.serialNo)
                            }
                            db.insert(
                                "keyboardAssists",
                                SQLiteDatabase.CONFLICT_REPLACE,
                                contentValues
                            )
                        }
                    }
                }
            }
        }

    }

}