package com.schednd.presentation.session.tabs

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.DateSummary
import com.schednd.ui.components.CalendarExpansionState
import com.schednd.ui.components.ScheduleCalendar
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.theme.FadeIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.schednd.R

@Composable
fun CalendarTabScreen(
    bottomPadding: PaddingValues,
    sessionName: String,
    dateSummaries: List<DateSummary>,
    totalParticipants: Int,
    confirmedDate: LocalDate?,
    onDayTap: (LocalDate) -> Unit,
    onBack: () -> Unit,
    expansion: CalendarExpansionState
) {
    val isDark = isSystemInDarkTheme()
    val progress = remember(expansion) { { expansion.progress } }
    // Con el mes abierto el deslizar horizontal cambia de mes. Se saca de `progress` un
    // booleano para no recomponer el calendario en cada frame de la ampliación.
    val swipeMonths by remember(expansion) {
        derivedStateOf { expansion.isOpen }
    }

    // Lo que hay de pantalla, para saber dónde termina. El mes ampliado tiene que morir
    // justo donde empieza la barra, y eso no se puede estimar: depende del alto real y de
    // lo que ocupe la cabecera, que lleva encima la barra de estado.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val pageHeight = maxHeight
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val expandedSpace = pageHeight -
        bottomPadding.calculateBottomPadding() -
        with(LocalDensity.current) { headerHeightPx.toDp() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // La cabecera se queda fuera del desplazamiento, al contrario que el título de
        // Inicio: aquí lleva el botón de volver, y perderlo de vista al tirar del mes
        // dejaría la pestaña sin salida más que por la barra.
        CalendarHeader(
            sessionName = sessionName,
            onBack = onBack,
            modifier = Modifier.onSizeChanged { headerHeightPx = it.height }
        )

        FadeIn(delayMs = 80) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(expansion.nestedScrollConnection)
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
                    onDayTap = onDayTap,
                    modifier = Modifier.fillMaxWidth(),
                    expansion = progress,
                    expandedSpace = expandedSpace,
                    swipeMonths = swipeMonths
                )

                // Lo de abajo no se va a ninguna parte: el mes, al crecer, lo empuja hacia
                // abajo hasta meterlo por detrás de la barra, que es de cristal y lo deja
                // ver. Sigue ahí para quien lo suba con el dedo.
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
}

@Composable
private fun CalendarHeader(
    sessionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val btnBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 0.9f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )

    Row(
        modifier = modifier
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
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp).padding(start = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(R.string.calendar_title),
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
            text = stringResource(R.string.calendar_legend_low),
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
            text = stringResource(R.string.calendar_legend_all),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConfirmedDateCard(date: LocalDate) {
    val dayName = DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_weekday), Locale.getDefault())
        .format(date).replaceFirstChar { it.uppercaseChar() }
    val fullDate = DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_full_date), Locale.getDefault())
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
                text = stringResource(R.string.calendar_confirmed_date),
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
