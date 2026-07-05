package com.rbmr.simpletodos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AddItemCard(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .dashedBorder(color = borderColor, cornerRadius = 16.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = contentColor)
        Text(text = text, color = contentColor, modifier = Modifier.padding(start = 12.dp))
    }
}

private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp): Modifier = drawWithCache {
    val stroke = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
    )
    val radiusPx = cornerRadius.toPx()
    onDrawBehind {
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
            style = stroke,
        )
    }
}
