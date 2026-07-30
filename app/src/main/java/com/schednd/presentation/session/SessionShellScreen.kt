package com.schednd.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.schednd.ui.components.ComingSoonDayDialog
import com.schednd.ui.components.DialogBlurRadius
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.presentation.detail.EventDetailScreen
import com.schednd.presentation.detail.EventDetailViewModel
import com.schednd.presentation.session.tabs.CalendarTabScreen
import com.schednd.presentation.session.tabs.ProfileTabScreen
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SessionShellScreen(
    eventDetailViewModel: EventDetailViewModel,
    onLeaveSession: () -> Unit,
    onEditAvailability: () -> Unit
) {
    val eventState by eventDetailViewModel.uiState.collectAsState()

    // Igual que en Home: la barra vive fuera del Scaffold porque el shader refracta lo
    // que hay dentro de esa capa, y la barra no puede refractarse a sí misma.
    val glass = rememberLiquidGlassState()
    // Fondo del diálogo de día: el cristal refracta el contenido del Scaffold, y por
    // debajo de API 33 el mismo contenido se difumina con Haze. Lleva capa aparte de la
    // barra porque el radio de difuminado es un uniform del backdrop, no de la pieza, y
    // el diálogo lo quiere bastante más alto que la bolita.
    val hazeState = remember { HazeState() }
    val dayDialogGlass = rememberLiquidGlassState()
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }
    val navigationBarsBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    // Dentro de una sesión el listado general no tiene sentido.
    val tabs = remember { SessionTab.entries.filterNot { it == SessionTab.SESSIONS } }
    // El pager es la única fuente de la pestaña actual: manda tanto al deslizar como al
    // tocar la barra, y es su posición continua la que mueve la bolita.
    val pagerState = rememberPagerState { tabs.size }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
                .liquidGlassBackdrop(dayDialogGlass, blurRadius = DialogBlurRadius)
                .liquidGlassBackdrop(glass)
                .background(MaterialTheme.colorScheme.background),
            containerColor = Color.Transparent,
            bottomBar = {
                // Hueco del alto de la barra real, para que innerPadding siga valiendo.
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SessionBottomBarHeight + navigationBarsBottom)
                )
            }
        ) { innerPadding ->
            // El pager guarda el estado de cada página al descartarla y lo restaura al
            // volver, así que el revelado escalonado de `FadeIn` no se repite en cada cambio.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { tabs[it].name }
            ) { page ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tabs[page]) {
                        SessionTab.CALENDAR -> CalendarTabScreen(
                            bottomPadding = innerPadding,
                            sessionName = eventState.event?.name.orEmpty(),
                            dateSummaries = eventState.dateSummaries,
                            totalParticipants = eventState.participants.size,
                            confirmedDate = eventState.confirmedDate,
                            onDayTap = { date -> tappedDate = date },
                            onBack = onLeaveSession
                        )
                        SessionTab.PROFILE -> ProfileTabScreen(
                            bottomPadding = innerPadding,
                            onBack = onLeaveSession
                        )
                        else -> EventDetailScreen(
                            viewModel = eventDetailViewModel,
                            bottomPadding = innerPadding,
                            onBack = onLeaveSession,
                            onEditAvailability = onEditAvailability
                        )
                    }
                }
            }
        }

        SessionBottomBar(
            position = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            items = tabs,
            onTabSelected = { tab ->
                val target = tabs.indexOf(tab)
                // Tocar "Inicio" estando ya en él devuelve al listado de sesiones. Se mira
                // el destino y no la página actual: a mitad de un recorrido hacia otra
                // pestaña, tocar "Inicio" es querer volver a él, no salirse.
                if (tab == SessionTab.HOME && pagerState.targetPage == target) {
                    onLeaveSession()
                } else {
                    scope.launch { pagerState.animateToTabPage(target) }
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            glass = glass
        )

        tappedDate?.let { date ->
            ComingSoonDayDialog(
                date = date,
                hazeState = hazeState,
                glass = dayDialogGlass,
                onDismiss = { tappedDate = null }
            )
        }
    }
}
