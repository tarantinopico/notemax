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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DirectoryViewModel(private val repository: NoteMaxRepository) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId
    
    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders = _currentFolderId.flatMapLatest { parentId ->
        repository.getFoldersWithCounts(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = _currentFolderId.flatMapLatest { parentId ->
        repository.getNotes(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFolder = _currentFolderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getFolderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }
    
    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertFolder(
                FolderEntity(name = name, parentFolderId = _currentFolderId.value, createdAt = now, updatedAt = now)
            )
            updateParentFolderTimestamp()
        }
    }

    fun createNote(title: String, content: String = "") {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repository.insertNote(
                NoteEntity(title = title, content = content, parentFolderId = _currentFolderId.value, createdAt = now, updatedAt = now)
            )
            updateParentFolderTimestamp()
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.deleteFolder(folder)
            updateParentFolderTimestamp()
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
            updateParentFolderTimestamp()
        }
    }
    
    private suspend fun updateParentFolderTimestamp() {
        _currentFolderId.value?.let { parentId ->
            repository.getFolderById(parentId)?.let { 
                repository.updateFolder(it.copy(updatedAt = System.currentTimeMillis())) 
            }
        }
    }
}
