package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object FolderIcons {
    val icons = mapOf(
        "folder" to Icons.Default.Folder,
        "work" to Icons.Default.Work,
        "star" to Icons.Default.Star,
        "favorite" to Icons.Default.Favorite,
        "home" to Icons.Default.Home,
        "school" to Icons.Default.School,
        "build" to Icons.Default.Build,
        "shopping_cart" to Icons.Default.ShoppingCart,
        "flight" to Icons.Default.Flight,
        "event" to Icons.Default.Event,
        "camera" to Icons.Default.Camera,
        "pets" to Icons.Default.Pets,
        "lightbulb" to Icons.Default.Lightbulb,
        "article" to Icons.Default.Article,
        "music_note" to Icons.Default.MusicNote,
        "sports_esports" to Icons.Default.SportsEsports
    )

    fun getIcon(name: String?): ImageVector {
        return icons[name] ?: Icons.Default.Folder
    }
}
