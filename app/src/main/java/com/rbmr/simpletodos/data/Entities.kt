package com.rbmr.simpletodos.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

const val TODO_LIST_NAME_MAX_LENGTH = 128

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey val id: UUID,
    val name: String,
    val order: Int,
)

@Entity(
    tableName = "todo_items",
    foreignKeys = [
        ForeignKey(
            entity = TodoList::class,
            parentColumns = ["id"],
            childColumns = ["todoListId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("todoListId")],
)
data class TodoItem(
    @PrimaryKey val id: UUID,
    val label: String,
    val finished: Boolean,
    val order: Int,
    val todoListId: UUID,
)

/** A [TodoList] together with its (already order-sorted) [TodoItem]s. */
data class TodoListWithItems(
    val list: TodoList,
    val items: List<TodoItem>,
)
