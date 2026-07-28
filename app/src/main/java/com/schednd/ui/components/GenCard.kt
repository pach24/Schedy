package com.schednd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.schednd.ui.theme.CardShape

@Composable
fun GenCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = if (isDark) Color(0xFF27272A).copy(alpha = 0.72f) else Color(0xFFFFFFFF)
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(fillColor)
            .border(1.dp, borderBrush, CardShape)
    ) {
        content()
    }
}
