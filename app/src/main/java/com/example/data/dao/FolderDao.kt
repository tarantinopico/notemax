package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("""
        SELECT f.*, 
        (SELECT COUNT(id) FROM folders WHERE parentFolderId = f.id) AS folderCount,
        (SELECT COUNT(id) FROM notes WHERE parentFolderId = f.id) AS noteCount
        FROM folders f WHERE parentFolderId IS NULL ORDER BY f.updatedAt DESC, f.name ASC
    """)
    fun getRootFoldersWithCounts(): Flow<List<FolderItem>>

    @Query("""
        SELECT f.*, 
        (SELECT COUNT(id) FROM folders WHERE parentFolderId = f.id) AS folderCount,
        (SELECT COUNT(id) FROM notes WHERE parentFolderId = f.id) AS noteCount
        FROM folders f WHERE parentFolderId = :parentId ORDER BY f.updatedAt DESC, f.name ASC
    """)
    fun getFoldersInParentWithCounts(parentId: Long): Flow<List<FolderItem>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): FolderEntity?
    
    @Query("SELECT * FROM folders WHERE id = :id")
    fun getFolderByIdFlow(id: Long): Flow<FolderEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}
