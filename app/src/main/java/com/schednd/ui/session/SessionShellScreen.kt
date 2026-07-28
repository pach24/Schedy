package com.schednd.ui.session

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.ui.detail.EventDetailScreen
import com.schednd.ui.detail.EventDetailViewModel
import com.schednd.ui.session.tabs.CalendarTabScreen
import com.schednd.ui.session.tabs.ProfileTabScreen

@Composable
fun SessionShellScreen(
    eventDetailViewModel: EventDetailViewModel,
    onLeaveSession: () -> Unit,
    onEditAvailability: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(SessionTab.HOME) }
    val eventState by eventDetailViewModel.uiState.collectAsState()

    // Igual que en Home: la barra vive fuera del Scaffold porque el shader refracta lo
    // que hay dentro de esa capa, y la barra no puede refractarse a sí misma.
    val glass = rememberLiquidGlassState()
    val navigationBarsBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    // Dentro de una sesión el listado general no tiene sentido.
    val tabs = remember { SessionTab.entries.filterNot { it == SessionTab.SESSIONS } }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
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
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(durationMillis = 130),
                label = "shellCrossfade"
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (tab) {
                        SessionTab.HOME, SessionTab.SESSIONS -> EventDetailScreen(
                            viewModel = eventDetailViewModel,
                            bottomPadding = innerPadding,
                            onBack = onLeaveSession,
                            onEditAvailability = onEditAvailability
                        )
                        SessionTab.CALENDAR -> CalendarTabScreen(
                            bottomPadding = innerPadding,
                            sessionName = eventState.event?.name.orEmpty(),
                            dateSummaries = eventState.dateSummaries,
                            totalParticipants = eventState.participants.size,
                            confirmedDate = eventState.confirmedDate,
                            onBack = onLeaveSession
                        )
                        SessionTab.PROFILE -> ProfileTabScreen(
                            bottomPadding = innerPadding,
                            onBack = onLeaveSession
                        )
                    }
                }
            }
        }

        SessionBottomBar(
            selectedTab = selectedTab,
            items = tabs,
            onTabSelected = { tab ->
                // Tocar "Inicio" estando ya en él devuelve al listado de sesiones.
                if (tab == SessionTab.HOME && selectedTab == SessionTab.HOME) {
                    onLeaveSession()
                } else {
                    selectedTab = tab
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
            glass = glass
        )
    }
}
