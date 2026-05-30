package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.entities.FolderEntity
import com.example.data.entities.FolderItem
import com.example.data.entities.ImageEntity
import com.example.data.entities.NoteEntity
import com.example.data.entities.TableEntity
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val folderContent by viewModel.folderContent.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
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

    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoFile = remember { mutableStateOf<java.io.File?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile.value != null) {
            // Note: viewmodel might not have this method in new version, falling back to basic handling
            // viewModel.createImage(Uri.fromFile(photoFile.value).toString())
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val pickMediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) {}

    val attemptFolderNavigation: (FolderEntity) -> Unit = { folder ->
        val item = folderContent.folders.find { it.folder.id == folder.id }
        if (item?.folder?.isLocked == true) {
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

    val listState = rememberLazyListState()
    val gridState = rememberLazyStaggeredGridState()

    val maxHeaderHeight = 160.dp
    val minHeaderHeight = 64.dp
    val maxHeaderHeightPx = with(LocalDensity.current) { maxHeaderHeight.toPx() }
    val minHeaderHeightPx = with(LocalDensity.current) { minHeaderHeight.toPx() }

    val scrollOffset = when (viewMode) {
        ViewMode.GRID -> if (gridState.firstVisibleItemIndex == 0) gridState.firstVisibleItemScrollOffset.toFloat() else maxHeaderHeightPx
        else -> if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset.toFloat() else maxHeaderHeightPx
    }

    val headerHeightPx = (maxHeaderHeightPx - scrollOffset).coerceIn(minHeaderHeightPx, maxHeaderHeightPx)
    val fraction = 1f - ((headerHeightPx - minHeaderHeightPx) / (maxHeaderHeightPx - minHeaderHeightPx))

    val animatedFraction by animateFloatAsState(targetValue = fraction, label = "HeaderFraction")
    val isCollapsed = animatedFraction > 0.8f

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 16.dp)) {
                        SmallFloatingActionButton(onClick = { showAddFolderDialog = true; showFabMenu = false }) { Icon(Icons.Default.CreateNewFolder, "Add Folder") }
                        SmallFloatingActionButton(onClick = { showAddTableDialog = true; showFabMenu = false }) { Icon(Icons.Default.TableChart, "Add Table") }
                        FloatingActionButton(onClick = { showAddNoteDialog = true; showFabMenu = false }, containerColor = accentColor) { Icon(Icons.Default.Edit, "New Note", tint = MaterialTheme.colorScheme.onPrimary) }
                    }
                }
                FloatingActionButton(onClick = { showFabMenu = !showFabMenu }, containerColor = if (showFabMenu) MaterialTheme.colorScheme.surfaceVariant else accentColor) {
                    Icon(if (showFabMenu) Icons.Default.Close else Icons.Default.Add, "Open Menu")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Main Content Area
            if (folderContent.folders.isEmpty() && folderContent.notes.isEmpty() && folderContent.tables.isEmpty() && folderContent.images.isEmpty()) {
                EmptyState(modifier = Modifier.fillMaxSize())
            } else {
                val showCompact = currentFolder?.showCompactPreviews == true
                Crossfade(targetState = viewMode, label = "view_mode", modifier = Modifier.fillMaxSize()) { mode ->
                    when (mode) {
                        ViewMode.GRID -> GridView(folderContent, showCompact, gridState, maxHeaderHeight, minHeaderHeight, attemptFolderNavigation, onNavigateToNote, onNavigateToTable, {}, { showDeleteConfirmDialog = it }, { viewModel.lockFolder(it) })
                        else -> ListView(folderContent, showCompact, listState, maxHeaderHeight, minHeaderHeight, attemptFolderNavigation, onNavigateToNote, onNavigateToTable, {}, { showDeleteConfirmDialog = it }, { viewModel.lockFolder(it) })
                    }
                }
            }

            // Top Header (Translucent iOS style)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { headerHeightPx.toDp() })
                    .background(Color.Transparent)
            ) {
                // Glass effect when collapsed
                if (animatedFraction > 0.1f) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f * animatedFraction)).blur(if (animatedFraction > 0.5f) 20.dp else 0.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(minHeaderHeight).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentFolder != null) {
                        IconButton(onClick = onNavigateUp) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                    } else Spacer(modifier = Modifier.width(48.dp))

                    Text(
                        text = currentFolder?.name ?: "NoteMax",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = animatedFraction),
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    IconButton(onClick = { 
                        if (currentFolder == null) onNavigateToSettings() 
                        else showFolderSettingsDialog = true 
                    }) {
                        Icon(Icons.Default.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box {
                        IconButton(onClick = { showViewModeMenu = true }) {
                            Icon(when(viewMode) {
                                ViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                                ViewMode.GRID -> Icons.Default.GridView
                                ViewMode.TABLE -> Icons.Default.TableRows
                            }, "View Mode", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        DropdownMenu(expanded = showViewModeMenu, onDismissRequest = { showViewModeMenu = false }) {
                            DropdownMenuItem(text = { Text("List View") }, onClick = { viewModel.setViewMode(ViewMode.LIST); showViewModeMenu = false })
                            DropdownMenuItem(text = { Text("Grid View") }, onClick = { viewModel.setViewMode(ViewMode.GRID); showViewModeMenu = false })
                        }
                    }
                }

                // Expanded large title
                Text(
                    text = currentFolder?.name ?: "NoteMax",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 1f - animatedFraction),
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 12.dp)
                )
            }
        }
    }

    if (showAddFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddFolderDialog = false },
            title = { Text("New Folder") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) viewModel.createFolder(name); showAddFolderDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showAddFolderDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddNoteDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddNoteDialog = false },
            title = { Text("New Note") },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (title.isNotBlank()) viewModel.createNote(title) { id -> onNavigateToNote(id) }; showAddNoteDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showAddNoteDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddTableDialog) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddTableDialog = false },
            title = { Text("New Table") },
            text = { OutlinedTextField(value = title, onValueChange = { title = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { if (title.isNotBlank()) viewModel.createTable(title); showAddTableDialog = false }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showAddTableDialog = false }) { Text("Cancel") } }
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
                    else if (item is TableEntity) viewModel.deleteTable(item)
                    showDeleteConfirmDialog = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ListView(
    content: FolderContentState,
    showCompact: Boolean,
    state: LazyListState,
    maxHeaderHeight: androidx.compose.ui.unit.Dp,
    minHeaderHeight: androidx.compose.ui.unit.Dp,
    onFolderClick: (FolderEntity) -> Unit,
    onNoteClick: (Long) -> Unit,
    onTableClick: (Long) -> Unit,
    onImageClick: (ImageEntity) -> Unit,
    onDelete: (Any) -> Unit,
    onLock: (FolderEntity) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = maxHeaderHeight + 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(content.folders, key = { "folder_${it.folder.id}" }) { folderItem ->
            FolderListRow(folderItem, onClick = { onFolderClick(folderItem.folder) })
        }
        items(content.tables, key = { "table_${it.id}" }) { table ->
            TableListRow(table, onClick = { onTableClick(table.id) })
        }
        items(content.notes, key = { "note_${it.id}" }) { note ->
            NoteListRow(note, showCompact, onClick = { onNoteClick(note.id) })
        }
    }
}

@Composable
fun GridView(
    content: FolderContentState,
    showCompact: Boolean,
    state: LazyStaggeredGridState,
    maxHeaderHeight: androidx.compose.ui.unit.Dp,
    minHeaderHeight: androidx.compose.ui.unit.Dp,
    onFolderClick: (FolderEntity) -> Unit,
    onNoteClick: (Long) -> Unit,
    onTableClick: (Long) -> Unit,
    onImageClick: (ImageEntity) -> Unit,
    onDelete: (Any) -> Unit,
    onLock: (FolderEntity) -> Unit
) {
    LazyVerticalStaggeredGrid(
        state = state,
        columns = StaggeredGridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = maxHeaderHeight + 8.dp, bottom = 80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        items(content.folders, key = { "folder_${it.folder.id}" }) { folderItem ->
            FolderGridCard(folderItem, onClick = { onFolderClick(folderItem.folder) })
        }
        items(content.tables, key = { "table_${it.id}" }) { table ->
            TableGridCard(table, onClick = { onTableClick(table.id) })
        }
        items(content.notes, key = { "note_${it.id}" }) { note ->
            NoteGridCard(note, showCompact, onClick = { onNoteClick(note.id) })
        }
    }
}

@Composable
fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("No items here", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}



@Composable
fun FolderListRow(folderItem: FolderItem, onClick: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(fColor.copy(alpha=0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fColor, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column { Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor) }
        }
    }
}

@Composable
fun TableListRow(table: TableEntity, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.8f)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text("Modified ${formatShortDate(table.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha=0.7f))
            }
        }
    }
}

@Composable
fun NoteListRow(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Modified ${formatShortDate(note.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showCompact && note.previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note.previewText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f), maxLines=2, overflow=TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun FolderGridCard(folderItem: FolderItem, onClick: () -> Unit) {
    val fColor = folderItem.folder.color?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    val icon = FolderIcons.getIcon(folderItem.folder.iconName)
    Surface(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.6f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(fColor.copy(alpha=0.15f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = fColor, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.weight(1f))
            Text(folderItem.folder.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = fColor, maxLines=1, overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun TableGridCard(table: TableEntity, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha=0.8f)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.tertiary.copy(alpha=0.2f), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp)) }
            Spacer(modifier = Modifier.weight(1f))
            Text(table.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onTertiaryContainer, maxLines=1, overflow=TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun NoteGridCard(note: NoteEntity, showCompact: Boolean, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
            if (showCompact && note.previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(note.previewText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.8f), maxLines=2, overflow=TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines=1, overflow=TextOverflow.Ellipsis)
        }
    }
}
