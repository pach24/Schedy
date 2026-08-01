package com.schednd.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.HomeScheduleCalendar
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.SkeletonMorph
import com.schednd.ui.theme.pressScale
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import com.schednd.R

@Composable
internal fun HomeCalendarTab(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onDayTap: (LocalDate) -> Unit,
    expansion: CalendarExpansionState
) {
    val sessionDates = remember(uiState.allSessions) {
        uiState.allSessions
            .filter { it.confirmedDate != null }
            .associate { it.confirmedDate!! to it.name.ifBlank { it.code } }
    }
    val nextDate = uiState.nextSession?.confirmedDate
    val progress = remember(expansion) { { expansion.progress } }
    // Con el mes abierto el deslizar horizontal cambia de mes. Se saca de `progress` un
    // booleano para no recomponer el calendario en cada frame de la ampliación.
    val swipeMonths by remember(expansion) {
        derivedStateOf { expansion.isOpen }
    }

    // Lo que hay de pantalla, para saber dónde termina. El mes ampliado tiene que morir
    // justo donde empieza la barra, y eso no se puede estimar: depende del alto real y de
    // lo que ocupe el título, que lleva encima la barra de estado.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val pageHeight = maxHeight
    var titleHeightPx by remember { mutableIntStateOf(0) }
    val expandedSpace = pageHeight -
        innerPadding.calculateBottomPadding() -
        with(LocalDensity.current) { titleHeightPx.toDp() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(expansion.nestedScrollConnection)
            .verticalScroll(rememberScrollState())
            // El hueco de la barra se mantiene al final del recorrido: es lo que deja
            // volver a subir la lista por encima de ella cuando se quiere leer entera.
            .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { titleHeightPx = it.height }
                .statusBarsPadding()
                .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.calendar_title),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        FadeIn(delayMs = 60) {
            HomeScheduleCalendar(
                sessionDates = sessionDates,
                nextSessionDate = nextDate,
                onDayTap = onDayTap,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                expansion = progress,
                expandedSpace = expandedSpace,
                swipeMonths = swipeMonths
            )
        }

        // "Fechas confirmadas" mira hacia delante: una sesión ya jugada no es una fecha
        // que haya que tener presente. En la rejilla sí se quedan marcadas, como registro.
        val upcomingSessions = remember(uiState.allSessions) {
            val today = LocalDate.now()
            uiState.allSessions
                .filter { it.confirmedDate != null && !it.confirmedDate.isBefore(today) }
                .sortedBy { it.confirmedDate }
        }

        // Lo de abajo no se va a ninguna parte: el mes, al crecer, lo empuja hacia abajo
        // hasta meterlo por detrás de la barra, que es de cristal y lo deja ver. Sigue ahí
        // para quien lo suba con el dedo.
        Spacer(modifier = Modifier.height(6.dp))
        SkeletonMorph(targetState = uiState.slotFor(upcomingSessions)) { slot ->
            when (slot) {
                SessionsSlot.LOADING -> CalendarListSkeleton()
                SessionsSlot.EMPTY -> Column {
                    Spacer(modifier = Modifier.height(26.dp))
                    EmptyCalendarHint(modifier = Modifier.padding(horizontal = 20.dp))
                }
                SessionsSlot.FILLED -> Column {
                    HomeCalendarSessionList(
                        sessions = upcomingSessions,
                        nextSession = uiState.nextSession,
                        onSessionClick = onDayTap
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun HomeCalendarSessionList(
    sessions: List<HomeSessionCard>,
    nextSession: HomeSessionCard?,
    onSessionClick: (LocalDate) -> Unit
) {
    SectionHeader(
        title = stringResource(R.string.calendar_confirmed_header),
        trailing = "${sessions.size}",
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
    )
    sessions.forEach { session ->
        HomeCalendarSessionRow(
            session = session,
            isNext = session.code == nextSession?.code,
            onClick = { session.confirmedDate?.let(onSessionClick) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HomeCalendarSessionRow(
    session: HomeSessionCard,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    GenCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction),
        onClick = onClick,
        interactionSource = interaction
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2DC653))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name.ifBlank { stringResource(R.string.session_fallback_name) },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                session.confirmedDate?.let { date ->
                    Text(
                        text = dateShortLabel(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isNext) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_badge_next),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCalendarHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.calendar_no_confirmed_title),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.calendar_no_confirmed_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
