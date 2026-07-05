package com.rbmr.simpletodos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Shared top bar for every screen in the pager: same padding and divider so the
 * title sits at an identical position whether you're on a list or the Manage page.
 */
@Composable
fun AppHeader(
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
    title: @Composable RowScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            title()
            Row(verticalAlignment = Alignment.CenterVertically, content = trailing)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
