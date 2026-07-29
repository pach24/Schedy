package com.schednd.ui.detail

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.R
import com.schednd.domain.model.AttendanceTier
import java.time.LocalDate
import java.time.LocalTime
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.GenTopBar
import com.schednd.ui.components.AvailabilityGrid
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SquircleMiniShape
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit,
    onEditAvailability: () -> Unit,
    bottomPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp)
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreDialog by remember { mutableStateOf(false) }
    var showConfirmDateDialog by remember { mutableStateOf(false) }
    var pendingConfirmDate by remember { mutableStateOf<LocalDate?>(null) }
    var copiedCode by remember { mutableStateOf(false) }
    var copyBounceScale by remember { mutableStateOf(1f) }
    val copyIconScale by animateFloatAsState(
        targetValue = copyBounceScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "copyIconScale"
    )
    LaunchedEffect(copiedCode) {
        if (copiedCode) {
            copyBounceScale = 1.4f
            delay(80)
            copyBounceScale = 1f
            delay(1500)
            copiedCode = false
        }
    }
    val hazeState = remember { HazeState() }
    // Cristal propio de esta pantalla: refracta el contenido del Scaffold, que es justo
    // lo que tienen detrás la top bar y los diálogos. Es un estado distinto al de la
    // barra inferior, porque cada uno refracta una capa distinta.
    val overlayGlass = rememberLiquidGlassState()
    val scrollState = rememberScrollState()
    var confirmedCardY by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBack()
    }

    var scrollToConfirmed by remember { mutableStateOf(false) }
    LaunchedEffect(scrollToConfirmed) {
        if (scrollToConfirmed) {
            scrollToConfirmed = false
            delay(80)
            scrollState.animateScrollTo(confirmedCardY)
        }
    }

    val anyDialogOpen = showDeleteDialog || showMoreDialog || showConfirmDateDialog

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

    Scaffold(
            modifier = Modifier
                .hazeSource(state = hazeState)
                .liquidGlassBackdrop(overlayGlass, blurRadius = 12.dp)
                .then(if (anyDialogOpen) Modifier.blur(4.dp) else Modifier),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            topBar = {}
        ) { innerPadding ->

        // El contenido pasa por detrás de la top bar de cristal, pero no debe invadir la
        // barra de estado. Se recorta el dibujado justo por debajo de ella: es un recorte
        // de pintado, no de layout, así que nada se mueve de sitio y el cristal sigue
        // viendo lo que tiene detrás.
        val statusBarHeightPx = with(LocalDensity.current) {
            WindowInsets.statusBars.getTop(this).toFloat()
        }

        Box(
            modifier = Modifier
                .padding(innerPadding)
                .drawWithContent {
                    clipRect(top = statusBarHeightPx) {
                        this@drawWithContent.drawContent()
                    }
                }
        ) {
            // Animated content states — scale+fade
            AnimatedContent(
                targetState = when {
                    uiState.isLoading -> "loading"
                    uiState.error != null -> "error"
                    else -> "content"
                },
                transitionSpec = { PhaseEnterTransition togetherWith PhaseExitTransition },
                label = "DetailContent"
            ) { state ->
            when (state) {
                "loading" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
                "error" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // 3. AÑADIR un Spacer que empuje el contenido hacia abajo al inicio,
                        // pero que permita que todo fluya hacia arriba al hacer scroll.
                        Spacer(
                            modifier = Modifier
                                .statusBarsPadding()
                                .height(72.dp) // 64.dp de tu topBar + 8.dp de tu Spacer original
                        )

                        if (uiState.participants.isEmpty()) {
                            FadeIn(delayMs = 200) {
                                GenCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stringResource(R.string.detail_no_participants),
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            val confirmedDateLocal = uiState.confirmedDate
                            val sessionIsPast = confirmedDateLocal != null && confirmedDateLocal.isBefore(LocalDate.now())

                            if (confirmedDateLocal != null) {
                                FadeIn(delayMs = 100) {
                                    SessionCountdown(
                                        confirmedDate = confirmedDateLocal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, bottom = 20.dp)
                                    )
                                }
                            }

                            if (!sessionIsPast) {
                            // ── Grid de disponibilidad + leyenda + recomendadas ──

                            FadeIn(delayMs = 200) {
                                AvailabilityGrid(
                                    dates = uiState.datesAsLocal,
                                    participants = uiState.participants,
                                    participantAvailability = uiState.participantAvailability,
                                )
                            }

                            val recommended = uiState.dateSummaries.filter {
                                it.tier == AttendanceTier.FULL || it.tier == AttendanceTier.VIABLE
                            }


// LEYENDA ESTILO GITHUB ACTUALIZADA POR UX
                            FadeIn(delayMs = 250) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Etiqueta principal
                                    Text(
                                        text = stringResource(R.string.detail_availability_legend),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Texto "Baja"
                                    Text(
                                        text = stringResource(R.string.detail_availability_low),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Heatmap squares
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        repeat(7) { level ->
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp) // Reducido para mayor elegancia
                                                    .clip(SquircleMiniShape)
                                                    .background(getHeatmapColor(level, 6))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Texto "Alta"
                                    Text(
                                        text = stringResource(R.string.detail_availability_high),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (recommended.isNotEmpty()) {
                                val dateFormat = DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_day_month), Locale.getDefault())
                                Spacer(modifier = Modifier.height(16.dp))
                                FadeIn(delayMs = 300) {
                                    GenCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = stringResource(R.string.detail_recommended_title),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            recommended.forEach { s ->
                                                val label = if (s.absentNames.isEmpty())
                                                    stringResource(R.string.detail_recommended_full, s.count, s.total)
                                                else
                                                    stringResource(
                                                        R.string.detail_recommended_partial,
                                                        s.count,
                                                        s.total,
                                                        s.absentNames.joinToString(", ")
                                                    )
                                                Text(
                                                    text = stringResource(
                                                        R.string.detail_recommended_row,
                                                        dateFormat.format(s.date),
                                                        label
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            } // end else (not sessionIsPast)
                        }

                        Spacer(
                            modifier = Modifier
                                .height(20.dp)
                                .onGloballyPositioned { coords ->
                                    confirmedCardY = coords.positionInParent().y.toInt()
                                }
                        )

                        AnimatedVisibility(
                            visible = uiState.confirmedDate != null,
                            enter = scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                initialScale = 0.72f
                            ) + fadeIn(tween(200)),
                            exit = scaleOut(tween(160), targetScale = 0.88f) + fadeOut(tween(160))
                        ) {
                            val confirmedFormat = DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_day_month), Locale.getDefault())
                            val startTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
                            Column {
                                GenCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .animateEnterExit(
                                                    enter = scaleIn(
                                                        spring(
                                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                                            stiffness = Spring.StiffnessLow
                                                        ),
                                                        initialScale = 0f
                                                    ),
                                                    exit = scaleOut(tween(80))
                                                )
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF1A95FF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = stringResource(R.string.detail_chosen_date),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = uiState.confirmedDate?.let { date ->
                                                    val day = confirmedFormat.format(date)
                                                    uiState.startTime
                                                        ?.let {
                                                            context.getString(
                                                                R.string.detail_date_with_time,
                                                                day,
                                                                startTimeFormat.format(it)
                                                            )
                                                        }
                                                        ?: day
                                                } ?: "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                GenActionButton(
                                    onClick = {
                                        uiState.confirmedDate?.let { date ->
                                            val time = uiState.startTime
                                            // Con hora fijada creamos un evento real de 3 h;
                                            // sin ella, uno de día completo como hasta ahora.
                                            val zone = ZoneId.systemDefault()
                                            val start = (time?.let { date.atTime(it) }
                                                ?: date.atStartOfDay())
                                                .atZone(zone).toInstant().toEpochMilli()
                                            val end = if (time != null) {
                                                start + SESSION_DEFAULT_DURATION_MS
                                            } else {
                                                start
                                            }
                                            val intent = Intent(Intent.ACTION_INSERT).apply {
                                                data = CalendarContract.Events.CONTENT_URI
                                                putExtra(
                                                    CalendarContract.Events.TITLE,
                                                    uiState.event?.name?.ifBlank { null } ?: context.getString(R.string.detail_calendar_event_title)
                                                )
                                                putExtra(
                                                    CalendarContract.EXTRA_EVENT_ALL_DAY,
                                                    time == null
                                                )
                                                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                                                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                                            }
                                            runCatching { context.startActivity(intent) }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.detail_add_to_calendar),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        FadeIn(delayMs = 300) {
                            GenCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.detail_code_label),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = uiState.event?.code ?: "",
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 4.sp
                                            ),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setText(
                                            AnnotatedString(uiState.event?.code ?: "")
                                        )
                                        copiedCode = true
                                    }) {
                                        Box(modifier = Modifier.scale(copyIconScale)) {
                                            AnimatedContent(
                                                targetState = copiedCode,
                                                transitionSpec = {
                                                    (scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh)) + fadeIn()) togetherWith
                                                    (scaleOut(tween(150)) + fadeOut(tween(150)))
                                                },
                                                label = "copyIconContent"
                                            ) { isCopied ->
                                                Icon(
                                                    if (isCopied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                                    if (isCopied) "Copiado" else "Copiar codigo",
                                                    tint = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = {
                                        val code = uiState.event?.code ?: return@IconButton
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                context.getString(R.string.share_event, code)
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, context.getString(R.string.chooser_share_code))
                                        )
                                    }) {
                                        Icon(
                                            Icons.Filled.Share,
                                            "Compartir",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(20.dp))

                        val editInteraction = remember { MutableInteractionSource() }
                        GenActionButton(
                            onClick = onEditAvailability,
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = editInteraction
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_edit_availability),
                                color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val shareInteraction = remember { MutableInteractionSource() }
                        GenActionButton(
                            onClick = {
                                val code = uiState.event?.code ?: return@GenActionButton
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.getString(R.string.share_event, code)
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, context.getString(R.string.chooser_share_code))
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = shareInteraction
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.detail_share_with_group),
                                color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(24.dp + bottomPadding.calculateBottomPadding()))
                    }
                }
            }
        }
        }
    }

    GenTopBar(
        title = uiState.event?.name ?: stringResource(R.string.session_fallback_name),
        hazeState = hazeState,
        onBack = onBack,
        onTrailingClick = { showMoreDialog = true },
        glass = overlayGlass
    )

    ScrimDialogOverlay(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false }
    ) {
        DeleteSessionDialog(
            hazeState = hazeState,
            glass = overlayGlass,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteEvent()
            }
        )
    }

    ScrimDialogOverlay(
        visible = showMoreDialog,
        onDismiss = { showMoreDialog = false }
    ) {
        MoreOptionsDialog(
            hazeState = hazeState,
            glass = overlayGlass,
            isCreator = uiState.isCreator,
            onFixDate = {
                showMoreDialog = false
                showConfirmDateDialog = true
            },
            onDelete = {
                showMoreDialog = false
                showDeleteDialog = true
            }
        )
    }

    ScrimDialogOverlay(
        visible = showConfirmDateDialog,
        onDismiss = { showConfirmDateDialog = false }
    ) {
        ConfirmDateDialog(
            dateSummaries = uiState.dateSummaries,
            currentConfirmedDate = uiState.confirmedDate,
            hazeState = hazeState,
            glass = overlayGlass,
            onDateSelected = { date ->
                showConfirmDateDialog = false
                // Elegido el día, preguntamos la hora: cuadrar solo el día
                // deja la coordinación a medias.
                pendingConfirmDate = date
            },
            onClearDate = {
                showConfirmDateDialog = false
                viewModel.clearConfirmedDate()
            }
        )
    }

    pendingConfirmDate?.let { date ->
        StartTimePickerDialog(
            initialTime = uiState.startTime ?: LocalTime.of(18, 0),
            onDismiss = {
                // Sin hora la sesión sigue siendo útil: fijamos solo el día.
                pendingConfirmDate = null
                viewModel.confirmDate(date, null)
                scrollToConfirmed = true
            },
            onConfirm = { time ->
                pendingConfirmDate = null
                viewModel.confirmDate(date, time)
                scrollToConfirmed = true
            }
        )
    }

    } // Box
}
