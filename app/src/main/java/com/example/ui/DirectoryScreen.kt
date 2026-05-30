package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import com.example.data.entities.NoteEntity

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        currentFolder?.name ?: "Home",
                        fontWeight = FontWeight.SemiBold
                    ) 
                },
                navigationIcon = {
                    if (currentFolder != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showViewModeMenu = true }) {
                            val icon = when(viewMode) {
                                ViewMode.LIST -> Icons.Default.ViewList
                                ViewMode.GRID -> Icons.Default.GridView
                                ViewMode.TABLE -> Icons.Default.TableRows
                            }
                            Icon(icon, contentDescription = "Change View Mode")
                        }
                        DropdownMenu(
                            expanded = showViewModeMenu,
                            onDismissRequest = { showViewModeMenu = false }
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
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showAddFolderDialog = true },
                    modifier = Modifier.padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                }
                ExtendedFloatingActionButton(
                    text = { Text("New Note") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Note") },
                    onClick = { showAddNoteDialog = true }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            if (folders.isEmpty() && notes.isEmpty()) {
                EmptyState()
            } else {
                Crossfade(targetState = viewMode, label = "view_mode_anim") { mode ->
                    when (mode) {
                        ViewMode.LIST -> ListView(folders, notes, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                        ViewMode.GRID -> GridView(folders, notes, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                        ViewMode.TABLE -> TableView(folders, notes, onNavigateToFolder, onNavigateToNote, { showDeleteConfirmDialog = it })
                    }
                }
            }
        }
    }

    if (showAddFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) viewModel.createFolder(name)
                    showAddFolderDialog = false
                }) { Text("Create") }
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
            title = { Text("New Note") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) viewModel.createNote(title)
                    showAddNoteDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirm deletion") },
            text = { Text("Are you sure you want to delete this ${if (item is FolderEntity) "folder and all its contents" else "note"}?") },
            confirmButton = {
                TextButton(onClick = {
                    if (item is FolderEntity) viewModel.deleteFolder(item)
                    else if (item is NoteEntity) viewModel.deleteNote(item)
                    showDeleteConfirmDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel") }
            }
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
        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))
        Text("This directory is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        Text("Create a folder or a note to get started.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ListView(
    folders: List<FolderItem>,
    notes: List<NoteEntity>,
    onFolderClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onDelete: (Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), 
        verticalArrangement = Arrangement.spacedBy(12.dp), 
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
            FolderListRow(folderItem, onClick = { onFolderClick(folderItem.folder.id) }, onDelete = { onDelete(folderItem.folder) })
        }
        items(notes, key = { "n_${it.id}" }) { note ->
            NoteListRow(note, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
        }
    }
}

@Composable
fun GridView(
    folders: List<FolderItem>,
    notes: List<NoteEntity>,
    onFolderClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onDelete: (Any) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(140.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
             FolderGridCard(folderItem, onClick = { onFolderClick(folderItem.folder.id) }, onDelete = { onDelete(folderItem.folder) })
        }
        items(notes, key = { "n_${it.id}" }) { note ->
             NoteGridCard(note, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
        }
    }
}

@Composable
fun TableView(
    folders: List<FolderItem>,
    notes: List<NoteEntity>,
    onFolderClick: (Long) -> Unit,
    onNoteClick: (Long) -> Unit,
    onDelete: (Any) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Name", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(2f))
                Text("Modified", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            }
            HorizontalDivider()
        }
        items(folders, key = { "f_${it.folder.id}" }) { folderItem ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onFolderClick(folderItem.folder.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(2f)) {
                    Text(folderItem.folder.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${folderItem.totalChildren} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(formatShortDate(folderItem.folder.updatedAt), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
            }
            HorizontalDivider()
        }
        items(notes, key = { "n_${it.id}" }) { note ->
            Row(modifier = Modifier.fillMaxWidth().clickable { onNoteClick(note.id) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(note.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(2f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatShortDate(note.updatedAt), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), maxLines = 1)
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun FolderListRow(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                val itemsStr = if (folderItem.totalChildren == 1) "1 item" else "${folderItem.totalChildren} items"
                Text("$itemsStr • Modified ${formatShortDate(folderItem.folder.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NoteListRow(note: NoteEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Modified ${formatShortDate(note.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FolderGridCard(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val itemsStr = if (folderItem.totalChildren == 1) "1 item" else "${folderItem.totalChildren} items"
            Text(itemsStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NoteGridCard(note: NoteEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(140.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.weight(1f))
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatShortDate(note.updatedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
