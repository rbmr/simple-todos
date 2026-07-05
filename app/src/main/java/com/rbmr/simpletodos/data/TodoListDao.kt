package com.rbmr.simpletodos.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface TodoListDao {
    @Query("SELECT * FROM todo_lists ORDER BY `order` ASC")
    fun observeAll(): Flow<List<TodoList>>

    @Query("SELECT * FROM todo_lists ORDER BY `order` ASC")
    suspend fun getAllOnce(): List<TodoList>

    @Query("SELECT COALESCE(MAX(`order`), -1) + 1 FROM todo_lists")
    suspend fun nextOrder(): Int

    @Insert
    suspend fun insert(list: TodoList)

    @Insert
    suspend fun insertAll(lists: List<TodoList>)

    @Update
    suspend fun update(list: TodoList)

    @Update
    suspend fun updateAll(lists: List<TodoList>)

    @Delete
    suspend fun delete(list: TodoList)

    @Query("DELETE FROM todo_lists")
    suspend fun deleteAll()

    @Query("SELECT * FROM todo_lists WHERE id = :id")
    suspend fun getById(id: UUID): TodoList?
}
