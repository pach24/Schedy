package com.schednd.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.res.painterResource
import com.schednd.R
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.ui.components.AppleCard
import com.schednd.ui.components.ComingSoonDayDialog
import com.schednd.ui.components.HomeScheduleCalendar
import com.schednd.ui.session.SessionBottomBar
import com.schednd.ui.session.SessionTab
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.SchedndTheme
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    var selectedTab by rememberSaveable { mutableStateOf(SessionTab.SESSION) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SessionBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == SessionTab.SESSION || tab == SessionTab.CALENDAR) {
                        selectedTab = tab
                    }
                }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 220),
            label = "homeTabCrossfade"
        ) { tab ->
            when (tab) {
                SessionTab.CALENDAR -> HomeCalendarTab(uiState = uiState, innerPadding = innerPadding)
                else -> HomeSessionTab(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onCreateEvent = onCreateEvent,
                    onJoinEvent = onJoinEvent,
                    onOpenEvent = onOpenEvent
                )
            }
        }
    }
}

@Composable
private fun HomeSessionTab(
    uiState: HomeUiState,
    innerPadding: PaddingValues,
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    onOpenEvent: (String) -> Unit
) {
    if (!uiState.isAuthReady && uiState.error == null) {
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
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 0.dp,
            bottom = innerPadding.calculateBottomPadding() + 24.dp
        )
    ) {
        item { Header() }

        if (uiState.error != null) {
            item {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }

        uiState.nextSession?.let { session ->
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
                        HeroDate(session = session)
                        HeroSessionLabel(session = session)
                        HeroCountdown(session = session)
                    }
                }
            }
        }

        if (uiState.allSessions.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "TODAS LAS SESIONES",
                    trailing = "${uiState.allSessions.size}",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }
            items(uiState.allSessions, key = { it.code }) { session ->
                SessionRow(
                    session = session,
                    isNext = session.code == uiState.nextSession?.code,
                    onClick = { onOpenEvent(session.code) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }
        } else {
            item {
                EmptySessionsHint(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp))
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
private fun HomeCalendarTab(
    uiState: HomeUiState,
    innerPadding: PaddingValues
) {
    val sessionDates = remember(uiState.allSessions) {
        uiState.allSessions
            .filter { it.confirmedDate != null }
            .associate { it.confirmedDate!! to it.name.ifBlank { it.code } }
    }
    val nextDate = uiState.nextSession?.confirmedDate
    var tappedDate by remember { mutableStateOf<LocalDate?>(null) }

    if (tappedDate != null) {
        ComingSoonDayDialog(
            date = tappedDate!!,
            onDismiss = { tappedDate = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(bottom = innerPadding.calculateBottomPadding() + 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Text(
                text = "Calendario",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }

        if (sessionDates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            HomeCalendarSessionList(
                sessions = uiState.allSessions.filter { it.confirmedDate != null },
                nextSession = uiState.nextSession,
                onSessionClick = { date -> tappedDate = date }
            )
        } else if (uiState.isAuthReady) {
            Spacer(modifier = Modifier.height(32.dp))
            EmptyCalendarHint(modifier = Modifier.padding(horizontal = 20.dp))
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
        title = "FECHAS CONFIRMADAS",
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
    AppleCard(
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
                    text = session.name.ifBlank { "Sesión" },
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
                        text = "PRÓXIMA",
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
            text = "Sin fechas confirmadas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Cuando una sesión tenga fecha confirmada aparecerá aquí.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun Header() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.weight(1f))
            HeaderIconButton(
                icon = Icons.Filled.Search,
                contentDescription = "Buscar",
                onClick = { /* TODO buscar sesiones */ }
            )
            Spacer(modifier = Modifier.width(8.dp))
            HeaderIconButton(
                icon = Icons.Filled.Add,
                contentDescription = "Nueva sesión",
                onClick = { /* TODO acceso rápido a crear/unirse */ }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hola",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tus sesiones",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interaction)
            .size(40.dp)
            .clip(CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f), CircleShape)
            .clickable(
                indication = LocalIndication.current,
                interactionSource = interaction,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun HeroDate(session: HomeSessionCard, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = "PRÓXIMA SESIÓN",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (session.confirmedDate != null) {
            val dayName = DateTimeFormatter.ofPattern("EEEE", java.util.Locale("es"))
                .format(session.confirmedDate)
                .replaceFirstChar { it.uppercaseChar() }
            val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", java.util.Locale("es"))
                .format(session.confirmedDate)
            Text(
                text = dayName,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 40.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = dayMonth,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = "Fecha por\nconfirmar",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp,
                    lineHeight = 40.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun HeroSessionLabel(session: HomeSessionCard, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = "⚔️", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = session.name.ifBlank { "Sesión" },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "· ${session.code}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HeroCountdown(session: HomeSessionCard, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = LocalDateTime.now()
        }
    }

    val totalMinutes: Long = session.confirmedDate?.let { date ->
        val diff = Duration.between(now, date.atStartOfDay())
        if (diff.isNegative) 0L else diff.toMinutes()
    } ?: 0L

    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes % (60 * 24)) / 60
    val minutes = totalMinutes % 60

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CountdownBox(value = days, label = "DÍAS", modifier = Modifier.weight(1f))
        CountdownBox(value = hours, label = "HORAS", modifier = Modifier.weight(1f))
        CountdownBox(value = minutes, label = "MINUTOS", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CountdownBox(value: Long, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SquircleShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "%02d".format(value),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionRow(
    session: HomeSessionCard,
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    AppleCard(
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
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(SquircleShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.dice_d20_svgrepo_com),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name.ifBlank { "Sesión" },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = session.confirmedDate?.let { dateShortLabel(it) } ?: "Sin fecha · ${session.code}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = if (session.confirmedDate == null) FontFamily.Monospace else FontFamily.Default,
                        letterSpacing = if (session.confirmedDate == null) 1.sp else 0.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            if (isNext) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "PRÓXIMA",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptySessionsHint(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(SquircleShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.dice_d20_svgrepo_com),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Aún no tienes mesas",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Crea una sesión o únete con un código para empezar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ActionButtons(
    onCreateEvent: () -> Unit,
    onJoinEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val createInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(createInteraction)
                .clip(FullRoundShape)
                .background(MaterialTheme.colorScheme.onSurface)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = createInteraction,
                    onClick = onCreateEvent
                )
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Crear sesión",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.surface
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val joinInteraction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(joinInteraction)
                .clip(FullRoundShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), FullRoundShape)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = joinInteraction,
                    onClick = onJoinEvent
                )
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Unirse con código",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun dateShortLabel(date: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es"))
    return date.format(fmt).replaceFirstChar { it.uppercaseChar() }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Home – Vacío (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun HomePreviewEmpty() {
    SchedndTheme(darkTheme = false) {
        HomeContent(
            uiState = HomeUiState(isAuthReady = true),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}

@Preview(name = "Home – Con sesiones (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun HomePreviewLight() {
    SchedndTheme(darkTheme = false) {
        val today = LocalDate.now()
        val sample = listOf(
            HomeSessionCard(
                code = "ABC123",
                name = "D&D semanal",
                confirmedDate = today.plusDays(6),
                participantsCount = 5,
                totalParticipants = 5,
                participantInitials = listOf("A", "M", "D", "P", "O")
            ),
            HomeSessionCard(
                code = "VAM999",
                name = "Vampiro",
                confirmedDate = today.plusDays(20),
                participantsCount = 4,
                totalParticipants = 4,
                participantInitials = listOf("J", "L", "K", "T")
            ),
            HomeSessionCard(
                code = "HAL666",
                name = "One-Shot Halloween",
                confirmedDate = null,
                participantsCount = 0,
                totalParticipants = 0,
                participantInitials = emptyList()
            )
        )
        HomeContent(
            uiState = HomeUiState(
                isAuthReady = true,
                allSessions = sample,
                nextSession = sample.first()
            ),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}

@Preview(name = "Home – Con sesiones (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreviewDark() {
    SchedndTheme(darkTheme = true) {
        val today = LocalDate.now()
        val sample = listOf(
            HomeSessionCard(
                code = "ABC123",
                name = "D&D semanal",
                confirmedDate = today.plusDays(6),
                participantsCount = 5,
                totalParticipants = 5,
                participantInitials = listOf("A", "M", "D", "P", "O")
            )
        )
        HomeContent(
            uiState = HomeUiState(
                isAuthReady = true,
                allSessions = sample,
                nextSession = sample.first()
            ),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}
