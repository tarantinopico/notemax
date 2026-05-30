package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import com.example.data.entities.NoteEntity

@Composable
fun DirectoryScreen(
    viewModel: DirectoryViewModel,
    onNavigateToFolder: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToNote: (Long) -> Unit
) {
    val currentFolder by viewModel.currentFolder.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Any?>(null) } 
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showFolderSettingsDialog by remember { mutableStateOf(false) }

    val accentColor = currentFolder?.color?.let { Color(it) } 
        ?: MaterialTheme.colorScheme.primary

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showAddFolderDialog = true },
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                }
                ExtendedFloatingActionButton(
                    text = { Text("New Note", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Note") },
                    onClick = { showAddNoteDialog = true },
                    containerColor = accentColor,
                    contentColor = if (accentColor == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else Color.White
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            // Elegant Native Header
            val headerBg = currentFolder?.color?.let { Color(it).copy(alpha = 0.1f) } ?: Color.Transparent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentFolder != null) {
                    IconButton(
                        onClick = onNavigateUp,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Text(
                    text = currentFolder?.name ?: "NoteMax",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = currentFolder?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                
                if (currentFolder != null) {
                    IconButton(
                        onClick = { showFolderSettingsDialog = true },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Folder Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Box {
                    IconButton(
                        onClick = { showViewModeMenu = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                    ) {
                        val icon = when(viewMode) {
                            ViewMode.LIST -> Icons.Default.ViewList
                            ViewMode.GRID -> Icons.Default.GridView
                            ViewMode.TABLE -> Icons.Default.TableRows
                        }
                        Icon(icon, contentDescription = "Change View Mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(
                        expanded = showViewModeMenu,
                        onDismissRequest = { showViewModeMenu = false },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("List View") },
                            onClick = { viewModel.setViewMode(ViewMode.LIST); showViewModeMenu = false },
                            leadingIcon = { Icon(Icons.Default.ViewList, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Grid View") },
                            onClick = { viewModel.setViewMode(ViewMode.GRID); showViewModeMenu = false },
                            leadingIcon = { Icon(Icons.Default.GridView, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Table View") },
                            onClick = { viewModel.setViewMode(ViewMode.TABLE); showViewModeMenu = false },
                            leadingIcon = { Icon(Icons.Default.TableRows, contentDescription = null) }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (folders.isEmpty() && notes.isEmpty()) {
                    EmptyState()
                } else {
                    val showCompact = currentFolder?.showCompactPreviews == true
                    Crossfade(targetState = viewMode, label = "view_mode_anim") { mode ->
                        when (mode) {
                            ViewMode.LIST -> ListView(folders, notes, showCompact, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                            ViewMode.GRID -> GridView(folders, notes, showCompact, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                            ViewMode.TABLE -> TableView(folders, notes, showCompact, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                        }
                    }
                }
            }
        }
    }

    if (showAddFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("New Folder", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(24.dp),
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createFolder(name)
                    showAddFolderDialog = false
                }) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddFolderDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("New Note", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(24.dp),
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) viewModel.createNote(title)
                    showAddNoteDialog = false
                }) { Text("Create", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirm deletion", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(24.dp),
            text = { Text("Are you sure you want to delete this ${if (item is FolderEntity) "folder and all its contents" else "note"}?") },
            confirmButton = {
                TextButton(onClick = {
                    if (item is FolderEntity) viewModel.deleteFolder(item)
                    else if (item is NoteEntity) viewModel.deleteNote(item)
                    showDeleteConfirmDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel") }
            }
        )
    }

    if (showFolderSettingsDialog && currentFolder != null) {
        FolderSettingsSheet(
            folder = currentFolder!!,
            onDismiss = { showFolderSettingsDialog = false },
            onSave = { c, i, v, p -> viewModel.updateFolderSettings(c, i, v, p) }
        )
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        Text("This folder is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Create a folder or note to get started.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ListView(folders: List<FolderItem>, notes: List<NoteEntity>, showCompact: Boolean, onFolderClick: (Long) -> Unit, onNoteClick: (Long) -> Unit, onDelete: (Any) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), 
        verticalArrangement = Arrangement.spacedBy(16.dp), 
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
            FolderListRow(folderItem, onClick = { onFolderClick(folderItem.folder.id) }, onDelete = { onDelete(folderItem.folder) })
        }
        items(notes, key = { "n_${it.id}" }) { note ->
            NoteListRow(note, showCompact, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
        }
    }
}

@Composable
fun GridView(folders: List<FolderItem>, notes: List<NoteEntity>, showCompact: Boolean, onFolderClick: (Long) -> Unit, onNoteClick: (Long) -> Unit, onDelete: (Any) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
             FolderGridCard(folderItem, onClick = { onFolderClick(folderItem.folder.id) }, onDelete = { onDelete(folderItem.folder) })
        }
        items(notes, key = { "n_${it.id}" }) { note ->
             NoteGridCard(note, showCompact, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
        }
    }
}

@Composable
fun TableView(folders: List<FolderItem>, notes: List<NoteEntity>, showCompact: Boolean, onFolderClick: (Long) -> Unit, onNoteClick: (Long) -> Unit, onDelete: (Any) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(2f))
                Text("Modified", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        }
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
            val icon = FolderIcons.getIcon(folderItem.folder.iconName)
            val iconTint = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
            Row(modifier = Modifier.fillMaxWidth().clickable { onFolderClick(folderItem.folder.id) }.padding(vertical = 16.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(2f)) {
                    Text(folderItem.folder.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${folderItem.totalChildren} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatShortDate(folderItem.folder.updatedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { onDelete(folderItem.folder) }) { Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        }
        items(notes, key = { "n_${it.id}" }) { note ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note.id) }.padding(vertical = 16.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(2f)) {
                    Text(note.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (showCompact && note.previewText.isNotBlank()) {
                         Text(note.previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(formatShortDate(note.updatedAt), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { onDelete(note) }) { Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
        }
    }
}

@Composable
fun FolderListRow(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .clickable { onClick() }
            .border(1.dp, fColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(fColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = fColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor)
            Spacer(modifier = Modifier.height(2.dp))
            val itemsStr = if (folderItem.totalChildren == 1) "1 item" else "${folderItem.totalChildren} items"
            Text("$itemsStr • ${formatShortDate(folderItem.folder.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NoteListRow(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(2.dp))
                Text("Modified ${formatShortDate(note.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (showCompact && note.previewText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                note.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FolderGridCard(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, fColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(fColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = fColor)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(4.dp))
        val itemsStr = if (folderItem.totalChildren == 1) "1 item" else "${folderItem.totalChildren} items"
        Text(itemsStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NoteGridCard(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        if (showCompact && note.previewText.isNotBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                note.previewText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(modifier = Modifier.height(4.dp))
        Text(formatShortDate(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
