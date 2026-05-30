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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
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
    var editContent by remember { mutableStateOf("") }
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
            editContent = note!!.content
            editAttachedFileUri = note!!.attachedFileUri
        }
    }

    if (note == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            CircularProgressIndicator()
        }
        return
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(note!!.title)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Attach File")
                        }
                        IconButton(onClick = {
                            viewModel.updateNote(editTitle, editContent, editAttachedFileUri)
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Done, contentDescription = "Save")
                        }
                        IconButton(onClick = {
                            editTitle = note!!.title
                            editContent = note!!.content
                            editAttachedFileUri = note!!.attachedFileUri
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val fileUri = if (isEditing) editAttachedFileUri else note!!.attachedFileUri
            
            if (fileUri != null) {
                if (isEditing) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Attached: $fileUri", modifier = Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { editAttachedFileUri = null }) {
                            Icon(Icons.Default.Clear, contentDescription = "Remove File")
                        }
                    }
                } else {
                    val linkText = buildAnnotatedString {
                        pushStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline))
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
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (isEditing) {
                OutlinedTextField(
                    value = editContent,
                    onValueChange = { editContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    placeholder = { Text("Note content (Markdown supported)") },
                    minLines = 10
                )
            } else {
                MarkdownText(text = note!!.content)
            }
        }
    }
}
