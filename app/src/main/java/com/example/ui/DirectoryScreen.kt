package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.FolderEntity
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
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Any?>(null) } // Can be NoteEntity or FolderEntity

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentFolder?.name ?: "Home") },
                navigationIcon = {
                    if (currentFolder != null) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(
                    onClick = { showAddFolderDialog = true },
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Folder")
                }
                FloatingActionButton(onClick = { showAddNoteDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note")
                }
            }
        }
    ) { padding ->
        if (folders.isEmpty() && notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "Empty folder", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(folders, key = { "f_${it.id}" }) { folder ->
                    ListItem(
                        headlineContent = { Text(folder.name) },
                        leadingContent = { Icon(Icons.Default.Menu, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        trailingContent = {
                            IconButton(onClick = { showDeleteConfirmDialog = folder }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Folder")
                            }
                        },
                        modifier = Modifier.clickable { onNavigateToFolder(folder.id) }
                    )
                    HorizontalDivider()
                }
                items(notes, key = { "n_${it.id}" }) { note ->
                    ListItem(
                        headlineContent = { Text(note.title) },
                        leadingContent = { Icon(Icons.Default.List, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                        trailingContent = {
                            IconButton(onClick = { showDeleteConfirmDialog = note }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Note")
                            }
                        },
                        modifier = Modifier.clickable { onNavigateToNote(note.id) }
                    )
                    HorizontalDivider()
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
