package com.example

import android.app.Application
import com.example.data.NoteMaxDatabase
import com.example.data.NoteMaxRepository

class NoteMaxApplication : Application() {
    val database by lazy { NoteMaxDatabase.getDatabase(this) }
    val repository by lazy { 
        NoteMaxRepository(database.folderDao(), database.noteDao()) 
    }
}
