package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import com.example.data.entities.NoteEntity
import com.example.data.entities.ImageEntity
import android.widget.Toast
import android.net.Uri
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectoryScreen(
    viewModel: DirectoryViewModel,
    onNavigateToFolder: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToNote: (Long) -> Unit,
    onNavigateToTable: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentFolder by viewModel.currentFolder.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val tables by viewModel.tables.collectAsStateWithLifecycle()
    val images by viewModel.images.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
    
    var showAddFolderDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showAddTableDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Any?>(null) } 
    var showViewModeMenu by remember { mutableStateOf(false) }
    var showFolderSettingsDialog by remember { mutableStateOf(false) }
    var showFabMenu by remember { mutableStateOf(false) }
    var viewingImageUri by remember { mutableStateOf<String?>(null) }

    val accentColor = currentFolder?.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

    val snackbarHostState = remember { SnackbarHostState() }

    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoFile = remember { mutableStateOf<java.io.File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile.value != null) {
            viewModel.createImage(Uri.fromFile(photoFile.value).toString())
        }
    }

    val launchCamera = {
        try {
            val tempFile = java.io.File.createTempFile("photo_", ".jpg", context.filesDir)
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
            photoFile.value = tempFile
            photoUri.value = uri
            takePictureLauncher.launch(uri)
        } catch (e: android.content.ActivityNotFoundException) {
            scope.launch {
                snackbarHostState.showSnackbar("No camera app found on this device.")
            }
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Failed to open camera: ${e.message}")
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Camera permission is required to take photos.")
            }
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { uri ->
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.createImage(uri.toString())
            } catch (e: Exception) {
                // Ignore missing permissions smoothly
                viewModel.createImage(uri.toString())
            }
        }
    }

    val attemptFolderNavigation: (FolderEntity) -> Unit = { folder ->
        if (folder.isLocked) {
            scope.launch {
                val success = BiometricHelper.authenticate(context as FragmentActivity, "Unlock Folder", "")
                if (success) {
                    onNavigateToFolder(folder.id)
                }
            }
        } else {
            onNavigateToFolder(folder.id)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(visible = showFabMenu, enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        SmallFloatingActionButton(
                            onClick = { 
                                showFabMenu = false
                                pickMediaLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        }
                        SmallFloatingActionButton(
                            onClick = { 
                                showFabMenu = false
                                val permission = android.Manifest.permission.CAMERA
                                if (ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    launchCamera()
                                } else {
                                    cameraPermissionLauncher.launch(permission)
                                }
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Take Photo")
                        }
                        SmallFloatingActionButton(
                            onClick = { showAddFolderDialog = true; showFabMenu = false },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "Add Folder")
                        }
                        SmallFloatingActionButton(
                            onClick = { showAddTableDialog = true; showFabMenu = false },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = "Add Table")
                        }
                        ExtendedFloatingActionButton(
                            text = { Text("New Note", fontWeight = FontWeight.SemiBold) },
                            icon = { Icon(Icons.Default.Add, contentDescription = "Add Note") },
                            onClick = { showAddNoteDialog = true; showFabMenu = false },
                            containerColor = accentColor,
                            contentColor = if (accentColor == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else Color.White
                        )
                    }
                }
                
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    containerColor = if (showFabMenu) MaterialTheme.colorScheme.surfaceVariant else accentColor,
                    contentColor = if (showFabMenu) MaterialTheme.colorScheme.onSurfaceVariant else (if (accentColor == MaterialTheme.colorScheme.primary) MaterialTheme.colorScheme.onPrimary else Color.White)
                ) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, contentDescription = "Open Menu")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            val headerBg = currentFolder?.color?.let { Color(it).copy(alpha = 0.5f) } ?: MaterialTheme.colorScheme.surface
            com.example.ui.theme.GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.ui.graphics.RectangleShape,
                color = headerBg,
                alpha = 0.75f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentFolder != null) {
                        IconButton(
                            onClick = onNavigateUp,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        text = currentFolder?.name ?: "NoteMax",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = currentFolder?.color?.let { Color(it).let { if (it.luminance() > 0.5f) Color.Black else Color.White } } ?: MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (currentFolder == null) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Global Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        IconButton(
                            onClick = { showFolderSettingsDialog = true },
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(14.dp))
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Folder Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    Box {
                        IconButton(
                            onClick = { showViewModeMenu = true },
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f), RoundedCornerShape(14.dp))
                        ) {
                            val icon = when(viewMode) {
                                ViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
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
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, contentDescription = null) }
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
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (folders.isEmpty() && notes.isEmpty() && images.isEmpty() && tables.isEmpty()) {
                    EmptyState()
                } else {
                    val showCompact = currentFolder?.showCompactPreviews == true
                    Crossfade(targetState = viewMode, label = "view_mode_anim") { mode ->
                        when (mode) {
                            ViewMode.LIST -> ListView(folders, notes, images, tables, showCompact, attemptFolderNavigation, onNavigateToNote, { viewingImageUri = it.uri }, onNavigateToTable, { showDeleteConfirmDialog = it }, { viewModel.lockFolder(it) })
                            ViewMode.GRID -> GridView(folders, notes, images, tables, showCompact, attemptFolderNavigation, onNavigateToNote, { viewingImageUri = it.uri }, onNavigateToTable, { showDeleteConfirmDialog = it }, { viewModel.lockFolder(it) })
                            ViewMode.TABLE -> TableView(folders, notes, images, tables, showCompact, attemptFolderNavigation, onNavigateToNote, { viewingImageUri = it.uri }, onNavigateToTable, { showDeleteConfirmDialog = it }, { viewModel.lockFolder(it) })
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
            title = { Text("New Note", fontWeight = FontWeight.Bold) },
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

    if (showAddTableDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = { Text("New Table", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Table Title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (title.isNotBlank()) viewModel.createTable(title)
                    showAddTableDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showAddTableDialog = false }) { Text("Cancel") }
            }
        )
    }

    showDeleteConfirmDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Confirm deletion") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(onClick = {
                    if (item is FolderEntity) viewModel.deleteFolder(item)
                    else if (item is NoteEntity) viewModel.deleteNote(item)
                    else if (item is ImageEntity) viewModel.deleteImage(item)
                    else if (item is com.example.data.entities.TableEntity) viewModel.deleteTable(item)
                    showDeleteConfirmDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
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
            onSave = { c, i, v, p, l, onSuccess -> viewModel.updateFolderSettings(c, i, v, p, l, onSuccess) }
        )
    }
    
    if (viewingImageUri != null) {
        FullScreenImageViewer(uri = viewingImageUri!!, onDismiss = { viewingImageUri = null })
    }
}

@Composable
fun FullScreenImageViewer(uri: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            scale = (scale * zoom).coerceIn(1f, 5f)
            val maxOffset = (scale - 1) * 500f
            offset = androidx.compose.ui.geometry.Offset(
                (offset.x + pan.x).coerceIn(-maxOffset, maxOffset),
                (offset.y + pan.y).coerceIn(-maxOffset, maxOffset)
            )
        }
    }) {
        AsyncImage(
            model = uri,
            contentDescription = "Full screen image",
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopStart).padding(32.dp).windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableItem(
    onDelete: () -> Unit,
    onLock: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                return@rememberSwipeToDismissBoxState false // Reset visually, let dialog handle
            } else if (dismissValue == SwipeToDismissBoxValue.StartToEnd && onLock != null) {
                onLock()
                return@rememberSwipeToDismissBoxState false
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.EndToStart) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            } else if (direction == SwipeToDismissBoxValue.StartToEnd && onLock != null) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Lock", tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    ) {
        content()
    }
}

@Composable
fun ListView(folders: List<FolderItem>, notes: List<NoteEntity>, images: List<ImageEntity>, tables: List<com.example.data.entities.TableEntity>, showCompact: Boolean, onFolderClick: (FolderEntity) -> Unit, onNoteClick: (Long) -> Unit, onImageClick: (ImageEntity) -> Unit, onTableClick: (Long) -> Unit, onDelete: (Any) -> Unit, onLock: (FolderEntity) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
        items(folders) { folderItem ->
            SwipeableItem(onDelete = { onDelete(folderItem.folder) }, onLock = { onLock(folderItem.folder) }) {
                FolderListRow(folderItem, onClick = { onFolderClick(folderItem.folder) }, onDelete = { onDelete(folderItem.folder) })
            }
        }
        items(tables) { table ->
            SwipeableItem(onDelete = { onDelete(table) }) {
                TableListRow(table, onClick = { onTableClick(table.id) })
            }
        }
        items(notes) { note ->
            SwipeableItem(onDelete = { onDelete(note) }) {
                NoteListRow(note, showCompact, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
            }
        }
        items(images) { image ->
            SwipeableItem(onDelete = { onDelete(image) }) {
                ImageListRow(image, onClick = { onImageClick(image) })
            }
        }
    }
}

@Composable
fun GridView(folders: List<FolderItem>, notes: List<NoteEntity>, images: List<ImageEntity>, tables: List<com.example.data.entities.TableEntity>, showCompact: Boolean, onFolderClick: (FolderEntity) -> Unit, onNoteClick: (Long) -> Unit, onImageClick: (ImageEntity) -> Unit, onTableClick: (Long) -> Unit, onDelete: (Any) -> Unit, onLock: (FolderEntity) -> Unit) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalItemSpacing = 16.dp
    ) {
        items(folders) { folderItem ->
            SwipeableItem(onDelete = { onDelete(folderItem.folder) }, onLock = { onLock(folderItem.folder) }) {
                FolderGridCard(folderItem, onClick = { onFolderClick(folderItem.folder) }, onDelete = { onDelete(folderItem.folder) })
            }
        }
        items(tables) { table ->
            SwipeableItem(onDelete = { onDelete(table) }) {
                TableGridCard(table, onClick = { onTableClick(table.id) })
            }
        }
        items(notes) { note ->
            SwipeableItem(onDelete = { onDelete(note) }) {
                NoteGridCard(note, showCompact, onClick = { onNoteClick(note.id) }, onDelete = { onDelete(note) })
            }
        }
        items(images) { image ->
            SwipeableItem(onDelete = { onDelete(image) }) {
                ImageGridCard(image, onClick = { onImageClick(image) })
            }
        }
    }
}

@Composable
fun TableView(folders: List<FolderItem>, notes: List<NoteEntity>, images: List<ImageEntity>, tables: List<com.example.data.entities.TableEntity>, showCompact: Boolean, onFolderClick: (FolderEntity) -> Unit, onNoteClick: (Long) -> Unit, onImageClick: (ImageEntity) -> Unit, onTableClick: (Long) -> Unit, onDelete: (Any) -> Unit, onLock: (FolderEntity) -> Unit) {
    // Falls back to list for simplicity of table view logic in gestures
    ListView(folders, notes, images, tables, showCompact, onFolderClick, onNoteClick, onImageClick, onTableClick, onDelete, onLock)
}

@Composable
fun TableListRow(table: com.example.data.entities.TableEntity, onClick: () -> Unit) {
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        alpha = 0.9f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha=0.3f), RoundedCornerShape(20.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                    Text("Modified ${formatShortDate(table.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.7f))
                }
            }
        }
    }
}

@Composable
fun TableGridCard(table: com.example.data.entities.TableEntity, onClick: () -> Unit) {
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        alpha = 0.9f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha=0.3f), RoundedCornerShape(24.dp)).padding(16.dp)
        ) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.tertiary)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }
}

@Composable
fun ImageListRow(image: ImageEntity, onClick: () -> Unit) {
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        alpha = 0.5f
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = image.uri,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text("Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatShortDate(image.createdAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ImageGridCard(image: ImageEntity, onClick: () -> Unit) {
    AsyncImage(
        model = image.uri,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
    )
}

@Composable
fun EmptyState() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("No items here", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun FolderListRow(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        alpha = 0.6f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().border(1.dp, fColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).background(fColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = fColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor)
                    if (folderItem.folder.isLocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = fColor, modifier = Modifier.size(16.dp))
                    }
                }
                Text("${folderItem.totalChildren} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun NoteListRow(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        alpha = 0.9f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Modified ${formatShortDate(note.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showCompact && note.previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(note.previewText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun FolderGridCard(folderItem: FolderItem, onClick: () -> Unit, onDelete: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        alpha = 0.6f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, fColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp)).padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.size(48.dp).background(fColor.copy(alpha = 0.15f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = fColor)
                }
                if (folderItem.folder.isLocked) {
                    Icon(Icons.Outlined.Lock, contentDescription = "Locked", tint = fColor, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${folderItem.totalChildren} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NoteGridCard(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    com.example.ui.theme.GlassSurface(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        alpha = 0.9f
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(24.dp)).padding(16.dp)
        ) {
            Box(modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary)
            }
            if (showCompact && note.previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(note.previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
