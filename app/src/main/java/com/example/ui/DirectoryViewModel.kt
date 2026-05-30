package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteMaxRepository
import com.example.data.entities.FolderEntity
import com.example.data.entities.NoteEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DirectoryViewModel(private val repository: NoteMaxRepository) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders = _currentFolderId.flatMapLatest { parentId ->
        repository.getFolders(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = _currentFolderId.flatMapLatest { parentId ->
        repository.getNotes(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFolder = _currentFolderId.flatMapLatest { id ->
        if (id == null) kotlinx.coroutines.flow.flowOf(null) else repository.getFolderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.insertFolder(
                FolderEntity(name = name, parentFolderId = _currentFolderId.value)
            )
        }
    }

    fun createNote(title: String, content: String = "") {
        viewModelScope.launch {
            repository.insertNote(
                NoteEntity(title = title, content = content, parentFolderId = _currentFolderId.value)
            )
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}
