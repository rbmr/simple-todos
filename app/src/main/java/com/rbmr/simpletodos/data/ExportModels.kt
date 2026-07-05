package com.rbmr.simpletodos.data

import kotlinx.serialization.Serializable

@Serializable
data class TodoListExport(
    val id: String,
    val name: String,
    val order: Int,
)

@Serializable
data class TodoItemExport(
    val id: String,
    val label: String,
    val finished: Boolean,
    val order: Int,
    val todoListId: String,
)

@Serializable
data class DatabaseExport(
    val lists: List<TodoListExport>,
    val items: List<TodoItemExport>,
)

fun TodoList.toExport() = TodoListExport(id = id.toString(), name = name, order = order)

fun TodoItem.toExport() = TodoItemExport(
    id = id.toString(),
    label = label,
    finished = finished,
    order = order,
    todoListId = todoListId.toString(),
)

fun TodoListExport.toEntity() = TodoList(id = java.util.UUID.fromString(id), name = name, order = order)

fun TodoItemExport.toEntity() = TodoItem(
    id = java.util.UUID.fromString(id),
    label = label,
    finished = finished,
    order = order,
    todoListId = java.util.UUID.fromString(todoListId),
)
