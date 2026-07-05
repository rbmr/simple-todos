package com.rbmr.simpletodos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rbmr.simpletodos.data.AppDatabase
import com.rbmr.simpletodos.data.TodoRepository
import com.rbmr.simpletodos.ui.TodoApp
import com.rbmr.simpletodos.ui.TodoViewModel
import com.rbmr.simpletodos.ui.TodoViewModelFactory
import com.rbmr.simpletodos.ui.theme.SimpleTodosTheme
import com.rbmr.simpletodos.ui.theme.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = TodoRepository(AppDatabase.getInstance(applicationContext))
        val factory = TodoViewModelFactory(repository)
        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            SimpleTodosApp(factory, themePreferences)
        }
    }
}

@Composable
private fun SimpleTodosApp(factory: TodoViewModelFactory, themePreferences: ThemePreferences) {
    val viewModel: TodoViewModel = viewModel(factory = factory)
    var themeMode by remember { mutableStateOf(themePreferences.load()) }
    SimpleTodosTheme(themeMode = themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            TodoApp(
                viewModel = viewModel,
                themeMode = themeMode,
                onThemeModeChange = {
                    themeMode = it
                    themePreferences.save(it)
                },
            )
        }
    }
}
