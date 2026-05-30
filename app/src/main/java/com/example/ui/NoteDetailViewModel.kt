package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteMaxRepository
import com.example.data.entities.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteDetailViewModel(private val repository: NoteMaxRepository) : ViewModel() {

    private val _noteId = MutableStateFlow<Long?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val note: StateFlow<NoteEntity?> = _noteId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getNoteFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadNote(id: Long) {
        _noteId.value = id
    }

    fun updateNote(title: String, content: String, attachedFileUri: String?) {
        viewModelScope.launch {
            note.value?.let { currentNote ->
                repository.updateNote(
                    currentNote.copy(
                        title = title,
                        content = content,
                        attachedFileUri = attachedFileUri,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }
}
