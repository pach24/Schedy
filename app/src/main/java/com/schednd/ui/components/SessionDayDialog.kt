package com.schednd.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.R
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Lo justo que el diálogo necesita saber de una sesión; no conoce las clases de Inicio. */
data class DayDialogSession(
    val code: String,
    val name: String,
    val startTime: LocalTime?
)

private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Detalle de un día con sesión confirmada en el calendario de Inicio.
 *
 * La etiqueta se calcula contra hoy: una sesión ya jugada se dice ya jugada y no se
 * disfraza de próxima, que es justo lo que hacía el aviso genérico de "Próximamente".
 */
@Composable
fun SessionDayDialog(
    date: LocalDate,
    sessions: List<DayDialogSession>,
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onOpenSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = Locale.getDefault()
    val dayName = date.dayOfWeek.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }
    val monthName = date.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase() }

    val today = LocalDate.now()
    val daysAway = ChronoUnit.DAYS.between(today, date).toInt()
    val isPast = daysAway < 0

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

        // Lo pasado va en gris: el azul de la app es el color de lo que aún queda por
        // jugar, y usarlo aquí volvería a leerse como "próxima".
        val badgeBackground = if (isPast) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.primary
        }
        val badgeContent = if (isPast) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimary
        }
        Box(
            modifier = Modifier
                .clip(FullRoundShape)
                .background(badgeBackground)
                .padding(horizontal = 16.dp, vertical = 7.dp)
        ) {
            // `days_ago` se reaprovecha del listado de sesiones, donde va a media frase y
            // por eso empieza en minúscula; aquí es una etiqueta suelta.
            val badgeText = when {
                isPast -> pluralStringResource(R.plurals.days_ago, -daysAway, -daysAway)
                daysAway == 0 -> stringResource(R.string.calendar_day_session_today)
                else -> pluralStringResource(R.plurals.days_until, daysAway, daysAway)
            }
            Text(
                text = badgeText.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                color = badgeContent
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SquircleShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            sessions.forEachIndexed { index, session ->
                SessionRow(
                    session = session,
                    isPast = isPast,
                    onClick = { onOpenSession(session.code) }
                )
                if (index < sessions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
            }
        }

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
                text = stringResource(R.string.calendar_day_close),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: DayDialogSession,
    isPast: Boolean,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val dotColor = if (isPast) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(FullRoundShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = session.name.ifBlank { stringResource(R.string.session_fallback_name) },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = session.startTime?.format(TimeFormat)
                ?: stringResource(R.string.calendar_day_session_no_time),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
