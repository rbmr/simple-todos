package com.rbmr.simpletodos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rbmr.simpletodos.data.TodoRepository

class TodoViewModelFactory(private val repository: TodoRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TodoViewModel::class.java))
        return TodoViewModel(repository) as T
    }
}
