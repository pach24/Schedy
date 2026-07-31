package com.schednd.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.ui.theme.CalendarCellShape
import com.schednd.ui.theme.SchedyTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.schednd.R

/**
 * Mini calendario de la semana en curso con el día de hoy destacado y un punto
 * bajo los días que tienen sesión.
 */
@Composable
fun MiniWeekCalendar(
    sessionDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    today: LocalDate = LocalDate.now(),
    onClick: (() -> Unit)? = null,
) {
    val weekStart = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val days = remember(weekStart) { (0L..6L).map { weekStart.plusDays(it) } }

    val interaction = remember { MutableInteractionSource() }
    GenCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        interactionSource = interaction
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = todayLabel(today, stringResource(R.string.date_pattern_today_full)),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.home_open_calendar),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                days.forEach { date ->
                    val isToday = date == today
                    val hasSession = date in sessionDates

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = weekDayInitial(date),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CalendarCellShape)
                                .background(
                                    if (isToday) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    CalendarCellShape
                                )
                                .then(
                                    if (hasSession && !isToday) {
                                        Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = .25f),
                                            CalendarCellShape
                                        )
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday || hasSession) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isToday) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasSession) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                        )
                    }
                }
            }
        }
    }
}

private fun todayLabel(today: LocalDate, pattern: String): String =
    DateTimeFormatter.ofPattern(pattern, Locale.getDefault())
        .format(today)
        .uppercase(Locale.getDefault())

/** La inicial del día en el idioma del móvil; la lista entera vive en [weekDayInitials]. */
private fun weekDayInitial(date: LocalDate): String =
    Locale.getDefault().let { locale ->
        date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale).uppercase(locale)
    }

@Preview(showBackground = true)
@Composable
private fun MiniWeekCalendarPreview() {
    SchedyTheme(darkTheme = false) {
        val today = LocalDate.now()
        MiniWeekCalendar(
            sessionDates = setOf(today.plusDays(2), today.plusDays(4)),
            modifier = Modifier.padding(20.dp)
        )
    }
}
