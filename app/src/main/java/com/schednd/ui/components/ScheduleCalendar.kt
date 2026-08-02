package com.schednd.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.DateSummary
import com.schednd.ui.theme.CalendarCellShape
import java.time.LocalDate
import java.time.YearMonth

/**
 * Calendario de Inicio: en qué días hay mesa, cuál es la próxima y cuáles quedaron atrás.
 *
 * El armazón —el carril de meses, la cabecera, el estirado— es [MonthCalendar]; aquí solo
 * se decide qué se pinta en un día. Ver allí qué hacen [expansion], [expandedSpace] y
 * [swipeMonths].
 */
@Composable
fun HomeScheduleCalendar(
    sessionDates: Map<LocalDate, String>,
    nextSessionDate: LocalDate?,
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    expansion: () -> Float = { 0f },
    expandedSpace: Dp = Dp.Unspecified,
    swipeMonths: Boolean = false
) {
    val today = LocalDate.now()

    val initialMonth = remember(nextSessionDate, sessionDates) {
        nextSessionDate?.let { YearMonth.from(it) }
            ?: sessionDates.keys.filter { !it.isBefore(today) }.minOrNull()?.let { YearMonth.from(it) }
            ?: YearMonth.now()
    }

    // Único movimiento del calendario: el punto de la próxima sesión respira despacio.
    // Solo ese, y solo en opacidad: en una rejilla densa cualquier cosa que crezca o se
    // repita en varias celdas se lee como un fallo de carga, no como un acento.
    val pulse = rememberInfiniteTransition(label = "calendarPulse")
    val nextDotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PulseMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nextSessionDot"
    )

    MonthCalendar(
        initialMonth = initialMonth,
        modifier = modifier,
        expansion = expansion,
        expandedSpace = expandedSpace,
        swipeMonths = swipeMonths
    ) { date, cell ->
        SessionDay(
            date = date,
            hasSession = sessionDates.containsKey(date),
            isNext = date == nextSessionDate,
            today = today,
            // Envuelto en una lambda a propósito: el respiro se lee al dibujar, dentro del
            // `graphicsLayer`. Leído aquí recompondría las cuarenta y dos celdas por frame.
            nextDotAlpha = { nextDotAlpha },
            onTap = { onDayTap(date) },
            modifier = cell
        )
    }
}

/**
 * Calendario de dentro de una sesión: el mapa de calor de quién puede cada día y la fecha
 * que se acabó confirmando.
 *
 * Mismo armazón que el de Inicio —se tira de él para abrirlo, se desliza de lado para
 * cambiar de mes— y lo único suyo es la celda, que aquí lleva color de fondo y recuento.
 */
@Composable
fun ScheduleCalendar(
    dateSummaries: List<DateSummary>,
    totalParticipants: Int,
    confirmedDate: LocalDate?,
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    expansion: () -> Float = { 0f },
    expandedSpace: Dp = Dp.Unspecified,
    swipeMonths: Boolean = false
) {
    val isDark = isSystemInDarkTheme()
    val summaryMap = remember(dateSummaries) { dateSummaries.associateBy { it.date } }

    val initialMonth = remember(confirmedDate, dateSummaries) {
        confirmedDate?.let { YearMonth.from(it) }
            ?: dateSummaries.minByOrNull { it.date }?.let { YearMonth.from(it.date) }
            ?: YearMonth.now()
    }

    MonthCalendar(
        initialMonth = initialMonth,
        modifier = modifier,
        expansion = expansion,
        expandedSpace = expandedSpace,
        swipeMonths = swipeMonths
    ) { date, cell ->
        ScheduleDay(
            date = date,
            summary = summaryMap[date],
            totalParticipants = totalParticipants,
            isConfirmed = date == confirmedDate,
            isDark = isDark,
            onTap = { onDayTap(date) },
            modifier = cell
        )
    }
}

/**
 * Un día del calendario de sesión: el fondo dice cuánta gente puede y el número de debajo
 * lo cuenta. Solo se puede tocar si hay algo que enseñar.
 */
@Composable
private fun ScheduleDay(
    date: LocalDate,
    summary: DateSummary?,
    totalParticipants: Int,
    isConfirmed: Boolean,
    isDark: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == LocalDate.now()
    val hasData = summary != null && summary.count > 0 && totalParticipants > 0
    val isTappable = hasData || isConfirmed
    val heatColor = if (hasData) getHeatmapColor(summary!!.count, totalParticipants) else null

    val bgColor = when {
        isConfirmed -> MaterialTheme.colorScheme.primary
        heatColor != null -> heatColor
        else -> Color.Transparent
    }

    val textColor = when {
        isConfirmed -> MaterialTheme.colorScheme.onPrimary
        hasData && summary != null -> {
            val ratio = summary.count.toFloat() / totalParticipants.coerceAtLeast(1)
            if (isDark) {
                if (ratio < 0.57f) Color.White else Color(0xFF111111)
            } else {
                if (ratio < 0.43f) Color(0xFF111111) else Color.White
            }
        }
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(CalendarCellShape)
            .background(bgColor, CalendarCellShape)
            .then(
                if (isToday && !isConfirmed && !hasData) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), CalendarCellShape)
                } else Modifier
            )
            .then(
                if (isTappable) Modifier.clickable(
                    indication = LocalIndication.current,
                    interactionSource = interaction,
                    onClick = onTap
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isConfirmed || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor
            )
            if (hasData && summary != null) {
                Text(
                    text = "${summary.count}/${totalParticipants}",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                    color = textColor.copy(alpha = 0.75f)
                )
            }
        }
    }
}

/**
 * Un día del calendario de Inicio: hoy es la celda maciza y la sesión es contorno más
 * punto. El verde del mapa de calor se queda fuera a propósito: en esta app significa
 * disponibilidad, y usarlo también para "confirmada" mezcla dos cosas distintas.
 */
@Composable
private fun SessionDay(
    date: LocalDate,
    hasSession: Boolean,
    isNext: Boolean,
    today: LocalDate,
    nextDotAlpha: () -> Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = date == today
    // Una sesión ya jugada sigue marcada, como registro, pero apagada: con el mismo
    // contorno y el mismo punto que una futura se leía como si quedara por jugar.
    val isPlayed = hasSession && date.isBefore(today)

    val onSurface = MaterialTheme.colorScheme.onSurface
    val contentColor = if (isToday) MaterialTheme.colorScheme.surface else onSurface

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .padding(3.dp)
            .clip(CalendarCellShape)
            .background(
                if (isToday) onSurface else Color.Transparent,
                CalendarCellShape
            )
            .then(
                if (hasSession && !isToday) Modifier.border(
                    width = if (isNext) 1.5.dp else 1.dp,
                    color = onSurface.copy(
                        alpha = when {
                            isNext -> 0.45f
                            isPlayed -> 0.10f
                            else -> 0.22f
                        }
                    ),
                    shape = CalendarCellShape
                ) else Modifier
            )
            .then(
                if (hasSession) Modifier.clickable(
                    indication = LocalIndication.current,
                    interactionSource = interaction,
                    onClick = onTap
                ) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if ((hasSession && !isPlayed) || isToday) {
                    FontWeight.SemiBold
                } else {
                    FontWeight.Normal
                },
                color = contentColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            // El hueco del punto se reserva siempre, tenga sesión o no: así todos los
            // números del mes caen sobre la misma línea.
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .graphicsLayer { alpha = if (isNext) nextDotAlpha() else 1f }
                    .background(
                        when {
                            !hasSession -> Color.Transparent
                            isPlayed -> contentColor.copy(alpha = 0.3f)
                            else -> contentColor
                        },
                        CircleShape
                    )
            )
        }
    }
}

/** Medio ciclo del respiro del punto de la próxima sesión. */
private const val PulseMillis = 1400
