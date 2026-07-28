package com.schednd.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.schednd.ui.theme.CalendarCellShape
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarGrid(
    selectedDates: Set<LocalDate>,
    onDateToggled: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    selectableDates: Set<LocalDate>? = null,
    minDate: LocalDate = LocalDate.now(),
    dateAttendeeCount: Map<LocalDate, Int> = emptyMap(),
    mySavedDates: Set<LocalDate> = emptySet()
) {
    var currentMonth by remember { mutableStateOf(YearMonth.from(minDate)) }
    // Track direction for slide animation
    var slideDirection by remember { mutableIntStateOf(1) }

    Column(modifier = modifier) {
        // Month navigation header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    val prev = currentMonth.minusMonths(1)
                    if (!prev.isBefore(YearMonth.from(minDate))) {
                        slideDirection = -1
                        currentMonth = prev
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
            }
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale("es")).replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge
            )
            IconButton(onClick = {
                slideDirection = 1
                currentMonth = currentMonth.plusMonths(1)
            }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
            }
        }

        // Day-of-week headers
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            val dayNames = listOf("L", "M", "X", "J", "V", "S", "D")
            dayNames.forEach { day ->
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

        // Animated month transition — spring-based
        val dir = slideDirection
        AnimatedContent(
            targetState = currentMonth,
            transitionSpec = {
                (slideInHorizontally(
                    spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                ) { dir * (it / 3) } + fadeIn(tween(300))) togetherWith
                        (slideOutHorizontally(
                            spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow)
                        ) { -dir * (it / 3) } + fadeOut(tween(250)))
            },
            label = "MonthTransition"
        ) { month ->
            Column {
                val firstDayOfMonth = month.atDay(1)
                val startOffset = firstDayOfMonth.dayOfWeek.value - 1
                val daysInMonth = month.lengthOfMonth()

                val cells = buildList {
                    repeat(startOffset) { add(null) }
                    for (day in 1..daysInMonth) {
                        add(month.atDay(day))
                    }
                }

                cells.chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) {
                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isPast = date.isBefore(minDate)
                                val isSelectable = !isPast && (selectableDates == null || date in selectableDates)
                                val isSelected = date in selectedDates
                                val isToday = date == LocalDate.now()

                                // Animated selection
                                val bgColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    animationSpec = tween(250),
                                    label = "dateBg"
                                )
                                val scale by animateFloatAsState(
                                    targetValue = if (isSelected) 1f else 0.88f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "dateScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(3.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .clip(CalendarCellShape)
                                        .background(bgColor, CalendarCellShape)
                                        .then(
                                            if (isToday && !isSelected) {
                                                Modifier.border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                                    CalendarCellShape
                                                )
                                            } else Modifier
                                        )
                                        .then(
                                            if (isSelectable) {
                                                Modifier.clickable { onDateToggled(date) }
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val serverCount = dateAttendeeCount[date] ?: 0
                                    val count = maxOf(0, serverCount
                                            + (if (date in selectedDates) 1 else 0)
                                            - (if (date in mySavedDates) 1 else 0)
                                    )
                                    val textColor = when {
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        isPast -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        !isSelectable -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${date.dayOfMonth}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = textColor
                                        )
                                        if (count > 0) {
                                            Text(
                                                text = "+$count",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 8.sp
                                                ),
                                                color = if (isSelected)
                                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                else
                                                    MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Fill remaining slots in the last row
                        repeat(7 - week.size) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}
