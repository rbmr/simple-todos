package com.rbmr.simpletodos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoApp(viewModel: TodoViewModel) {
    val listsWithItems by viewModel.listsWithItems.collectAsStateWithLifecycle()
    val listCount = listsWithItems.size
    val pageCount = listCount + 1
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    LaunchedEffect(pageCount) {
        if (pagerState.currentPage >= pageCount) {
            pagerState.scrollToPage((pageCount - 1).coerceAtLeast(0))
        }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentPage = pagerState.currentPage,
                pageCount = pageCount,
                listCount = listCount,
                onPrevious = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                onNext = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
            )
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            beyondViewportPageCount = 1,
        ) { page ->
            if (page < listCount) {
                val listWithItems = listsWithItems[page]
                TodoListScreen(
                    listWithItems = listWithItems,
                    onToggleItem = viewModel::toggleItem,
                    onLiveLabelChange = viewModel::updateItemLabelLive,
                    onCommitLabel = viewModel::commitItemLabel,
                    onDeleteItem = viewModel::deleteItem,
                    onReorderItems = viewModel::reorderItems,
                    onAddItem = { onCreated -> viewModel.addItem(listWithItems.list.id, onCreated) },
                    onRenameList = { newName -> viewModel.renameList(listWithItems.list, newName) },
                    onMarkAllFinished = { finished -> viewModel.setAllFinished(listWithItems.list.id, finished) },
                    onTransferItem = { item, direction ->
                        val targetPage = page + direction
                        if (targetPage in 0 until listCount) {
                            val targetList = listsWithItems[targetPage].list
                            val insertIndex = if (direction < 0) 0 else Int.MAX_VALUE
                            viewModel.moveItem(item, targetList.id, insertIndex)
                            scope.launch { pagerState.animateScrollToPage(targetPage) }
                        }
                    },
                    hasPreviousList = page > 0,
                    hasNextList = page < listCount - 1,
                )
            } else {
                ManageListsScreen(
                    lists = listsWithItems.map { it.list },
                    viewModel = viewModel,
                    onOpenList = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(
    currentPage: Int,
    pageCount: Int,
    listCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (currentPage > 0) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous list")
            }
        } else {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(48.dp))
        }

        val label = if (currentPage < listCount) "${currentPage + 1} / $listCount" else "Manage Lists"
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (currentPage < pageCount - 1) {
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next list")
            }
        } else {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(48.dp))
        }
    }
}
