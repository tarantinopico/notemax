package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.entities.FolderEntity
import com.example.data.entities.NoteEntity

@Database(entities = [FolderEntity::class, NoteEntity::class], version = 1, exportSchema = false)
abstract class NoteMaxDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteMaxDatabase? = null

        fun getDatabase(context: Context): NoteMaxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteMaxDatabase::class.java,
                    "notemax_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
