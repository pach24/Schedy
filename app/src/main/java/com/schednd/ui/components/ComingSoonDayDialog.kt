package com.schednd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.R
import com.schednd.ui.theme.FullRoundShape
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * Detalle de un día del calendario de la sesión: todavía no muestra quién puede y quién
 * no, así que avisa de que está por llegar. El calendario de Inicio no lo usa —ahí un día
 * con sesión sí tiene algo que contar, y lo cuenta [SessionDayDialog].
 */
@Composable
fun ComingSoonDayDialog(
    date: LocalDate,
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onDismiss: () -> Unit
) {
    val locale = Locale.getDefault()
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }

    DayDialogShell(hazeState = hazeState, glass = glass, onDismiss = onDismiss) { dismiss ->
        DayDialogIcon()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "$dayName ${date.dayOfMonth}",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$monthName ${date.year}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(FullRoundShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            Text(
                text = stringResource(R.string.coming_soon),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.calendar_day_soon_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(FullRoundShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { dismiss() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.calendar_day_soon_dismiss),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
