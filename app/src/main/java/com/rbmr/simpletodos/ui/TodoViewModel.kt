package com.rbmr.simpletodos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rbmr.simpletodos.data.TodoItem
import com.rbmr.simpletodos.data.TodoList
import com.rbmr.simpletodos.data.TodoListWithItems
import com.rbmr.simpletodos.data.TodoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TodoViewModel(private val repository: TodoRepository) : ViewModel() {

    val listsWithItems: StateFlow<List<TodoListWithItems>> = repository.listsWithItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addList(name: String, onCreated: (TodoList) -> Unit = {}) {
        viewModelScope.launch {
            val list = repository.addList(name)
            onCreated(list)
        }
    }

    fun renameList(list: TodoList, newName: String) {
        viewModelScope.launch { repository.renameList(list, newName) }
    }

    fun deleteList(list: TodoList) {
        viewModelScope.launch { repository.deleteList(list) }
    }

    fun reorderLists(orderedLists: List<TodoList>) {
        viewModelScope.launch { repository.reorderLists(orderedLists) }
    }

    fun setAllFinished(listId: UUID, finished: Boolean) {
        viewModelScope.launch { repository.setAllFinished(listId, finished) }
    }

    fun addItem(listId: UUID, onCreated: (TodoItem) -> Unit) {
        viewModelScope.launch {
            val item = repository.addItem(listId)
            onCreated(item)
        }
    }

    fun toggleItem(item: TodoItem) {
        viewModelScope.launch { repository.updateItem(item.copy(finished = !item.finished)) }
    }

    /** Persists in-progress typing so nothing is lost on a crash, without deleting on a transient empty value. */
    fun updateItemLabelLive(item: TodoItem, newLabel: String) {
        viewModelScope.launch { repository.updateItem(item.copy(label = newLabel)) }
    }

    /** Commits a label edit. An empty label deletes the item instead. */
    fun commitItemLabel(item: TodoItem, newLabel: String) {
        viewModelScope.launch {
            if (newLabel.isBlank()) {
                repository.deleteItem(item)
            } else {
                repository.updateItem(item.copy(label = newLabel))
            }
        }
    }

    fun deleteItem(item: TodoItem) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun reorderItems(orderedItems: List<TodoItem>) {
        viewModelScope.launch { repository.reorderItems(orderedItems) }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) { repository.exportJson() }

    suspend fun importJson(content: String) = withContext(Dispatchers.IO) { repository.importJson(content) }
}
