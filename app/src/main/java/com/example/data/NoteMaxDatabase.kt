package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.dao.ImageDao
import com.example.data.entities.FolderEntity
import com.example.data.entities.ImageEntity
import com.example.data.entities.NoteEntity

@Database(entities = [
    FolderEntity::class, NoteEntity::class, ImageEntity::class,
    com.example.data.entities.TableEntity::class, 
    com.example.data.entities.ColumnEntity::class, 
    com.example.data.entities.RowEntity::class, 
    com.example.data.entities.CellEntity::class
], version = 6, exportSchema = false)
abstract class NoteMaxDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao
    abstract fun imageDao(): ImageDao
    abstract fun tableDao(): com.example.data.dao.TableDao

    companion object {
        @Volatile
        private var INSTANCE: NoteMaxDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE folders SET updatedAt = createdAt")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN color INTEGER")
                db.execSQL("ALTER TABLE folders ADD COLUMN iconName TEXT")
                db.execSQL("ALTER TABLE folders ADD COLUMN defaultViewModeString TEXT")
                db.execSQL("ALTER TABLE folders ADD COLUMN showCompactPreviews INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN previewText TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN drawingData TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE folders ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `images` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `uri` TEXT NOT NULL, `parentFolderId` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `tables` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `folderId` INTEGER, `title` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`folderId`) REFERENCES `folders`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `table_columns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableId` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `displayOrder` INTEGER NOT NULL, FOREIGN KEY(`tableId`) REFERENCES `tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `table_rows` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `tableId` INTEGER NOT NULL, `displayOrder` INTEGER NOT NULL, FOREIGN KEY(`tableId`) REFERENCES `tables`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE TABLE IF NOT EXISTS `table_cells` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rowId` INTEGER NOT NULL, `columnId` INTEGER NOT NULL, `value` TEXT NOT NULL, FOREIGN KEY(`rowId`) REFERENCES `table_rows`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`columnId`) REFERENCES `table_columns`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tables_folderId` ON `tables` (`folderId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_columns_tableId` ON `table_columns` (`tableId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_rows_tableId` ON `table_rows` (`tableId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_cells_rowId` ON `table_cells` (`rowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_table_cells_columnId` ON `table_cells` (`columnId`)")
            }
        }

        fun getDatabase(context: Context): NoteMaxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteMaxDatabase::class.java,
                    "notemax_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
