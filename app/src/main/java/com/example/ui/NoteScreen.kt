package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    viewModel: NoteDetailViewModel,
    onNavigateUp: () -> Unit
) {
    val note by viewModel.note.collectAsStateWithLifecycle()
    var isEditing by remember { mutableStateOf(false) }

    var editTitle by remember { mutableStateOf("") }
    var textState by remember { mutableStateOf(TextFieldValue("")) }
    var editAttachedFileUri by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current

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

    LaunchedEffect(note) {
        if (!isEditing && note != null) {
            editTitle = note!!.title
            textState = TextFieldValue(note!!.content)
            editAttachedFileUri = note!!.attachedFileUri
        }
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }

    val handleBack = {
        if (isEditing) {
            if (editTitle != note!!.title || textState.text != note!!.content || editAttachedFileUri != note!!.attachedFileUri) {
                showDiscardDialog = true
            } else {
                isEditing = false
            }
        } else {
            onNavigateUp()
        }
    }

    fun insertFormatting(prefix: String, suffix: String = "") {
        val start = textState.selection.min
        val end = textState.selection.max
        val text = textState.text
        
        val selectedText = text.substring(start, end)
        val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        val newSelectionStart = start + prefix.length + selectedText.length
        
        textState = textState.copy(
            text = newText,
            selection = TextRange(newSelectionStart, newSelectionStart)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (isEditing) {
                        OutlinedTextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            placeholder = { Text("Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                        )
                    } else {
                        Text(note!!.title)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back or Cancel")
                    }
                },
                actions = {
                    if (isEditing) {
                        Box {
                            IconButton(onClick = { showInsertMenu = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Insert Formatting")
                            }
                            DropdownMenu(expanded = showInsertMenu, onDismissRequest = { showInsertMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Heading 1") },
                                    onClick = { insertFormatting("# "); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Heading 2") },
                                    onClick = { insertFormatting("## "); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Heading 3") },
                                    onClick = { insertFormatting("### "); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bold") },
                                    onClick = { insertFormatting("**", "**"); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatBold, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Underline") },
                                    onClick = { insertFormatting("<u>", "</u>"); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatUnderlined, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("File Link") },
                                    onClick = { insertFormatting("[File Link](file://your_local_path)"); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Web Link") },
                                    onClick = { insertFormatting("[Web Link](https://)"); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Numbered List") },
                                    onClick = { insertFormatting("1. "); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null) }
                                )
                                DropdownMenuItem(
                                    text = { Text("Bulleted List") },
                                    onClick = { insertFormatting("- "); showInsertMenu = false },
                                    leadingIcon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) }
                                )
                            }
                        }
                        IconButton(onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }) {
                            Icon(Icons.Default.AttachFile, contentDescription = "Attach File directly")
                        }
                        IconButton(onClick = {
                            viewModel.updateNote(editTitle, textState.text, editAttachedFileUri)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Save Note")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Note")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val fileUri = if (isEditing) editAttachedFileUri else note!!.attachedFileUri
            
            if (!isEditing) {
                Text("Created: ${formatShortDate(note!!.createdAt)} • Edited: ${formatShortDate(note!!.updatedAt)}", 
                     style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                     modifier = Modifier.padding(bottom = 16.dp))
            }

            if (fileUri != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant, 
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    if (isEditing) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                            Icon(Icons.Default.AttachFile, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Attached: $fileUri", modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { editAttachedFileUri = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Remove File")
                            }
                        }
                    } else {
                        val linkText = buildAnnotatedString {
                            pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium))
                            append("Attached File: $fileUri")
                            pop()
                        }
                        ClickableText(
                            text = linkText,
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
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = textState,
                    onValueChange = { textState = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    placeholder = { Text("Note content (Markdown supported)") },
                    minLines = 15,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
            } else {
                MarkdownText(text = note!!.content, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isEditing = false
                    editTitle = note!!.title
                    textState = TextFieldValue(note!!.content)
                    editAttachedFileUri = note!!.attachedFileUri
                }) { Text("Discard", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }
}
