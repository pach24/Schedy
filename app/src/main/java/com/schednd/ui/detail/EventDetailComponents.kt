package com.schednd.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.pressScale
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.schednd.R

/** Duración por defecto del evento de calendario cuando la sesión tiene hora. */
internal const val SESSION_DEFAULT_DURATION_MS = 3L * 60 * 60 * 1000

@Composable
internal fun GenActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.72f) else Color(0xFFFFFFFF)
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(FullRoundShape)
            .background(fillColor)
            .border(1.dp, borderBrush, FullRoundShape)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = { content() }
        )
    }
}

@Composable
internal fun SessionCountdown(
    confirmedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, confirmedDate)
    val isPast = daysLeft < 0
    val daysAgo = -daysLeft

    Column(modifier = modifier) {
        Text(
            text = stringResource(if (isPast) R.string.detail_past_session else R.string.detail_next_session),
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (isPast) {
            val pastLabel = confirmedDate.format(
                DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_weekday_day_month), Locale.getDefault())
            ).replaceFirstChar { it.uppercaseChar() }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pastLabel,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = pluralStringResource(R.plurals.days_ago, daysAgo.toInt(), daysAgo.toInt()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        } else {
            val dateLabel = confirmedDate.format(
                DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_weekday_day), Locale.getDefault())
            ).replaceFirstChar { it.uppercaseChar() }
            Row(verticalAlignment = Alignment.Bottom) {
                if (daysLeft == 0L) {
                    Text(
                        text = stringResource(R.string.detail_countdown_today),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 72.sp,
                            letterSpacing = (-2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", daysLeft),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 88.sp,
                            letterSpacing = (-4).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(
                        text = if (daysLeft == 0L) "" else stringResource(R.string.detail_countdown_days),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Envoltorio común de los diálogos de la pantalla: scrim que cierra al tocar fuera y
 * entrada/salida con rebote. Los tres diálogos del detalle compartían este mismo bloque.
 */
@Composable
internal fun ScrimDialogOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(200))
    ) {
        val animScope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            with(animScope) {
                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.86f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(180)),
                        exit = scaleOut(targetScale = 0.92f, animationSpec = tween(160)) +
                            fadeOut(tween(160))
                    )
                ) {
                    content()
                }
            }
        }
    }
}
