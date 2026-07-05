package com.rbmr.simpletodos.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rbmr.simpletodos.data.TODO_LIST_NAME_MAX_LENGTH
import com.rbmr.simpletodos.data.TodoList
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ManageListsScreen(
    lists: List<TodoList>,
    viewModel: TodoViewModel,
    onOpenList: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var listPendingDelete by remember { mutableStateOf<TodoList?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportJson()
                context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                val text = context.contentResolver.openInputStream(uri)?.let { stream ->
                    BufferedReader(InputStreamReader(stream)).use { it.readText() }
                }
                if (text != null) viewModel.importJson(text)
            }
        }
    }

    val localLists = remember { mutableStateOf(lists) }
    LaunchedEffect(lists) { localLists.value = lists }

    val listState = rememberLazyListState()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }

    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { from, to ->
            val current = localLists.value
            if (from < current.size && to < current.size) {
                localLists.value = current.toMutableList().apply { moveItem(from, to) }
            }
        },
    )

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.material3.Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    text = "Manage Lists",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = { exportLauncher.launch("simple-todos-export.json") }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text("Export", modifier = Modifier.padding(start = 6.dp))
                    }
                    TextButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Text("Import", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .pointerInput(dragDropState) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset -> dragDropState.onDragStart(offset) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragDropState.onDrag(dragAmount, change.position.x)
                            if (overscrollJob?.isActive == true) return@detectDragGesturesAfterLongPress
                            dragDropState.checkForOverScroll()
                                .takeIf { it != 0f }
                                ?.let {
                                    overscrollJob = scope.launch {
                                        dragDropState.state.animateScrollBy(it * 1.3f, tween(easing = FastOutLinearInEasing))
                                    }
                                } ?: overscrollJob?.cancel()
                        },
                        onDragEnd = {
                            dragDropState.onDragInterrupted()
                            overscrollJob?.cancel()
                            viewModel.reorderLists(localLists.value)
                        },
                        onDragCancel = {
                            dragDropState.onDragInterrupted()
                            overscrollJob?.cancel()
                        },
                    )
                },
        ) {
            itemsIndexed(localLists.value, key = { _, list -> list.id.toString() }) { index, list ->
                DraggableItem(dragDropState = dragDropState, index = index) {
                    ManageListRow(
                        list = list,
                        onOpen = { onOpenList(index) },
                        onRename = { newName -> viewModel.renameList(list, newName) },
                        onDeleteRequest = { listPendingDelete = list },
                    )
                }
            }
            item(key = "add-list-card") {
                AddItemCard(text = "Add list", onClick = { viewModel.addList("New list") })
            }
        }
    }

    listPendingDelete?.let { list ->
        AlertDialog(
            onDismissRequest = { listPendingDelete = null },
            title = { Text("Delete \"${list.name}\"?") },
            text = { Text("This permanently deletes the list and all of its items.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteList(list)
                    listPendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { listPendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageListRow(
    list: TodoList,
    onOpen: () -> Unit,
    onRename: (String) -> Unit,
    onDeleteRequest: () -> Unit,
) {
    var isEditing by remember(list.id) { mutableStateOf(false) }
    var text by remember(list.id) { mutableStateOf(list.name) }
    LaunchedEffect(list.name) { if (!isEditing) text = list.name }

    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Reorder, contentDescription = null, modifier = Modifier.padding(end = 12.dp))

            if (isEditing) {
                val focusRequester = remember { FocusRequester() }
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val keyboardController = LocalSoftwareKeyboardController.current
                var fieldValue by remember {
                    mutableStateOf(TextFieldValue(text, selection = TextRange(0, text.length)))
                }
                val latestText = rememberUpdatedState(fieldValue.text)
                var hasBeenFocused by remember { mutableStateOf(false) }
                var isFocusedNow by remember { mutableStateOf(false) }
                var imeWasVisible by remember { mutableStateOf(false) }
                var committed by remember { mutableStateOf(false) }
                val commit = {
                    if (!committed) {
                        committed = true
                        isEditing = false
                        onRename(latestText.value.ifBlank { list.name })
                    }
                }
                val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(
                    androidx.compose.ui.platform.LocalDensity.current,
                ) > 0
                LaunchedEffect(imeVisible) {
                    if (imeVisible) {
                        imeWasVisible = true
                    } else if (imeWasVisible && isFocusedNow) {
                        delay(200)
                        focusManager.clearFocus()
                    }
                }
                LaunchedEffect(Unit) { focusRequester.requestFocus() }
                TextField(
                    value = fieldValue,
                    onValueChange = { if (it.text.length <= TODO_LIST_NAME_MAX_LENGTH) fieldValue = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            isFocusedNow = state.isFocused
                            if (state.isFocused) {
                                hasBeenFocused = true
                            } else if (hasBeenFocused) {
                                commit()
                            }
                        },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        commit()
                        keyboardController?.hide()
                    }),
                )
            } else {
                Text(
                    text = list.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp)
                        .clickable { isEditing = true },
                )
            }

            IconButton(onClick = onOpen) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Open list")
            }
            IconButton(onClick = onDeleteRequest) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete list",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
