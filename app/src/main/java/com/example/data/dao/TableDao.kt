package com.example.data.dao

import androidx.room.*
import com.example.data.entities.TableEntity
import com.example.data.entities.ColumnEntity
import com.example.data.entities.RowEntity
import com.example.data.entities.CellEntity
import kotlinx.coroutines.flow.Flow

data class FullTable(
    @Embedded val table: TableEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "tableId"
    )
    val columns: List<ColumnEntity>,
    @Relation(
        entity = RowEntity::class,
        parentColumn = "id",
        entityColumn = "tableId"
    )
    val rows: List<RowWithCells>
)

data class RowWithCells(
    @Embedded val row: RowEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "rowId"
    )
    val cells: List<CellEntity>
)

@Dao
interface TableDao {
    @Transaction
    @Query("SELECT * FROM tables WHERE id = :tableId")
    fun getFullTableFlow(tableId: Long): Flow<FullTable?>

    @Query("SELECT * FROM tables WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun getTablesInFolder(folderId: Long): Flow<List<TableEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTable(table: TableEntity): Long

    @Update
    suspend fun updateTable(table: TableEntity)

    @Delete
    suspend fun deleteTable(table: TableEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColumn(column: ColumnEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRow(row: RowEntity): Long

    @Delete
    suspend fun deleteRow(row: RowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: CellEntity): Long

    @Update
    suspend fun updateCell(cell: CellEntity)
    
    @Query("SELECT * FROM table_cells WHERE rowId = :rowId AND columnId = :columnId LIMIT 1")
    suspend fun getCell(rowId: Long, columnId: Long): CellEntity?
}
