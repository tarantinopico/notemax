package com.example.ui

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    var showDiscardDialog by remember { mutableStateOf(false) }

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
        
        val replacement = if (selectedText.isEmpty() && suffix.isNotEmpty()) {
            "text"
        } else {
            selectedText
        }
        
        val newText = text.substring(0, start) + prefix + replacement + suffix + text.substring(end)
        
        val newSelectionStart = start + prefix.length
        val newSelectionEnd = newSelectionStart + replacement.length
        
        textState = textState.copy(
            text = newText,
            selection = TextRange(newSelectionStart, newSelectionEnd)
        )
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolbarButton(text = "H1", onClick = { insertFormatting("# ", "") })
                        ToolbarButton(text = "H2", onClick = { insertFormatting("## ", "") })
                        ToolbarButton(text = "H3", onClick = { insertFormatting("### ", "") })
                        
                        HorizontalDivider(
                            modifier = Modifier.height(24.dp).width(1.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        
                        IconButton(onClick = { insertFormatting("**", "**") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.FormatBold, "Bold", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { insertFormatting("<u>", "</u>") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.FormatUnderlined, "Underline", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { insertFormatting("*", "*") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.FormatItalic, "Italic", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.height(24.dp).width(1.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                        
                        IconButton(onClick = { insertFormatting("- ", "") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.FormatListBulleted, "Bulleted List", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { insertFormatting("1. ", "") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.FormatListNumbered, "Numbered List", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.height(24.dp).width(1.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )

                        IconButton(onClick = { insertFormatting("[", "](https://)") }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Link, "Web Link", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.AttachFile, "Attach File", tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Elegant Native Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = handleBack,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Close else Icons.Default.ArrowBack, 
                        contentDescription = "Back or Cancel", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isEditing) {
                    Button(
                        onClick = {
                            viewModel.updateNote(editTitle.ifBlank { "Untitled Note" }, textState.text, editAttachedFileUri)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.SemiBold)
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
                    .padding(horizontal = 24.dp)
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
                        style = MaterialTheme.typography.bodyMedium, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }

                val fileUri = if (isEditing) editAttachedFileUri else note!!.attachedFileUri
                
                if (fileUri != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(16.dp)
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
                                Icon(Icons.Default.OpenInNew, contentDescription = "Open File", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                if (isEditing) {
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 26.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(bottom = 32.dp),
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
                    MarkdownText(text = note!!.content, modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp))
                }
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard changes?", fontWeight = FontWeight.Bold) },
            shape = RoundedCornerShape(24.dp),
            text = { Text("You have unsaved changes. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isEditing = false
                    editTitle = note!!.title
                    textState = TextFieldValue(note!!.content)
                    editAttachedFileUri = note!!.attachedFileUri
                }) { Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
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
