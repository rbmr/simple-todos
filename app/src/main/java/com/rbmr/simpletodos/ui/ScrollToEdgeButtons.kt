package com.rbmr.simpletodos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Small up/down affordance pinned to the bottom-right of a scrollable list. Each
 * arrow disappears once you're already at that edge, which is what signals there's
 * more content to scroll to in the first place.
 */
@Composable
fun BoxScope.ScrollToEdgeButtons(listState: LazyListState, scope: CoroutineScope) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (listState.canScrollBackward) {
            SmallFloatingActionButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        }
        if (listState.canScrollForward) {
            SmallFloatingActionButton(onClick = {
                scope.launch {
                    val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                    listState.animateScrollToItem(lastIndex)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Scroll to bottom")
            }
        }
    }
}
