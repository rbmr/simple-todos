package com.rbmr.simpletodos.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drag-and-drop reordering for a [LazyListState]-backed list, driven by
 * `detectDragGesturesAfterLongPress`. Adapted from the well-known Compose
 * manual-reorder pattern (see e.g. github.com/parniyan7/DraggableLazyColumnCompose).
 */
@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    onMove: (from: Int, to: Int) -> Unit,
): DragDropState {
    val scope = rememberCoroutineScope()
    return remember(lazyListState) {
        DragDropState(state = lazyListState, scope = scope, onMove = onMove)
    }
}

private fun LazyListState.getVisibleItemInfoFor(absoluteIndex: Int): LazyListItemInfo? =
    layoutInfo.visibleItemsInfo.getOrNull(absoluteIndex - layoutInfo.visibleItemsInfo.first().index)

private val LazyListItemInfo.offsetEnd: Int
    get() = offset + size

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LazyItemScope.DraggableItem(
    dragDropState: DragDropState,
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(isDragging: Boolean) -> Unit,
) {
    val current: Float by animateFloatAsState(dragDropState.draggingItemOffset, label = "dragOffset")
    val previous: Float by animateFloatAsState(dragDropState.previousItemOffset.value, label = "prevDragOffset")
    val dragging = index == dragDropState.currentIndexOfDraggedItem
    val draggingModifier = when {
        dragging -> Modifier.zIndex(1f).graphicsLayer { translationY = current }
        index == dragDropState.previousIndexOfDraggedItem -> Modifier.zIndex(1f).graphicsLayer { translationY = previous }
        else -> Modifier
    }
    Column(modifier = modifier.then(draggingModifier)) {
        content(dragging)
    }
}

class DragDropState internal constructor(
    val state: LazyListState,
    private val scope: CoroutineScope,
    private val onMove: (Int, Int) -> Unit,
) {
    private var draggedDistance by mutableStateOf(0f)
    private var draggingItemInitialOffset by mutableStateOf(0)

    internal val draggingItemOffset: Float
        get() = draggingItemLayoutInfo?.let { item -> draggingItemInitialOffset + draggedDistance - item.offset } ?: 0f

    private val draggingItemLayoutInfo: LazyListItemInfo?
        get() = state.layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentIndexOfDraggedItem }

    internal var previousIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set
    internal var previousItemOffset = Animatable(0f)
        private set

    private var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)

    var currentIndexOfDraggedItem by mutableStateOf<Int?>(null)
        private set

    val isDragging: Boolean get() = currentIndexOfDraggedItem != null

    private val initialOffsets: Pair<Int, Int>?
        get() = initiallyDraggedElement?.let { it.offset to it.offsetEnd }

    private val currentElement: LazyListItemInfo?
        get() = currentIndexOfDraggedItem?.let { state.getVisibleItemInfoFor(it) }

    fun onDragStart(offset: Offset) {
        state.layoutInfo.visibleItemsInfo
            .firstOrNull { item -> offset.y.toInt() in item.offset..item.offsetEnd }
            ?.also {
                currentIndexOfDraggedItem = it.index
                initiallyDraggedElement = it
                draggingItemInitialOffset = it.offset
            }
    }

    fun onDragInterrupted() {
        if (currentIndexOfDraggedItem != null) {
            previousIndexOfDraggedItem = currentIndexOfDraggedItem
            val startOffset = draggingItemOffset
            scope.launch {
                previousItemOffset.snapTo(startOffset)
                previousItemOffset.animateTo(0f, tween(easing = FastOutLinearInEasing))
                previousIndexOfDraggedItem = null
            }
        }
        draggingItemInitialOffset = 0
        draggedDistance = 0f
        currentIndexOfDraggedItem = null
        initiallyDraggedElement = null
    }

    fun onDrag(offset: Offset) {
        draggedDistance += offset.y

        val (topOffset, bottomOffset) = initialOffsets ?: return
        val startOffset = topOffset + draggedDistance
        val endOffset = bottomOffset + draggedDistance

        val hovered = currentElement ?: return
        state.layoutInfo.visibleItemsInfo
            .filterNot { item -> item.offsetEnd < startOffset || item.offset > endOffset || hovered.index == item.index }
            .firstOrNull { item ->
                val delta = startOffset - hovered.offset
                if (delta > 0) endOffset > item.offsetEnd else startOffset < item.offset
            }
            ?.also { item ->
                currentIndexOfDraggedItem?.let { current -> onMove(current, item.index) }
                currentIndexOfDraggedItem = item.index
            }
    }

    fun checkForOverScroll(): Float = initiallyDraggedElement?.let {
        val startOffset = it.offset + draggedDistance
        val endOffset = it.offsetEnd + draggedDistance
        when {
            draggedDistance > 0 -> (endOffset - state.layoutInfo.viewportEndOffset + 50f).takeIf { diff -> diff > 0 }
            draggedDistance < 0 -> (startOffset - state.layoutInfo.viewportStartOffset - 50f).takeIf { diff -> diff < 0 }
            else -> null
        }
    } ?: 0f
}

fun <T> MutableList<T>.moveItem(from: Int, to: Int) {
    if (from == to) return
    add(to, removeAt(from))
}
