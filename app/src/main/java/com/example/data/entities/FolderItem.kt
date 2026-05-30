package com.example.data.entities

import androidx.room.Embedded

data class FolderItem(
    @Embedded val folder: FolderEntity,
    val folderCount: Int,
    val noteCount: Int,
    val tableCount: Int
) {
    val totalChildren: Int get() = folderCount + noteCount + tableCount
}
