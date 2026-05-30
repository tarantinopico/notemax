package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NoteMaxRepository
import com.example.data.dao.FullTable
import com.example.data.entities.CellEntity
import com.example.data.entities.ColumnEntity
import com.example.data.entities.RowEntity
import com.example.data.entities.TableEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TableViewModel(private val repository: NoteMaxRepository) : ViewModel() {
    private val _currentTableId = MutableStateFlow<Long?>(null)
    val currentTableId: StateFlow<Long?> = _currentTableId

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun dismissError() {
        _error.value = null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val fullTable = _currentTableId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getFullTableFlow(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadTable(tableId: Long) {
        _currentTableId.value = tableId
    }

    fun updateTableTitle(title: String) {
        val table = fullTable.value?.table ?: return
        viewModelScope.launch {
            try {
                repository.updateTable(table.copy(title = title, updatedAt = System.currentTimeMillis()))
            } catch (e: Exception) { _error.value = "Failed to update title: ${e.localizedMessage}" }
        }
    }

    fun addRow() {
        val table = fullTable.value?.table ?: return
        val currentRows = fullTable.value?.rows ?: emptyList()
        viewModelScope.launch {
            try {
                val order = currentRows.size
                repository.insertRow(RowEntity(tableId = table.id, displayOrder = order))
                updateTableTimestamp(table)
            } catch (e: Exception) { _error.value = "Failed to add row: ${e.localizedMessage}" }
        }
    }

    fun deleteRow(row: RowEntity) {
        val table = fullTable.value?.table ?: return
        viewModelScope.launch {
            try {
                repository.deleteRow(row)
                updateTableTimestamp(table)
            } catch (e: Exception) { _error.value = "Failed to delete row: ${e.localizedMessage}" }
        }
    }

    fun addColumn(name: String, type: com.example.data.entities.ColumnType) {
        if (name.isBlank()) return
        val table = fullTable.value?.table ?: return
        val currentColumns = fullTable.value?.columns ?: emptyList()
        viewModelScope.launch {
            try {
                val order = currentColumns.size
                repository.insertColumn(ColumnEntity(tableId = table.id, name = name, type = type, displayOrder = order))
                updateTableTimestamp(table)
            } catch (e: Exception) { _error.value = "Failed to add column: ${e.localizedMessage}" }
        }
    }

    fun updateCell(rowId: Long, columnId: Long, value: String) {
        val table = fullTable.value?.table ?: return
        viewModelScope.launch {
            try {
                val cell = repository.getCell(rowId, columnId)
                if (cell != null) {
                    repository.updateCell(cell.copy(value = value))
                } else {
                    repository.insertCell(CellEntity(rowId = rowId, columnId = columnId, value = value))
                }
                updateTableTimestamp(table)
            } catch (e: Exception) { _error.value = "Failed to update cell: ${e.localizedMessage}" }
        }
    }
    
    private suspend fun updateTableTimestamp(table: TableEntity) {
        try {
            repository.updateTable(table.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) { _error.value = "Failed to update table timestamp: ${e.localizedMessage}" }
    }
}
