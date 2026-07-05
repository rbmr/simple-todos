package com.rbmr.simpletodos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TodoItemDao {
    @Query("SELECT * FROM todo_items ORDER BY `order` ASC")
    fun observeAll(): Flow<List<TodoItem>>

    @Query("SELECT * FROM todo_items ORDER BY `order` ASC")
    suspend fun getAllOnce(): List<TodoItem>

    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM todo_items WHERE todoListId = :listId")
    suspend fun nextOrder(listId: UUID): Int

    @Insert
    suspend fun insert(item: TodoItem)

    @Insert
    suspend fun insertAll(items: List<TodoItem>)

    @Update
    suspend fun update(item: TodoItem)

    @Update
    suspend fun updateAll(items: List<TodoItem>)

    @Delete
    suspend fun delete(item: TodoItem)

    @Query("DELETE FROM todo_items WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM todo_items")
    suspend fun deleteAll()

    @Query("SELECT * FROM todo_items WHERE todoListId = :listId ORDER BY `order` ASC")
    suspend fun getByListOnce(listId: UUID): List<TodoItem>
}
