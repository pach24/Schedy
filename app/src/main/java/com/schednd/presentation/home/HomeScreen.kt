package com.schednd.presentation.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.ui.components.DayDialogSession
import com.schednd.ui.components.DialogBlurRadius
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.MiniWeekCalendar
import com.schednd.ui.components.SessionDayDialog
import com.schednd.ui.components.heroSurfaceColor
import com.schednd.ui.components.rimHighlightBrush
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.presentation.session.SessionBottomBar
import com.schednd.presentation.session.rememberTabNavigator
import com.schednd.presentation.session.SessionBottomBarHeight
import com.schednd.presentation.session.SessionTab
import com.schednd.presentation.session.tabs.ProfileTabScreen
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.RowRemovalDurationMs
import com.schednd.ui.theme.RowRemovalExit
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.delay
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.schednd.R

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
        onOpenEvent = onOpenEvent,
        onRemoveSession = viewModel::removeSession
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onOpenEvent: (String) -> Unit,
    onRemoveSession: (HomeSessionCard) -> Unit = {}
) {
    // El pager manda sobre la pestaña actual, tanto al deslizar como al tocar la barra.
    // La bolita la mueve el navigator: pegada al pager con el dedo, por su cuenta al tocar.
    val tabs = SessionTab.entries
    val pagerState = rememberPagerState { tabs.size }
    val navigator = rememberTabNavigator(pagerState)
    fun goToTab(tab: SessionTab) {
        navigator.goTo(tabs.indexOf(tab))
    }

    // La barra se dibuja fuera del Scaffold: el shader del cristal refracta lo que hay
    // dentro del Scaffold, así que la barra no puede formar parte de esa capa.
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

    // Sesión que se mantuvo pulsada: mientras haya una, su hoja de acciones está abierta.
    var sessionUnderAction by remember { mutableStateOf<HomeSessionCard?>(null) }
    // Sesión cayéndose del listado. La fila se va antes de tocar nada: borrar o salirse
    // tarda lo que tarde Firestore, y esperar la respuesta para animar deja un tirón.
    //
    // Ambas viven aquí y no en la pestaña porque el pager descarta las páginas que no se
    // ven: dentro, deslizar a otra pestaña a media caída se llevaría por delante la espera
    // y la sesión se quedaría sin borrar.
    var leavingSession by remember { mutableStateOf<HomeSessionCard?>(null) }

    LaunchedEffect(leavingSession) {
        val leaving = leavingSession ?: return@LaunchedEffect
        delay(RowRemovalDurationMs.toLong())
        onRemoveSession(leaving)
    }

    // La fila se queda fuera hasta que el listado deje de traerla; soltarla antes la haría
    // reaparecer durante el viaje de ida y vuelta. Si la cosa falló y ahí sigue, vuelve.
    LaunchedEffect(uiState) {
        val leaving = leavingSession ?: return@LaunchedEffect
        val gone = uiState.allSessions.none { it.code == leaving.code }
        if (gone || uiState.error != null) leavingSession = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .liquidGlassBackdrop(dayDialogGlass, blurRadius = DialogBlurRadius)
                .liquidGlassBackdrop(glass),
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
            // El pager guarda el estado de cada página al descartarla y lo restaura al
            // volver, así que el revelado escalonado de `FadeIn` no se repite en cada cambio.
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { tabs[it].name }
            ) { page ->
                when (tabs[page]) {
                    SessionTab.CALENDAR -> HomeCalendarTab(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onDayTap = { date -> tappedDate = date }
                    )
                    SessionTab.PROFILE -> ProfileTabScreen(
                        bottomPadding = innerPadding,
                        onBack = { goToTab(SessionTab.HOME) }
                    )
                    SessionTab.SESSIONS -> HomeSessionsTab(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onCreateEvent = onCreateEvent,
                        onJoinEvent = onJoinEvent,
                        onOpenEvent = onOpenEvent,
                        leavingCode = leavingSession?.code,
                        onLongPressSession = { sessionUnderAction = it }
                    )
                    SessionTab.HOME -> HomeMainTab(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onCreateEvent = onCreateEvent,
                        onJoinEvent = onJoinEvent,
                        onSeeAllSessions = { goToTab(SessionTab.SESSIONS) },
                        onOpenCalendar = { goToTab(SessionTab.CALENDAR) },
                        onOpenEvent = onOpenEvent
                    )
                }
            }
        }

        SessionBottomBar(
            position = navigator::position,
            hop = navigator::hop,
            items = tabs,
            onTabSelected = { tab -> goToTab(tab) },
            modifier = Modifier.align(Alignment.BottomCenter),
            glass = glass
        )

        sessionUnderAction?.let { session ->
            SessionActionsSheet(
                session = session,
                onConfirm = {
                    sessionUnderAction = null
                    leavingSession = session
                },
                onDismiss = { sessionUnderAction = null }
            )
        }

        // La rejilla solo deja tocar días con sesión, así que la lista nunca debería venir
        // vacía; si viniera, no hay diálogo que enseñar.
        tappedDate?.let { date ->
            val sessionsThatDay = remember(date, uiState.allSessions) {
                uiState.allSessions
                    .filter { it.confirmedDate == date }
                    .map { DayDialogSession(it.code, it.name, it.startTime) }
            }
            if (sessionsThatDay.isNotEmpty()) {
                SessionDayDialog(
                    date = date,
                    sessions = sessionsThatDay,
                    hazeState = hazeState,
                    glass = dayDialogGlass,
                    onOpenSession = { code ->
                        tappedDate = null
                        onOpenEvent(code)
                    },
                    onDismiss = { tappedDate = null }
                )
            }
        }
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
    onOpenCalendar: () -> Unit,
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
        item {
            val name = uiState.playerName
            Header(
                title = stringResource(R.string.home_title),
                // Sin nombre guardado el saludo se queda a secas, que es como estaba.
                greeting = if (name.isNullOrBlank()) {
                    stringResource(R.string.home_greeting)
                } else {
                    stringResource(R.string.home_greeting_named, name)
                }
            )
        }

        if (uiState.error != null) {
            item {
                Text(
                    text = stringResource(R.string.error_prefix, uiState.error ?: ""),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        item {
            FadeIn(delayMs = 50) {
                val session = uiState.nextSession
                val heroInteraction = remember { MutableInteractionSource() }
                val heroShape = SquircleShape(24.dp)
                // Sin próxima sesión la tarjeta es solo informativa: no hay adónde llevar.
                val openHero = session?.let { { onOpenEvent(it.code) } }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .then(
                            if (openHero != null) Modifier.pressScale(heroInteraction) else Modifier
                        )
                        .clip(heroShape)
                        .background(heroSurfaceColor())
                        // El cristal de verdad no cabe aquí: esta tarjeta va dentro del
                        // contenido que el shader refracta y se refractaría a sí misma.
                        // El filo sí, pintado, igual que en el resto de tarjetas.
                        .border(1.dp, rimHighlightBrush(), heroShape)
                        .then(
                            if (openHero != null) {
                                Modifier.clickable(
                                    indication = LocalIndication.current,
                                    interactionSource = heroInteraction,
                                    onClick = openHero
                                )
                            } else Modifier
                        )
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
    onOpenEvent: (String) -> Unit,
    leavingCode: String?,
    onLongPressSession: (HomeSessionCard) -> Unit
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
        item { Header(title = stringResource(R.string.tab_sessions)) }

        if (uiState.allSessions.isEmpty()) {
            item {
                EmptySessionsHint(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp))
            }
        } else {
            item {
                SectionHeader(
                    title = stringResource(R.string.home_upcoming_header),
                    trailing = "${uiState.upcomingSessions.size}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            if (uiState.upcomingSessions.isEmpty()) {
                item {
                    SectionEmptyHint(
                        text = stringResource(R.string.home_upcoming_empty),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
            removableSessionRows(
                sessions = uiState.upcomingSessions,
                keyPrefix = "upcoming",
                nextCode = uiState.nextSession?.code,
                leavingCode = leavingCode,
                onOpenEvent = onOpenEvent,
                onLongPress = onLongPressSession
            )

            item {
                SectionHeader(
                    title = stringResource(R.string.home_past_header),
                    trailing = "${uiState.pastSessions.size}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            if (uiState.pastSessions.isEmpty()) {
                item {
                    SectionEmptyHint(
                        text = stringResource(R.string.home_past_empty),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            }
            removableSessionRows(
                sessions = uiState.pastSessions,
                keyPrefix = "past",
                // Ninguna sesión pasada puede ser la próxima, así que nunca lleva la chapa.
                nextCode = null,
                leavingCode = leavingCode,
                onOpenEvent = onOpenEvent,
                onLongPress = onLongPressSession
            )
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

/**
 * Filas de sesión que pueden irse del listado. La que esté en [leavingCode] se cae en vez
 * de esfumarse; sigue en la lista mientras dura la caída, porque lo que se anima tiene que
 * estar compuesto para poder animarse.
 */
private fun LazyListScope.removableSessionRows(
    sessions: List<HomeSessionCard>,
    keyPrefix: String,
    nextCode: String?,
    leavingCode: String?,
    onOpenEvent: (String) -> Unit,
    onLongPress: (HomeSessionCard) -> Unit
) {
    items(sessions, key = { "$keyPrefix-${it.code}" }) { session ->
        AnimatedVisibility(
            visible = session.code != leavingCode,
            exit = RowRemovalExit,
            // Las de debajo suben a ocupar el hueco en vez de dar el salto.
            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null)
        ) {
            SessionRow(
                session = session,
                isNext = session.code == nextCode,
                onClick = { onOpenEvent(session.code) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                onLongClick = { onLongPress(session) }
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
            .pressScale(interaction),
        onClick = onClick,
        interactionSource = interaction
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(R.plurals.sessions_count, total, total),
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
