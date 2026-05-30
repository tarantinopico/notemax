package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteMaxRepository
import com.example.data.entities.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(private val repository: NoteMaxRepository) : ViewModel() {
    private val _noteId = MutableStateFlow<Long?>(null)
    private var autoSaveJob: Job? = null
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    @OptIn(ExperimentalCoroutinesApi::class)
    val note: StateFlow<NoteEntity?> = _noteId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(null) else repository.getNoteFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadNote(id: Long) {
        _noteId.value = id
    }
    
    fun clearError() {
        _error.value = null
    }

    fun updateNoteDebounced(title: String, content: String, attachedFileUri: String?) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1500)
            saveNoteInternal(title, content, attachedFileUri)
        }
    }

    fun updateNote(title: String, content: String, attachedFileUri: String?) {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            saveNoteInternal(title, content, attachedFileUri)
        }
    }
    
    private suspend fun saveNoteInternal(title: String, content: String, attachedFileUri: String?) {
        try {
            val currentNote = note.value
            if (currentNote != null) {
                repository.updateNote(
                    currentNote.copy(
                        title = title,
                        content = content,
                        previewText = StringUtils.extractPreviewText(content),
                        attachedFileUri = attachedFileUri,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            _error.value = "Error saving note: ${e.localizedMessage}"
        }
    }
}
