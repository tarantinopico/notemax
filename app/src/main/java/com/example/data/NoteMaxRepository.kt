package com.example.data

import com.example.data.dao.FolderDao
import com.example.data.dao.NoteDao
import com.example.data.dao.ImageDao
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import com.example.data.entities.ImageEntity
import com.example.data.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

class NoteMaxRepository(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao,
    private val imageDao: ImageDao
) {
    fun getFoldersWithCounts(parentId: Long?): Flow<List<FolderItem>> {
        return if (parentId == null) folderDao.getRootFoldersWithCounts() else folderDao.getFoldersInParentWithCounts(parentId)
    }

    fun getNotes(parentId: Long?): Flow<List<NoteEntity>> {
        return if (parentId == null) noteDao.getRootNotes() else noteDao.getNotesInParent(parentId)
    }

    fun getImages(parentId: Long?): Flow<List<ImageEntity>> {
        return if (parentId == null) kotlinx.coroutines.flow.flowOf(emptyList()) else imageDao.getImagesInFolder(parentId)
    }

    suspend fun insertImage(image: ImageEntity) = imageDao.insertImage(image)
    suspend fun deleteImageById(id: Long) = imageDao.deleteImageById(id)
    suspend fun getImageById(id: Long) = imageDao.getImageById(id)

    fun getAllNotesFlow(): Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()

    suspend fun getNoteByTitle(title: String): NoteEntity? = noteDao.getNoteByTitle(title)

    fun getFolderFlow(id: Long): Flow<FolderEntity?> = folderDao.getFolderByIdFlow(id)
    suspend fun getFolderById(id: Long): FolderEntity? = folderDao.getFolderById(id)

    suspend fun insertFolder(folder: FolderEntity) {
        folderDao.insertFolder(folder)
    }

    suspend fun updateFolder(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        folderDao.deleteFolder(folder)
    }

    fun getNoteFlow(id: Long): Flow<NoteEntity?> = noteDao.getNoteByIdFlow(id)
    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(note: NoteEntity): Long = noteDao.insertNote(note)

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }
}
