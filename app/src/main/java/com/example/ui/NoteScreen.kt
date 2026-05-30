package com.example.ui

import com.example.ui.theme.toHex
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteDetailViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToNote: (Long) -> Unit
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var editAttachedFileUri by remember { mutableStateOf<String?>(null) }
    
    // Drawing State
    var isSketchMode by remember { mutableStateOf(false) }
    var activeTool by remember { mutableStateOf(ToolType.PEN) }
    var activeColor by remember { mutableStateOf(Color.Black) }
    var activeStrokeWidth by remember { mutableStateOf(8f) }
    var currentStrokes by remember { mutableStateOf<List<DrawingStroke>>(emptyList()) }
    var initialStrokesLoaded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val historyManager = remember { EditorHistory() }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var showNotePicker by remember { mutableStateOf(false) }
    var linkInsertPosition by remember { mutableStateOf(-1) }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                editAttachedFileUri = it.toString()
            } catch (e: SecurityException) {
                Toast.makeText(context, "Could not get permission for file", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(note) {
        if (!isEditing && note != null) {
            editTitle = note!!.title
            val initialText = TextFieldValue(note!!.content)
            textState = initialText
            editAttachedFileUri = note!!.attachedFileUri
            historyManager.push(initialText)
            
            if (!initialStrokesLoaded) {
                scope.launch(Dispatchers.IO) {
                    val parsed = DrawingSerializer.deserialize(note!!.drawingData)
                    withContext(Dispatchers.Main) {
                        currentStrokes = parsed
                        initialStrokesLoaded = true
                    }
                }
            }
        }
    }

    LaunchedEffect(editTitle, textState.text, editAttachedFileUri, currentStrokes) {
        if (isEditing && note != null) {
            scope.launch(Dispatchers.IO) {
                val serialized = DrawingSerializer.serialize(currentStrokes)
                withContext(Dispatchers.Main) {
                    viewModel.updateNoteDebounced(editTitle.ifBlank { "Untitled Note" }, textState.text, editAttachedFileUri, serialized)
                }
            }
        }
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        if (isEditing) {
            scope.launch(Dispatchers.IO) {
                val serialized = DrawingSerializer.serialize(currentStrokes)
                withContext(Dispatchers.Main) {
                    viewModel.updateNote(editTitle.ifBlank { "Untitled Note" }, textState.text, editAttachedFileUri, serialized)
                    isEditing = false
                    isSketchMode = false
                }
            }
        } else {
            onNavigateUp()
        }
    }

    fun insertFormatting(prefix: String, suffix: String = "") {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val start = textState.selection.min
        val end = textState.selection.max
        val text = textState.text
        
        val selectedText = text.substring(start, end)
        
        val replacement = if (selectedText.isEmpty() && suffix.isNotEmpty()) {
            "text"
        } else {
            selectedText
        }
        
        val newText = text.substring(0, start) + prefix + replacement + suffix + text.substring(end)
        
        val newSelectionStart = start + prefix.length
        val newSelectionEnd = newSelectionStart + replacement.length
        
        val newState = textState.copy(
            text = newText,
            selection = TextRange(newSelectionStart, newSelectionEnd)
        )
        textState = newState
        historyManager.push(newState)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = isEditing,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                        val effects = com.example.ui.theme.LocalVisualEffects.current
                        val surfaceAlpha = if (effects.uiTransparency) 0.65f else 0.85f
                        val glassmorphicMod = if (effects.glassmorphism) Modifier.blur(16.dp) else Modifier
                        
                        var activeCategory by remember { mutableStateOf(ToolbarCategory.STYLE) }
                        
                        LaunchedEffect(activeCategory) {
                            if (activeCategory == ToolbarCategory.CANVAS) {
                                isSketchMode = true
                            } else if (activeCategory != ToolbarCategory.CANVAS && isSketchMode) {
                                isSketchMode = false
                            }
                        }
                        
                        Column(modifier = glassmorphicMod) {
                            Crossfade(targetState = activeCategory, label = "toolbar_crossfade") { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when (category) {
                                        ToolbarCategory.STYLE -> {
                                            IconButton(onClick = { 
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                historyManager.undo()?.let { textState = it } 
                                            }, enabled = historyManager.canUndo, modifier = Modifier.size(40.dp)) {
                                                Icon(Icons.AutoMirrored.Filled.Undo, "Undo", tint = if (historyManager.canUndo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                            }
                                            IconButton(onClick = { historyManager.redo()?.let { textState = it } }, enabled = historyManager.canRedo, modifier = Modifier.size(40.dp)) {
                                                Icon(Icons.AutoMirrored.Filled.Redo, "Redo", tint = if (historyManager.canRedo) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                            }
                                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            ToolbarButton(text = "H1", onClick = { insertFormatting("# ", "") })
                                            ToolbarButton(text = "H2", onClick = { insertFormatting("## ", "") })
                                            ToolbarButton(text = "H3", onClick = { insertFormatting("### ", "") })
                                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            IconButton(onClick = { insertFormatting("**", "**") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.FormatBold, "Bold", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            IconButton(onClick = { insertFormatting("<u>", "</u>") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.FormatUnderlined, "Underline", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            IconButton(onClick = { insertFormatting("*", "*") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.FormatItalic, "Italic", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            IconButton(onClick = { insertFormatting("- ", "") }, modifier = Modifier.size(40.dp)) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bulleted List", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            IconButton(onClick = { insertFormatting("1. ", "") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.FormatListNumbered, "Numbered List", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        ToolbarCategory.INSERT -> {
                                            IconButton(onClick = { insertFormatting("[", "](https://)") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Link, "Web Link", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            IconButton(onClick = { insertFormatting("#", " ") }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.Tag, "Tag", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                            ToolbarButton(text = "@todo", onClick = { insertFormatting("@todo ", "") })
                                            ToolbarButton(text = "#important", onClick = { insertFormatting("#important ", "") })
                                            HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                            IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.size(40.dp)) { Icon(Icons.Default.AttachFile, "Attach File", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                                        }
                                        ToolbarCategory.CANVAS -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                ToolButton("Pen", ToolType.PEN == activeTool) { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); activeTool = ToolType.PEN 
                                                }
                                                ToolButton("Marker", ToolType.HIGHLIGHTER == activeTool) { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); activeTool = ToolType.HIGHLIGHTER 
                                                }
                                                ToolButton("Eraser", ToolType.ERASER == activeTool) { 
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); activeTool = ToolType.ERASER 
                                                }
                                                IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); currentStrokes = emptyList() }) {
                                                    Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
                                                }
                                                HorizontalDivider(modifier = Modifier.height(24.dp).width(1.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                                                val colors = listOf(Color.Black, Color.Red, Color.Blue, Color.Green, Color(0xFFFFA500), Color.Magenta)
                                                colors.forEach { color ->
                                                    Box(modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape).background(color).clickable { 
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress); activeColor = color 
                                                    }, contentAlignment = Alignment.Center) {
                                                        if (activeColor == color) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                        ToolbarCategory.STRUCTURE -> {
                                            ToolbarButton(text = "Table", onClick = { insertFormatting("\n| Header | Header |\n| --- | --- |\n| Cell | Cell |\n", "") })
                                            ToolbarButton(text = "Big Arrow", onClick = { insertFormatting("==>", "") })
                                            ToolbarButton(text = "Check Box", onClick = { insertFormatting("[ ] ", "") })
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                CategoryTab("Style", ToolbarCategory.STYLE, activeCategory) { activeCategory = it }
                                CategoryTab("Insert", ToolbarCategory.INSERT, activeCategory) { activeCategory = it }
                                CategoryTab("Canvas", ToolbarCategory.CANVAS, activeCategory) { activeCategory = it }
                                CategoryTab("Structure", ToolbarCategory.STRUCTURE, activeCategory) { activeCategory = it }
                            }
                        }
                    }
                }
            }
        
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack, 
                        contentDescription = "Back or Cancel", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isEditing) {
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val serialized = DrawingSerializer.serialize(currentStrokes)
                                withContext(Dispatchers.Main) {
                                    viewModel.updateNote(editTitle.ifBlank { "Untitled Note" }, textState.text, editAttachedFileUri, serialized)
                                    isEditing = false
                                    isSketchMode = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Finish Editing", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Note", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (isEditing) {
                    BasicTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        decorationBox = { innerTextField ->
                            if (editTitle.isEmpty()) {
                                Text("Note Title", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            innerTextField()
                        }
                    )
                } else {
                    Text(
                        text = note!!.title,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        "Created ${formatShortDate(note!!.createdAt)} • Edited ${formatShortDate(note!!.updatedAt)}", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                val fileUri = if (isEditing) editAttachedFileUri else note!!.attachedFileUri
                
                if (fileUri != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(Modifier.width(16.dp))
                        
                        if (isEditing) {
                            Text(fileUri.substringAfterLast("/").takeIf { it.isNotBlank() } ?: "Attached File", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { editAttachedFileUri = null }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Remove File", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text("Open Attached File", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(fileUri)
                                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: ActivityNotFoundException) {
                                        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
                                    } catch (e: SecurityException) {
                                        Toast.makeText(context, "Cannot access this file", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open File", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 32.dp)
                ) {
                    if (isEditing) {
                        BasicTextField(
                            value = textState,
                            onValueChange = { newValue ->
                                var updatedValue = newValue
                                val oldText = textState.text
                                val newText = newValue.text
                                
                                if (newText.length > oldText.length) {
                                    val cursor = newValue.selection.start
                                    val rules = listOf(
                                        "->" to "→", "=>" to "⇒", "*>" to "✦", "[ ]" to "☐", "[x]" to "☑"
                                    )
                                    for ((trigger, replacement) in rules) {
                                        if (cursor >= trigger.length && newText.substring(cursor - trigger.length, cursor) == trigger) {
                                            val replaced = newText.substring(0, cursor - trigger.length) + replacement + newText.substring(cursor)
                                            updatedValue = TextFieldValue(replaced, TextRange(cursor - trigger.length + replacement.length))
                                            break
                                        }
                                    }
                                    if (cursor >= 2 && newText.substring(cursor - 2, cursor) == "[[") {
                                        linkInsertPosition = cursor
                                        showNotePicker = true
                                    }
                                }
                                textState = updatedValue
                                historyManager.push(updatedValue)
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground,
                                lineHeight = 26.sp
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = MarkdownVisualTransformation(MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            decorationBox = { innerTextField ->
                                if (textState.text.isEmpty()) {
                                    Text(
                                        "Start writing... (Markdown supported)", 
                                        style = MaterialTheme.typography.bodyLarge, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        MarkdownText(
                            text = note!!.content, 
                            modifier = Modifier.fillMaxWidth(),
                            onNoteLinkClick = { title ->
                                scope.launch {
                                    val id = viewModel.findNoteIdByTitle(title)
                                    if (id != null) {
                                        onNavigateToNote(id)
                                    } else {
                                        Toast.makeText(context, "Note not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    
                    if (isSketchMode || currentStrokes.isNotEmpty()) {
                        SketchLayer(
                            modifier = Modifier.matchParentSize(),
                            strokes = currentStrokes,
                            isEditable = isSketchMode,
                            onStrokeAdded = { stroke -> currentStrokes = currentStrokes + stroke },
                            activeTool = activeTool,
                            activeColor = activeColor,
                            activeStrokeWidth = activeStrokeWidth
                        )
                    }
                }
            }
        }
    }

    if (showNotePicker) {
        NotePickerBottomSheet(
            notes = allNotes,
            onNoteSelected = { selectedNote ->
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                val insertText = "${selectedNote.title}]]"
                val prefix = textState.text.substring(0, linkInsertPosition)
                val suffix = textState.text.substring(linkInsertPosition)
                val newText = prefix + insertText + suffix
                val newCursor = linkInsertPosition + insertText.length
                val newState = TextFieldValue(newText, TextRange(newCursor))
                textState = newState
                historyManager.push(newState)
                showNotePicker = false
            },
            onDismiss = { showNotePicker = false }
        )
    }

    if (showTagDialog) {
        var tagText by remember { mutableStateOf("") }
        var selectedBgColor by remember { mutableStateOf(Color(0xFF4CAF50)) }
        var selectedFgColor by remember { mutableStateOf(Color.White) }
        val colors = listOf(Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFF44336), Color(0xFFFF9800), Color(0xFF9C27B0))
        
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Create Custom Tag") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text("Tag Content") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Tag Color", style = MaterialTheme.typography.labelLarge)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        colors.forEach { c ->
                            Box(modifier = Modifier.size(36.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c).clickable {
                                selectedBgColor = c
                            }, contentAlignment = Alignment.Center) {
                                if (selectedBgColor == c) Icon(Icons.Default.Check, null, tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tagText.isNotBlank()) {
                        val hexBg = selectedBgColor.toHex()
                        val hexFg = selectedFgColor.toHex()
                        insertFormatting("$\$TAG_BEGIN$\$color:$hexBg,textColor:$hexFg,text:$tagText$\$TAG_END$\$", "")
                    }
                    showTagDialog = false
                }) { Text("Insert Tag") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Go back?", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(24.dp),
            text = { Text("Are you sure you want to exit editing mode?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isEditing = false
                }) { Text("Exit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }
}

enum class ToolbarCategory { STYLE, INSERT, CANVAS, STRUCTURE }

@Composable
fun CategoryTab(text: String, category: ToolbarCategory, activeCategory: ToolbarCategory, onSelect: (ToolbarCategory) -> Unit) {
    val haptic = LocalHapticFeedback.current
    val isSelected = category == activeCategory
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(category) 
            }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text, 
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ToolbarButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
