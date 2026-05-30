package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entities.CellEntity
import com.example.data.entities.ColumnEntity
import com.example.data.entities.ColumnType
import com.example.data.entities.RowEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableScreen(
    viewModel: TableViewModel,
    tableId: Long,
    onNavigateBack: () -> Unit,
    settingsManager: com.example.data.SettingsManager
) {
    LaunchedEffect(tableId) {
        viewModel.loadTable(tableId)
    }

    val fullTable by viewModel.fullTable.collectAsStateWithLifecycle()
    val table = fullTable?.table
    val columns = fullTable?.columns?.sortedBy { it.displayOrder } ?: emptyList()
    val rows = fullTable?.rows?.map { it.row }?.sortedBy { it.displayOrder } ?: emptyList()
    val cells = fullTable?.rows?.flatMap { it.cells } ?: emptyList()

    val gridStyle by settingsManager.tableGridStyle.collectAsStateWithLifecycle(com.example.data.TableGridStyle.SUBTLE_LINES)

    var showAddColumnDialog by remember { mutableStateOf(false) }

    if (table == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var isEditingTitle by remember { mutableStateOf(false) }
                    var titleText by remember { mutableStateOf(table.title) }

                    if (isEditingTitle) {
                        BasicTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            onTextLayout = {},
                            modifier = Modifier.fillMaxWidth(0.6f),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    innerTextField()
                                }
                            }
                        )
                        LaunchedEffect(Unit) {
                            // save on unfocus or similar, for now just simple
                        }
                        DisposableEffect(Unit) {
                            onDispose {
                                if (titleText != table.title) viewModel.updateTableTitle(titleText)
                            }
                        }
                    } else {
                        Text(
                            text = table.title,
                            modifier = Modifier.clickable { isEditingTitle = true }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val horizontalScrollState = rememberScrollState()

            Column(modifier = Modifier.fillMaxSize()) {
                // Table implementation using LazyColumn for rows, and horizontal scroll for columns
                Box(modifier = Modifier.weight(1f).horizontalScroll(horizontalScrollState)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 100.dp, start = 16.dp, end = 16.dp, top = 16.dp)
                    ) {
                    // Header Row
                    item {
                        TableRowContainer(style = gridStyle, isHeader = true) {
                            columns.forEach { column ->
                                val cellWidth = if (column.type == ColumnType.LONG_TEXT) 300.dp else 150.dp
                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .padding(8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = column.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Box(modifier = Modifier.width(100.dp).padding(8.dp)) {
                                val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                                val hapticIntensity by settingsManager.hapticIntensity.collectAsStateWithLifecycle(com.example.data.HapticIntensity.LIGHT)
                                TextButton(
                                    onClick = { 
                                        if (hapticIntensity != com.example.data.HapticIntensity.OFF) {
                                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        }
                                        showAddColumnDialog = true 
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Column", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Column")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                        // Data Rows
                        items(items = rows, key = { it.id }) { row ->
                            SwipeToDismissRow(
                                onDismiss = { viewModel.deleteRow(row) }
                            ) {
                                TableRowContainer(style = gridStyle, isHeader = false) {
                                    columns.forEach { column ->
                                        val cellWidth = if (column.type == ColumnType.LONG_TEXT) 300.dp else 150.dp
                                        val cell = cells.find { it.rowId == row.id && it.columnId == column.id }
                                        val cellValue = cell?.value ?: ""

                                        Box(
                                            modifier = Modifier
                                                .width(cellWidth)
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            EditableCell(
                                                value = cellValue,
                                                columnType = column.type,
                                                onValueChange = { newValue ->
                                                    viewModel.updateCell(row.id, column.id, newValue)
                                                }
                                            )
                                        }
                                    }
                                    // Empty space for the "Add Column" alignment
                                    Spacer(modifier = Modifier.width(100.dp))
                                }
                            }
                        }
                        
                        // Add Row Button
                        item {
                            Spacer(Modifier.height(8.dp))
                            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
                            val hapticIntensity by settingsManager.hapticIntensity.collectAsStateWithLifecycle(com.example.data.HapticIntensity.LIGHT)
                            TextButton(
                                onClick = { 
                                    if (hapticIntensity != com.example.data.HapticIntensity.OFF) {
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    }
                                    viewModel.addRow() 
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Row")
                                Spacer(Modifier.width(8.dp))
                                Text("Add Row")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddColumnDialog) {
        AddColumnDialog(
            onDismiss = { showAddColumnDialog = false },
            onConfirm = { name, type ->
                viewModel.addColumn(name, type)
                showAddColumnDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissRow(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                MaterialTheme.colorScheme.errorContainer
            } else Color.Transparent
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .background(color, RoundedCornerShape(8.dp))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
        content = {
            Box(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    )
}

@Composable
fun TableRowContainer(
    style: com.example.data.TableGridStyle,
    isHeader: Boolean,
    content: @Composable RowScope.() -> Unit
) {
    val backgroundColor = if (isHeader) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f) 
                          else if (style == com.example.data.TableGridStyle.CARDS) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.1f)
                          else Color.Transparent
                          
    val shape = if (style == com.example.data.TableGridStyle.CARDS) RoundedCornerShape(12.dp) else RoundedCornerShape(4.dp)
    val border = if (style == com.example.data.TableGridStyle.SUBTLE_LINES && !isHeader) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) 
                 else if (style == com.example.data.TableGridStyle.CARDS && !isHeader) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                 else null

    Surface(
        color = backgroundColor,
        shape = shape,
        border = border,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

@Composable
fun EditableCell(
    value: String,
    columnType: ColumnType,
    onValueChange: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    
    if (isEditing) {
        when (columnType) {
            ColumnType.LONG_TEXT -> {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    visualTransformation = MarkdownVisualTransformation(MaterialTheme.colorScheme.primary),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
            ColumnType.NUMBER -> {
                BasicTextField(
                    value = value,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^-?\\d*\\.?\\d*$"))) {
                            onValueChange(newValue)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }
            ColumnType.SELECT -> {
                var expanded by remember { mutableStateOf(true) }
                Box {
                    Text(
                        text = if (value.isEmpty()) "Select..." else value,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = true }
                            .padding(8.dp)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { 
                            expanded = false
                            isEditing = false
                        }
                    ) {
                        listOf("To Do", "In Progress", "Done", "Review").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    onValueChange(option)
                                    expanded = false
                                    isEditing = false
                                }
                            )
                        }
                    }
                }
            }
            else -> {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp)
                .clickable { isEditing = true }
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text("", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.5f))
            } else {
                if (columnType == ColumnType.LONG_TEXT) {
                    MarkdownText(text = value, modifier = Modifier.padding(1.dp), onNoteLinkClick = {})
                } else if (columnType == ColumnType.SELECT) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = value,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(value, color = MaterialTheme.colorScheme.onSurface, maxLines = if (columnType == ColumnType.TEXT) 1 else 1)
                }
            }
        }
    }
}

@Composable
fun AddColumnDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ColumnType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ColumnType.TEXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Column") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Column Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Column Type", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(8.dp))
                
                ColumnType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(type.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, selectedType) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
