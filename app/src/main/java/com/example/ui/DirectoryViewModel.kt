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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DirectoryViewModel(private val repository: NoteMaxRepository) : ViewModel() {

    private val _currentFolderId = MutableStateFlow<Long?>(null)
    val currentFolderId: StateFlow<Long?> = _currentFolderId
    
    private val _globalViewMode = MutableStateFlow(ViewMode.LIST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentFolder = _currentFolderId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getFolderFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val viewMode: StateFlow<ViewMode> = combine(_globalViewMode, currentFolder) { global, folder ->
        folder?.defaultViewModeString?.let {
            try { ViewMode.valueOf(it) } catch (e: Exception) { null }
        } ?: global
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewMode.LIST)

    @OptIn(ExperimentalCoroutinesApi::class)
    val folders = _currentFolderId.flatMapLatest { parentId ->
        repository.getFoldersWithCounts(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes = _currentFolderId.flatMapLatest { parentId ->
        repository.getNotes(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }
    
    fun setViewMode(mode: ViewMode) {
        val folder = currentFolder.value
        if (folder?.defaultViewModeString != null) {
            viewModelScope.launch {
                repository.updateFolder(folder.copy(defaultViewModeString = mode.name, updatedAt = System.currentTimeMillis()))
            }
        } else {
            _globalViewMode.value = mode
        }
    }

    fun updateFolderSettings(color: Long?, iconName: String?, defaultViewModeString: String?, showCompactPreviews: Boolean) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch {
            repository.updateFolder(
                folder.copy(
                    color = color,
                    iconName = iconName,
                    defaultViewModeString = defaultViewModeString,
                    showCompactPreviews = showCompactPreviews,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
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
                NoteEntity(title = title, content = content, previewText = StringUtils.extractPreviewText(content), parentFolderId = _currentFolderId.value, createdAt = now, updatedAt = now)
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
