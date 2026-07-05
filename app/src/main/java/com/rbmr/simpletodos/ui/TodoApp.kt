package com.rbmr.simpletodos.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rbmr.simpletodos.ui.theme.ThemeMode
import kotlin.math.abs
import kotlinx.coroutines.launch

/** How many times the real page sequence repeats across the pager's virtual page space. */
private const val LOOP_FACTOR = 5000

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TodoApp(
    viewModel: TodoViewModel,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    val listsWithItems by viewModel.listsWithItems.collectAsStateWithLifecycle()
    val listCount = listsWithItems.size
    // Lists 0..listCount-1, plus the Manage page as the final real page.
    val realPageCount = listCount + 1
    val virtualPageCount = realPageCount * LOOP_FACTOR

    val pagerState = rememberPagerState(
        initialPage = (LOOP_FACTOR / 2) * realPageCount,
        pageCount = { virtualPageCount },
    )
    val scope = rememberCoroutineScope()

    // Whenever the number of real pages changes (lists added/removed, or the initial
    // DB load landing), re-anchor the virtual page so we keep showing the same real
    // page instead of jumping somewhere arbitrary.
    var lastRealPageCount by remember { mutableIntStateOf(realPageCount) }
    LaunchedEffect(realPageCount) {
        val previousRealPageCount = lastRealPageCount
        if (realPageCount != previousRealPageCount) {
            val previousReal = pagerState.currentPage.mod(previousRealPageCount)
            val desiredReal = previousReal.coerceAtMost(realPageCount - 1)
            val target = nearestVirtualPage(desiredReal, pagerState.currentPage, realPageCount)
            pagerState.scrollToPage(target.coerceIn(0, virtualPageCount - 1))
            lastRealPageCount = realPageCount
        }
    }

    val currentRealPage = pagerState.currentPage.mod(realPageCount)

    fun jumpToRealPage(target: Int) {
        scope.launch { pagerState.scrollToPage(nearestVirtualPage(target, pagerState.currentPage, realPageCount)) }
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRealPage = currentRealPage,
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
            val realPage = page.mod(realPageCount)
            if (realPage < listCount) {
                val listWithItems = listsWithItems[realPage]
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
                    onJumpToManage = { jumpToRealPage(realPageCount - 1) },
                )
            } else {
                ManageScreen(
                    lists = listsWithItems,
                    viewModel = viewModel,
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    onOpenList = { index -> jumpToRealPage(index) },
                )
            }
        }
    }
}

/** Nearest virtual page (to [current]) whose real index is [desiredReal]. */
private fun nearestVirtualPage(desiredReal: Int, current: Int, realCount: Int): Int {
    val base = current - current.mod(realCount)
    return listOf(base - realCount, base, base + realCount)
        .map { it + desiredReal }
        .minByOrNull { abs(it - current) }!!
}

@Composable
private fun BottomNavBar(
    currentRealPage: Int,
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
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous")
        }

        val label = if (currentRealPage < listCount) "${currentRealPage + 1} / $listCount" else "Manage"
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
        }
    }
}
