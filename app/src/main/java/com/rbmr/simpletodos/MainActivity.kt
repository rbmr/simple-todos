package com.rbmr.simpletodos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbmr.simpletodos.data.AppDatabase
import com.rbmr.simpletodos.data.TodoRepository
import com.rbmr.simpletodos.ui.TodoApp
import com.rbmr.simpletodos.ui.TodoViewModel
import com.rbmr.simpletodos.ui.TodoViewModelFactory
import com.rbmr.simpletodos.ui.theme.SimpleTodosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TodoRepository(AppDatabase.getInstance(applicationContext))
        val factory = TodoViewModelFactory(repository)

        setContent {
            SimpleTodosApp(factory)
        }
    }
}

@Composable
private fun SimpleTodosApp(factory: TodoViewModelFactory) {
    val viewModel: TodoViewModel = viewModel(factory = factory)
    SimpleTodosTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            TodoApp(viewModel = viewModel)
        }
    }
}
