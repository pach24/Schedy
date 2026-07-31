package com.schednd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.schednd.ui.theme.CardShape
import com.schednd.ui.theme.LightHeroSurface

/**
 * Brillo del filo: una luz que entra por arriba y se apaga hacia abajo.
 *
 * Hace de rim light donde no puede haber cristal. Las piezas del shader tienen que
 * dibujarse fuera del contenido que refractan, así que todo lo que vive dentro de la
 * pantalla —tarjetas, listados— se queda sin él y lo suple con este filo pintado.
 */
@Composable
fun rimHighlightBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    return Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
}

/**
 * Relleno del widget de próxima sesión. Vive aquí, junto al filo, porque lo comparte todo
 * lo que tenga que parecerse a él: si se copiara a mano acabarían separándose.
 */
@Composable
fun heroSurfaceColor(): Color =
    if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        LightHeroSurface
    }

@Composable
fun GenCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = if (isDark) Color(0xFF27272A).copy(alpha = 0.72f) else Color(0xFFFFFFFF)
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(fillColor)
            .border(1.dp, rimHighlightBrush(), CardShape)
    ) {
        content()
    }
}
