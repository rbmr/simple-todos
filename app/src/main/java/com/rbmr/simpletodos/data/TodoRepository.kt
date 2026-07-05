package com.rbmr.simpletodos.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import java.util.UUID

class TodoRepository(private val db: AppDatabase) {
    private val listDao = db.todoListDao()
    private val itemDao = db.todoItemDao()

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** All lists with their items, kept in sync with the database. */
    val listsWithItems: Flow<List<TodoListWithItems>> =
        combine(listDao.observeAll(), itemDao.observeAll()) { lists, items ->
            val itemsByList = items.groupBy { it.todoListId }
            lists.map { list -> TodoListWithItems(list, itemsByList[list.id].orEmpty()) }
        }

    suspend fun addList(name: String): TodoList {
        val list = TodoList(id = UUID.randomUUID(), name = name.take(TODO_LIST_NAME_MAX_LENGTH), order = listDao.nextOrder())
        listDao.insert(list)
        return list
    }

    suspend fun renameList(list: TodoList, newName: String) {
        listDao.update(list.copy(name = newName.take(TODO_LIST_NAME_MAX_LENGTH)))
    }

    suspend fun deleteList(list: TodoList) {
        listDao.delete(list)
    }

    suspend fun reorderLists(orderedLists: List<TodoList>) {
        listDao.updateAll(orderedLists.mapIndexed { index, list -> list.copy(order = index) })
    }

    suspend fun setAllFinished(listId: UUID, finished: Boolean) {
        val items = itemDao.getByListOnce(listId)
        itemDao.updateAll(items.map { it.copy(finished = finished) })
    }

    suspend fun addItem(listId: UUID): TodoItem {
        val item = TodoItem(
            id = UUID.randomUUID(),
            label = "",
            finished = false,
            order = itemDao.nextOrder(listId),
            todoListId = listId,
        )
        itemDao.insert(item)
        return item
    }

    suspend fun updateItem(item: TodoItem) {
        itemDao.update(item)
    }

    suspend fun deleteItem(item: TodoItem) {
        itemDao.deleteById(item.id)
    }

    /** Sets a new order for all items within a single list. */
    suspend fun reorderItems(orderedItems: List<TodoItem>) {
        itemDao.updateAll(orderedItems.mapIndexed { index, item -> item.copy(order = index) })
    }

    /**
     * Moves [item] into [targetListId] at [newIndex], re-numbering both the source and
     * destination list orders. If the move is within the same list this behaves like a reorder.
     */
    suspend fun moveItem(item: TodoItem, targetListId: UUID, newIndex: Int) {
        if (item.todoListId == targetListId) {
            val siblings = itemDao.getByListOnce(targetListId).filter { it.id != item.id }.toMutableList()
            val insertAt = newIndex.coerceIn(0, siblings.size)
            siblings.add(insertAt, item)
            reorderItems(siblings)
        } else {
            val destination = itemDao.getByListOnce(targetListId).toMutableList()
            val insertAt = newIndex.coerceIn(0, destination.size)
            val moved = item.copy(todoListId = targetListId)
            destination.add(insertAt, moved)
            itemDao.updateAll(destination.mapIndexed { index, it -> it.copy(order = index) })

            val source = itemDao.getByListOnce(item.todoListId).filter { it.id != item.id }
            itemDao.updateAll(source.mapIndexed { index, it -> it.copy(order = index) })
        }
    }

    suspend fun exportJson(): String {
        val lists = listDao.getAllOnce()
        val items = itemDao.getAllOnce()
        val export = DatabaseExport(lists.map { it.toExport() }, items.map { it.toExport() })
        return json.encodeToString(DatabaseExport.serializer(), export)
    }

    suspend fun importJson(content: String) {
        val export = json.decodeFromString(DatabaseExport.serializer(), content)
        listDao.deleteAll()
        itemDao.deleteAll()
        listDao.insertAll(export.lists.map { it.toEntity() })
        itemDao.insertAll(export.items.map { it.toEntity() })
    }
}
