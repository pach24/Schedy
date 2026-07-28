package com.schednd.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState

/**
 * @param glass si se pasa un estado con soporte, las tres piezas usan el cristal propio
 *   (warp + rim + haze del shader) en vez del blur de Haze. Requiere que el contenido de
 *   detrás lleve `liquidGlassBackdrop` con ese mismo estado.
 */
@Composable
fun AppleTopBar(
    title: String,
    hazeState: HazeState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = Icons.Filled.MoreHoriz,
    trailingContentDescription: String? = "Más opciones",
    onTrailingClick: (() -> Unit)? = null,
    glass: LiquidGlassState? = null
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)
    // El cristal ya difumina y refracta por debajo: la pieza solo aporta un velo. Con el
    // tinte de Haze (0.75) taparía justo lo que tiene que dejar ver.
    val glassTint = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.45f)
    val btnBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    val iconTint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .frostedSurface(glass, hazeState, 22.dp, bgColor, tintColor, glassTint)
                .border(1.dp, btnBorder, CircleShape)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Volver",
                tint = iconTint,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .frostedSurface(glass, hazeState, 22.dp, bgColor, tintColor, glassTint)
                .border(1.dp, btnBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = iconTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        if (trailingIcon != null && onTrailingClick != null) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .frostedSurface(glass, hazeState, 22.dp, bgColor, tintColor, glassTint)
                    .border(1.dp, btnBorder, CircleShape)
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onTrailingClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = trailingContentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else {
            Box(modifier = Modifier.size(44.dp))
        }
    }
}
