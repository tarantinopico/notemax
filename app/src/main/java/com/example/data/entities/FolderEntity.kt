package com.example.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentFolderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentFolderId")]
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentFolderId: Long? = null,
    val color: Long? = null,
    val iconName: String? = null,
    val defaultViewModeString: String? = null,
    val showCompactPreviews: Boolean = false,
    val isLocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
