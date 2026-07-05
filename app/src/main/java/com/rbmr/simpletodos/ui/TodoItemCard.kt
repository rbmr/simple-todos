package com.rbmr.simpletodos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import com.rbmr.simpletodos.data.TodoItem
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoItemCard(
    item: TodoItem,
    isEditing: Boolean,
    isDragging: Boolean,
    onStartEditing: () -> Unit,
    onLiveLabelChange: (String) -> Unit,
    onCommitLabel: (String) -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onDoneEditing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color.White)
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDragging) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    CardDefaults.cardColors().containerColor
                },
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (item.finished && !isEditing) 0.5f else 1f)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isEditing) {
                    EditableLabelField(
                        item = item,
                        onLiveLabelChange = onLiveLabelChange,
                        onCommitLabel = onCommitLabel,
                        onDoneEditing = onDoneEditing,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Text(
                        text = item.label,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onStartEditing,
                            ),
                    )
                }
                Checkbox(
                    checked = item.finished,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun EditableLabelField(
    item: TodoItem,
    onLiveLabelChange: (String) -> Unit,
    onCommitLabel: (String) -> Unit,
    onDoneEditing: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember(item.id) {
        mutableStateOf(TextFieldValue(item.label, selection = TextRange(0, item.label.length)))
    }
    var committed by remember(item.id) { mutableStateOf(false) }
    var hasBeenFocused by remember(item.id) { mutableStateOf(false) }
    var isFocusedNow by remember(item.id) { mutableStateOf(false) }
    var imeWasVisible by remember(item.id) { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val latestText = rememberUpdatedState(fieldValue.text)
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0

    LaunchedEffect(item.id) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(fieldValue.text) {
        delay(300)
        onLiveLabelChange(fieldValue.text)
    }

    // If the keyboard gets dismissed (back gesture, swipe-down, tapping outside) without
    // pressing Done, Compose doesn't clear focus on its own — do it explicitly so the field
    // commits and stops showing a cursor once the keyboard is actually gone. Debounced because
    // the ime inset briefly flickers visible->invisible->visible when chaining focus straight
    // to a newly created item, which would otherwise be mistaken for a real dismissal.
    LaunchedEffect(imeVisible) {
        if (imeVisible) {
            imeWasVisible = true
        } else if (imeWasVisible && isFocusedNow) {
            delay(200)
            focusManager.clearFocus()
        }
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = { fieldValue = it },
        modifier = modifier
            .padding(vertical = 16.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { state ->
                isFocusedNow = state.isFocused
                if (state.isFocused) {
                    hasBeenFocused = true
                } else if (hasBeenFocused && !committed) {
                    committed = true
                    onCommitLabel(latestText.value)
                }
            },
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(TextStyle(color = LocalContentColor.current)),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                committed = true
                onDoneEditing(latestText.value)
                keyboardController?.hide()
            },
        ),
    )
}
