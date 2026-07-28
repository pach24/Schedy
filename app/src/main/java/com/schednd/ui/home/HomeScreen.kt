package com.schednd.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.MiniWeekCalendar
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.ui.session.SessionBottomBar
import com.schednd.ui.session.SessionBottomBarHeight
import com.schednd.ui.session.SessionTab
import com.schednd.ui.session.tabs.ProfileTabScreen
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale

@Composable
fun HomeScreen(
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onOpenEvent: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.refresh()
    }

    HomeContent(
        uiState = uiState,
        onCreateEvent = onCreateEvent,
        onJoinEvent = onJoinEvent,
        onOpenEvent = onOpenEvent
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onOpenEvent: (String) -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(SessionTab.HOME) }

    // La barra se dibuja fuera del Scaffold: el shader del cristal refracta lo que hay
    // dentro del Scaffold, así que la barra no puede formar parte de esa capa.
    val glass = rememberLiquidGlassState()
    val navigationBarsBottom = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.liquidGlassBackdrop(glass),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                // Hueco del mismo alto que la barra real, para que innerPadding siga valiendo.
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
                label = "homeTabCrossfade"
            ) { tab ->
                when (tab) {
                    SessionTab.CALENDAR -> HomeCalendarTab(uiState = uiState, innerPadding = innerPadding)
                    SessionTab.PROFILE -> ProfileTabScreen(
                        bottomPadding = innerPadding,
                        onBack = { selectedTab = SessionTab.HOME }
                    )
                    SessionTab.SESSIONS -> HomeSessionsTab(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onCreateEvent = onCreateEvent,
                        onJoinEvent = onJoinEvent,
                        onOpenEvent = onOpenEvent
                    )
                    SessionTab.HOME -> HomeMainTab(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onCreateEvent = onCreateEvent,
                        onJoinEvent = onJoinEvent,
                        onSeeAllSessions = { selectedTab = SessionTab.SESSIONS },
                        onOpenCalendar = { selectedTab = SessionTab.CALENDAR }
                    )
                }
            }
        }

        SessionBottomBar(
            selectedTab = selectedTab,
            onTabSelected = { tab -> selectedTab = tab },
            modifier = Modifier.align(Alignment.BottomCenter),
            glass = glass
        )
    }
}

/** Pestaña Inicio: cuenta atrás de la próxima sesión y accesos rápidos. */
@Composable
private fun HomeMainTab(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onSeeAllSessions: () -> Unit,
    onOpenCalendar: () -> Unit
) {
    if (!uiState.isAuthReady && uiState.error == null) {
        LoadingTab(innerPadding = innerPadding)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item { Header(title = "Tus sesiones", greeting = "Hola") }

        if (uiState.error != null) {
            item {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        item {
            FadeIn(delayMs = 50) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(SquircleShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    val session = uiState.nextSession
                    if (session != null) {
                        HeroDate(session = session)
                        HeroSessionLabel(session = session)
                        HeroCountdown(session = session)
                    } else {
                        NoUpcomingSessionHero()
                    }
                }
            }
        }

        item {
            val sessionDays = remember(uiState.allSessions) {
                uiState.allSessions.mapNotNull { it.confirmedDate }.toSet()
            }
            FadeIn(delayMs = 90) {
                MiniWeekCalendar(
                    sessionDates = sessionDays,
                    onClick = onOpenCalendar,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        if (uiState.allSessions.isEmpty()) {
            item {
                EmptySessionsHint(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp))
            }
        } else {
            item {
                SeeAllSessionsRow(
                    total = uiState.allSessions.size,
                    onClick = onSeeAllSessions,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
        item {
            ActionButtons(
                onCreateEvent = onCreateEvent,
                onJoinEvent = onJoinEvent,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

/** Pestaña Sesiones: el listado completo, próximas y pasadas. */
@Composable
private fun HomeSessionsTab(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onOpenEvent: (String) -> Unit
) {
    if (!uiState.isAuthReady && uiState.error == null) {
        LoadingTab(innerPadding = innerPadding)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item { Header(title = "Sesiones") }

        if (uiState.allSessions.isEmpty()) {
            item {
                EmptySessionsHint(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp))
            }
        } else {
            item {
                SectionHeader(
                    title = "PRÓXIMAS SESIONES",
                    trailing = "${uiState.upcomingSessions.size}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            if (uiState.upcomingSessions.isEmpty()) {
                item {
                    SectionEmptyHint(
                        text = "No hay sesiones próximas.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
            items(uiState.upcomingSessions, key = { "upcoming-${it.code}" }) { session ->
                SessionRow(
                    session = session,
                    isNext = session.code == uiState.nextSession?.code,
                    onClick = { onOpenEvent(session.code) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            item {
                SectionHeader(
                    title = "SESIONES PASADAS",
                    trailing = "${uiState.pastSessions.size}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            if (uiState.pastSessions.isEmpty()) {
                item {
                    SectionEmptyHint(
                        text = "Todavía no hay sesiones pasadas.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
            items(uiState.pastSessions, key = { "past-${it.code}" }) { session ->
                SessionRow(
                    session = session,
                    isNext = false,
                    onClick = { onOpenEvent(session.code) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(28.dp)) }
        item {
            ActionButtons(
                onCreateEvent = onCreateEvent,
                onJoinEvent = onJoinEvent,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun LoadingTab(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun SeeAllSessionsRow(
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    GenCard(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Todas las sesiones",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (total == 1) "1 sesión" else "$total sesiones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
