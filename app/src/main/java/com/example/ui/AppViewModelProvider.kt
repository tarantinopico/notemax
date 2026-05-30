package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.NoteMaxRepository

class AppViewModelProvider {
    companion object {
        fun factory(repository: NoteMaxRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(DirectoryViewModel::class.java)) {
                    return DirectoryViewModel(repository) as T
                }
                if (modelClass.isAssignableFrom(NoteDetailViewModel::class.java)) {
                    return NoteDetailViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
