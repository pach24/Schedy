package com.schednd.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.DateSummary
import com.schednd.ui.theme.CalendarCellShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.schednd.R

@Composable
fun ScheduleCalendar(
    dateSummaries: List<DateSummary>,
    totalParticipants: Int,
    confirmedDate: LocalDate?,
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val summaryMap = remember(dateSummaries) { dateSummaries.associateBy { it.date } }

    val initialMonth = remember(confirmedDate, dateSummaries) {
        confirmedDate?.let { YearMonth.from(it) }
            ?: dateSummaries.minByOrNull { it.date }?.let { YearMonth.from(it.date) }
            ?: YearMonth.now()
    }

    var currentMonth by remember { mutableStateOf(initialMonth) }
    var slideDirection by remember { mutableIntStateOf(1) }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                slideDirection = -1
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                slideDirection = 1
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val dir = slideDirection
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                (slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) { dir * (it / 3) } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) { -dir * (it / 3) } + fadeOut(tween(250)))
            },
            label = "ScheduleMonthTransition"
        ) { month ->
            Column {
                val firstDay = month.atDay(1)
                val startOffset = firstDay.dayOfWeek.value - 1
                val cells = buildList {
                    repeat(startOffset) { add(null) }
                    for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
                }

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                ScheduleDay(
                                    date = date,
                                    summary = summaryMap[date],
                                    totalParticipants = totalParticipants,
                                    isConfirmed = date == confirmedDate,
                                    isDark = isDark,
                                    onTap = { onDayTap(date) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

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
            .aspectRatio(1f)
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

@Composable
fun HomeScheduleCalendar(
    sessionDates: Map<LocalDate, String>,
    nextSessionDate: LocalDate?,
    onDayTap: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()

    val initialMonth = remember(nextSessionDate, sessionDates) {
        nextSessionDate?.let { YearMonth.from(it) }
            ?: sessionDates.keys.filter { !it.isBefore(today) }.minOrNull()?.let { YearMonth.from(it) }
            ?: YearMonth.now()
    }

    var currentMonth by remember { mutableStateOf(initialMonth) }
    var slideDirection by remember { mutableIntStateOf(1) }

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

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                slideDirection = -1
                currentMonth = currentMonth.minusMonths(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month), tint = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = {
                slideDirection = 1
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            listOf("L", "M", "X", "J", "V", "S", "D").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        val dir = slideDirection
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                (slideInHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) { dir * (it / 3) } + fadeIn(tween(300))) togetherWith
                    (slideOutHorizontally(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)) { -dir * (it / 3) } + fadeOut(tween(250)))
            },
            label = "HomeScheduleMonthTransition"
        ) { month ->
            Column {
                val firstDay = month.atDay(1)
                val startOffset = firstDay.dayOfWeek.value - 1
                val cells = buildList {
                    repeat(startOffset) { add(null) }
                    for (day in 1..month.lengthOfMonth()) add(month.atDay(day))
                }

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val sessionName = sessionDates[date]
                                val isNext = date == nextSessionDate
                                val isToday = date == today
                                val hasSession = sessionName != null

                                // Mismo lenguaje que el mini calendario de Inicio: hoy es la
                                // celda maciza y la sesión es contorno más punto. El verde
                                // del mapa de calor se queda fuera a propósito: en esta app
                                // significa disponibilidad, y usarlo también para
                                // "confirmada" mezcla dos cosas distintas.
                                val onSurface = MaterialTheme.colorScheme.onSurface
                                val contentColor = if (isToday) MaterialTheme.colorScheme.surface else onSurface

                                val interaction = remember { MutableInteractionSource() }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp)
                                        .clip(CalendarCellShape)
                                        .background(
                                            if (isToday) onSurface else Color.Transparent,
                                            CalendarCellShape
                                        )
                                        .then(
                                            if (hasSession && !isToday) Modifier.border(
                                                width = if (isNext) 1.5.dp else 1.dp,
                                                color = onSurface.copy(alpha = if (isNext) 0.45f else 0.22f),
                                                shape = CalendarCellShape
                                            ) else Modifier
                                        )
                                        .then(
                                            if (hasSession) Modifier.clickable(
                                                indication = LocalIndication.current,
                                                interactionSource = interaction,
                                                onClick = { onDayTap(date) }
                                            ) else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (hasSession || isToday) FontWeight.SemiBold else FontWeight.Normal,
                                            color = contentColor
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        // El hueco del punto se reserva siempre, tenga
                                        // sesión o no: así todos los números del mes caen
                                        // sobre la misma línea.
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .graphicsLayer { alpha = if (isNext) nextDotAlpha else 1f }
                                                .background(
                                                    if (hasSession) contentColor else Color.Transparent,
                                                    CircleShape
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

/** Medio ciclo del respiro del punto de la próxima sesión. */
private const val PulseMillis = 1400
