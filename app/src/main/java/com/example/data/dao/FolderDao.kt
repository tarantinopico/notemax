package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId ORDER BY name ASC")
    fun getFoldersInParent(parentId: Long): Flow<List<FolderEntity>>

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
