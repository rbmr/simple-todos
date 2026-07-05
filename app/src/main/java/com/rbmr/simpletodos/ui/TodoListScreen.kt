package com.rbmr.simpletodos.ui

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.rbmr.simpletodos.data.TODO_LIST_NAME_MAX_LENGTH
import com.rbmr.simpletodos.data.TodoItem
import com.rbmr.simpletodos.data.TodoListWithItems
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    listWithItems: TodoListWithItems,
    onToggleItem: (TodoItem) -> Unit,
    onLiveLabelChange: (TodoItem, String) -> Unit,
    onCommitLabel: (TodoItem, String) -> Unit,
    onDeleteItem: (TodoItem) -> Unit,
    onReorderItems: (List<TodoItem>) -> Unit,
    onAddItem: ((TodoItem) -> Unit) -> Unit,
    onRenameList: (String) -> Unit,
    onMarkAllFinished: (Boolean) -> Unit,
    onTransferItem: (TodoItem, Int) -> Unit,
    hasPreviousList: Boolean,
    hasNextList: Boolean,
    modifier: Modifier = Modifier,
) {
    val list = listWithItems.list
    val items = listWithItems.items

    val localItems = remember { mutableStateOf(items) }
    LaunchedEffect(items) { localItems.value = items }

    var editingItemId by remember(list.id) { mutableStateOf<java.util.UUID?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    var containerWidthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val edgeZonePx = with(density) { 56.dp.toPx() }

    val dragDropState = rememberDragDropState(
        lazyListState = listState,
        onMove = { from, to ->
            val current = localItems.value
            if (from < current.size && to < current.size) {
                localItems.value = current.toMutableList().apply { moveItem(from, to) }
            }
        },
    )

    // Dragging an item to within `edgeZonePx` of the left/right edge for a sustained moment
    // transfers it into the adjacent list (see TodoApp's onTransferItem for the page switch).
    val edgeDirection by remember(hasPreviousList, hasNextList) {
        derivedStateOf {
            when {
                !dragDropState.isDragging -> 0
                dragDropState.lastPointerX < edgeZonePx && hasPreviousList -> -1
                dragDropState.lastPointerX > containerWidthPx - edgeZonePx && hasNextList -> 1
                else -> 0
            }
        }
    }
    LaunchedEffect(edgeDirection) {
        if (edgeDirection != 0) {
            delay(450)
            val idx = dragDropState.currentIndexOfDraggedItem
            val current = localItems.value
            if (idx != null && idx < current.size) {
                val item = current[idx]
                localItems.value = current.toMutableList().apply { removeAt(idx) }
                dragDropState.onDragInterrupted()
                onTransferItem(item, edgeDirection)
            }
        }
    }

    // Keep the item being edited visible above the keyboard: when the IME opens (or the
    // edited item changes, e.g. chaining new items) scroll it into view.
    val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0
    val editingIndex by remember {
        derivedStateOf { editingItemId?.let { id -> localItems.value.indexOfFirst { it.id == id } } ?: -1 }
    }
    LaunchedEffect(editingIndex, imeVisible) {
        if (editingIndex >= 0 && imeVisible) {
            delay(80)
            listState.animateScrollToItem(editingIndex)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ListTitle(
            name = list.name,
            onRename = onRenameList,
            onToggleMarkAll = { onMarkAllFinished(it) },
            allFinished = items.isNotEmpty() && items.all { it.finished },
        )

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .onSizeChanged { containerWidthPx = it.width.toFloat() }
                .pointerInputDragReorder(
                    dragDropState = dragDropState,
                    scope = scope,
                    overscrollJobHolder = { overscrollJob },
                    setOverscrollJob = { overscrollJob = it },
                    onDragEnd = { onReorderItems(localItems.value) },
                ),
        ) {
            itemsIndexed(localItems.value, key = { _, item -> item.id.toString() }) { index, item ->
                DraggableItem(dragDropState = dragDropState, index = index) { isDragging ->
                    TodoItemCard(
                        item = item,
                        isEditing = editingItemId == item.id,
                        isDragging = isDragging,
                        onStartEditing = { editingItemId = item.id },
                        onLiveLabelChange = { onLiveLabelChange(item, it) },
                        onCommitLabel = { newLabel ->
                            onCommitLabel(item, newLabel)
                            if (editingItemId == item.id) editingItemId = null
                        },
                        onToggle = { onToggleItem(item) },
                        onDelete = { onDeleteItem(item) },
                        onDoneEditing = { newLabel ->
                            onCommitLabel(item, newLabel)
                            if (newLabel.isNotBlank()) {
                                onAddItem { newItem -> editingItemId = newItem.id }
                            } else {
                                editingItemId = null
                            }
                        },
                    )
                }
            }
            item(key = "add-item-card") {
                AddItemCard(
                    text = "Add item",
                    onClick = { onAddItem { newItem -> editingItemId = newItem.id } },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.pointerInputDragReorder(
    dragDropState: DragDropState,
    scope: kotlinx.coroutines.CoroutineScope,
    overscrollJobHolder: () -> Job?,
    setOverscrollJob: (Job?) -> Unit,
    onDragEnd: () -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(dragDropState) {
        detectDragGesturesAfterLongPress(
            onDragStart = { offset -> dragDropState.onDragStart(offset) },
            onDrag = { change, dragAmount ->
                change.consume()
                dragDropState.onDrag(dragAmount, change.position.x)

                if (overscrollJobHolder()?.isActive == true) return@detectDragGesturesAfterLongPress

                dragDropState.checkForOverScroll()
                    .takeIf { it != 0f }
                    ?.let {
                        setOverscrollJob(
                            scope.launch {
                                dragDropState.state.animateScrollBy(it * 1.3f, tween(easing = FastOutLinearInEasing))
                            },
                        )
                    } ?: overscrollJobHolder()?.cancel()
            },
            onDragEnd = {
                dragDropState.onDragInterrupted()
                overscrollJobHolder()?.cancel()
                onDragEnd()
            },
            onDragCancel = {
                dragDropState.onDragInterrupted()
                overscrollJobHolder()?.cancel()
                onDragEnd()
            },
        )
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTitle(
    name: String,
    allFinished: Boolean,
    onRename: (String) -> Unit,
    onToggleMarkAll: (Boolean) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(name) }
    LaunchedEffect(name) { if (!isEditing) text = name }

    androidx.compose.material3.Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
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
                        onRename(latestText.value.ifBlank { name })
                    }
                }
                val imeVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(LocalDensity.current) > 0
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
                    textStyle = MaterialTheme.typography.titleLarge,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        commit()
                        keyboardController?.hide()
                    }),
                )
            } else {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .clickable { isEditing = true },
                )
            }
            IconButton(onClick = { onToggleMarkAll(!allFinished) }) {
                Icon(
                    imageVector = if (allFinished) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (allFinished) "Unmark all as finished" else "Mark all as finished",
                )
            }
        }
    }
}
