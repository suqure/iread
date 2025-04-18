package ltd.finelink.read.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {

    val migrations: Array<Migration> by lazy {
        arrayOf(
            migration_2_3,
            migration_3_4
        )
    }
    private val migration_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS `llmConfig` 
                    (`id` INTEGER NOT NULL, `name` TEXT, 
                    `type` INTEGER NOT NULL DEFAULT(0),
                    `lib` TEXT NOT NULL, 
                    `cover` TEXT, 
                    `path` TEXT,
                    `local` TEXT,
                    `progress` INTEGER NOT NULL  DEFAULT(0),
                    `status` INTEGER NOT NULL  DEFAULT(0),
                    `description` TEXT NOT NULL,
                    `download` INTEGER NOT NULL DEFAULT(0),
                    PRIMARY KEY(`id`))"""
            )
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS `readAloudBook` 
                    (`bookUrl` TEXT NOT NULL,  
                    `modelId` INTEGER NOT NULL,
                    `speakerId` INTEGER NOT NULL DEFAULT(0),
                    `dialogueId` INTEGER NOT NULL DEFAULT(0),
                    `totalChapterNum` INTEGER NOT NULL,
                    `durChapterIndex` INTEGER NOT NULL  DEFAULT(0), 
                    `durChapterPos` INTEGER NOT NULL DEFAULT(0),
                    `advanceMode` INTEGER NOT NULL DEFAULT(0),
                    PRIMARY KEY(`bookUrl`))"""
            )
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS `bookSpeaker` 
                    (`id` INTEGER NOT NULL, 
                    `bookUrl` TEXT NOT NULL,
                    `modelId` INTEGER NOT NULL,
                    `speakerId` INTEGER NOT NULL,
                    `spkName` TEXT NOT NULL,
                    PRIMARY KEY(`id`))"""
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookSpeaker_bookUrl_spkName` ON `bookSpeaker` (`bookUrl`, `spkName`) ")
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS `bookSpeakerDetail` 
                    (`id` INTEGER NOT NULL, 
                    `bookUrl` TEXT NOT NULL,
                    `spkName` TEXT NOT NULL, 
                    `text` TEXT  NOT NULL, 
                    `detailId` TEXT NOT NULL,
                    `chapter` INTEGER NOT NULL DEFAULT(0),
                    `pos` INTEGER NOT NULL DEFAULT(0),
                    PRIMARY KEY(`id`))"""
            )
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_bookSpeakerDetail_bookUrl_detailId` ON `bookSpeakerDetail` (`bookUrl`, `detailId`) ")
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_bookSpeakerDetail_bookUrl_spkName` ON `bookSpeakerDetail` (`bookUrl`, `spkName`) ")


        }
    }

    private val migration_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """CREATE TABLE IF NOT EXISTS `rssReadAloudRule` 
                    (`url` TEXT NOT NULL,  
                    `ignoreTags` TEXT NOT NULL, 
                    `acceptTags` TEXT NOT NULL,
                    `ignoreClass` TEXT NOT NULL, 
                    `acceptClass` TEXT NOT NULL, 
                    `ignoreIds` TEXT NOT NULL, 
                    `acceptIds` TEXT NOT NULL, 
                    PRIMARY KEY(`url`))"""
            )
        }
    }











}