package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.entities.ImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE parentFolderId = :folderId ORDER BY createdAt DESC")
    fun getImagesInFolder(folderId: Long): Flow<List<ImageEntity>>
    
    @Query("SELECT * FROM images WHERE id = :id")
    suspend fun getImageById(id: Long): ImageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: ImageEntity): Long
    
    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: Long)
}
