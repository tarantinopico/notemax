package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteMaxRepository
import com.example.data.entities.FolderEntity
import com.example.data.entities.ImageEntity
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
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    fun clearError() { _error.value = null }

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
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val images = _currentFolderId.flatMapLatest { parentId ->
        repository.getImages(parentId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateToFolder(folderId: Long?) {
        _currentFolderId.value = folderId
    }

    fun setViewMode(mode: ViewMode) {
        val folder = currentFolder.value
        if (folder?.defaultViewModeString != null) {
            viewModelScope.launch {
                try {
                    repository.updateFolder(folder.copy(defaultViewModeString = mode.name, updatedAt = System.currentTimeMillis()))
                } catch(e: Exception) { _error.value = "Error updating view mode" }
            }
        } else {
            _globalViewMode.value = mode
        }
    }

    fun updateFolderSettings(color: Long?, iconName: String?, defaultViewModeString: String?, showCompactPreviews: Boolean, isLocked: Boolean, onSuccess: () -> Unit = {}) {
        val folder = currentFolder.value ?: return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                repository.updateFolder(
                    folder.copy(
                        color = color,
                        iconName = iconName,
                        defaultViewModeString = defaultViewModeString,
                        showCompactPreviews = showCompactPreviews,
                        isLocked = isLocked,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onSuccess()
                }
            } catch(e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _error.value = "Error saving folder settings"
                }
            }
        }
    }

    fun createImage(uri: String) {
        val parentId = _currentFolderId.value ?: return
        viewModelScope.launch {
            try {
                repository.insertImage(ImageEntity(uri = uri, parentFolderId = parentId))
                updateParentFolderTimestamp()
            } catch (e: Exception) {
                _error.value = "Error saving image"
            }
        }
    }

    fun deleteImage(image: ImageEntity) {
        viewModelScope.launch {
            try {
                repository.deleteImageById(image.id)
                updateParentFolderTimestamp()
            } catch (e: Exception) {
                _error.value = "Error deleting image"
            }
        }
    }

    fun lockFolder(folder: FolderEntity) {
        viewModelScope.launch {
            repository.updateFolder(folder.copy(isLocked = true, updatedAt = System.currentTimeMillis()))
        }
    }

    fun createFolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.insertFolder(
                    FolderEntity(name = name, parentFolderId = _currentFolderId.value, createdAt = now, updatedAt = now)
                )
                updateParentFolderTimestamp()
            } catch(e: Exception) { _error.value = "Error creating folder" }
        }
    }

    fun createNote(title: String, content: String = "") {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                repository.insertNote(
                    NoteEntity(title = title, content = content, previewText = StringUtils.extractPreviewText(content), parentFolderId = _currentFolderId.value, createdAt = now, updatedAt = now)
                )
                updateParentFolderTimestamp()
            } catch(e: Exception) { _error.value = "Error creating note: ${e.localizedMessage}" }
        }
    }

    fun deleteFolder(folder: FolderEntity) {
        viewModelScope.launch {
            try {
                repository.deleteFolder(folder)
                updateParentFolderTimestamp()
            } catch(e: Exception) { _error.value = "Error deleting folder" }
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            try {
                repository.deleteNote(note)
                updateParentFolderTimestamp()
            } catch(e: Exception) { _error.value = "Error deleting note" }
        }
    }

    private suspend fun updateParentFolderTimestamp() {
        val parentId = _currentFolderId.value
        if (parentId != null) {
            try {
                val parent = repository.getFolderById(parentId)
                if (parent != null) {
                    repository.updateFolder(parent.copy(updatedAt = System.currentTimeMillis()))
                }
            } catch(e: Exception) {}
        }
    }
}
