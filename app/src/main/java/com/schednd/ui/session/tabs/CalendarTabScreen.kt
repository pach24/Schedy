package com.schednd.ui.session.tabs

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.DateSummary
import com.schednd.ui.components.ScheduleCalendar
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.theme.FadeIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarTabScreen(
    bottomPadding: PaddingValues,
    sessionName: String,
    dateSummaries: List<DateSummary>,
    totalParticipants: Int,
    confirmedDate: LocalDate?,
    onBack: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        CalendarHeader(sessionName = sessionName, onBack = onBack)

        FadeIn(delayMs = 80) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = bottomPadding.calculateBottomPadding() + 16.dp
                    )
            ) {
                ScheduleCalendar(
                    dateSummaries = dateSummaries,
                    totalParticipants = totalParticipants,
                    confirmedDate = confirmedDate,
                    modifier = Modifier.fillMaxWidth()
                )

                if (totalParticipants > 0) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HeatmapLegend(
                        totalParticipants = totalParticipants,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                confirmedDate?.let { date ->
                    Spacer(modifier = Modifier.height(20.dp))
                    ConfirmedDateCard(date = date)
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(sessionName: String, onBack: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val btnBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 0.9f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, btnBorder, CircleShape)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = interaction,
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Volver",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp).padding(start = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = "Calendario",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (sessionName.isNotBlank()) {
                Text(
                    text = sessionName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HeatmapLegend(
    totalParticipants: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Poca",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        val steps = listOf(1, 2, 3, 4, 5, 6, totalParticipants)
        steps.forEach { step ->
            val count = ((step.toFloat() / 7f) * totalParticipants).toInt().coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(getHeatmapColor(count, totalParticipants))
            )
            Spacer(modifier = Modifier.width(3.dp))
        }
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = "Todos",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfirmedDateCard(date: LocalDate) {
    val dayName = DateTimeFormatter.ofPattern("EEEE", Locale("es"))
        .format(date).replaceFirstChar { it.uppercaseChar() }
    val fullDate = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale("es"))
        .format(date)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF2DC653))
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = "Fecha confirmada",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$dayName, $fullDate",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
