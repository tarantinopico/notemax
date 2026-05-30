package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.entities.FolderEntity
import com.example.data.entities.NoteEntity

@Database(entities = [FolderEntity::class, NoteEntity::class], version = 4, exportSchema = false)
abstract class NoteMaxDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao

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

        fun getDatabase(context: Context): NoteMaxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteMaxDatabase::class.java,
                    "notemax_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
