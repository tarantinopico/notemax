package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val parentFolderId: Long,
    val createdAt: Long = System.currentTimeMillis()
)
