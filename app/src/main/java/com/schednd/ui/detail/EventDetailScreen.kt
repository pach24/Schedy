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
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.schednd.ui.components.AppleTextField
import com.schednd.ui.components.LoadingDots
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.R
import com.schednd.domain.model.AttendanceTier
import com.schednd.domain.model.DateSummary
import java.time.LocalDate
import java.time.LocalTime
import com.schednd.ui.components.AppleCard
import com.schednd.ui.components.AppleTopBar
import com.schednd.ui.components.AvailabilityGrid
import com.schednd.ui.components.LiquidGlassState
import com.schednd.ui.components.frostedSurface
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.components.liquidGlassBackdrop
import com.schednd.ui.components.rememberLiquidGlassState
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SquircleMiniShape
import com.schednd.ui.theme.pressScale
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.delay
import com.schednd.model.Event
import com.schednd.model.Participant
import com.schednd.ui.theme.SchedndTheme

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

    val anyDialogOpen = showDeleteDialog || showMoreDialog

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

        Box(modifier = Modifier.padding(innerPadding)) {
            // Animated content states — Trade Republic style scale+fade
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
                                AppleCard(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Aun no hay participantes. Comparte el codigo para que se unan.",
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
                                        text = "Disponibilidad:",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Texto "Baja"
                                    Text(
                                        text = "Baja",
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
                                        text = "Alta",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (recommended.isNotEmpty()) {
                                val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
                                Spacer(modifier = Modifier.height(16.dp))
                                FadeIn(delayMs = 300) {
                                    AppleCard(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Text(
                                                text = "Fechas recomendadas",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            recommended.forEach { s ->
                                                val label = if (s.absentNames.isEmpty())
                                                    "Asistencia completa · ${s.count}/${s.total}"
                                                else
                                                    "Asisten ${s.count}/${s.total} · Falta: ${s.absentNames.joinToString(", ")}"
                                                Text(
                                                    text = "· ${dateFormat.format(s.date)}  –  $label",
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
                            val confirmedFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
                            val startTimeFormat = DateTimeFormatter.ofPattern("HH:mm")
                            Column {
                                AppleCard(modifier = Modifier.fillMaxWidth()) {
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
                                                text = "Fecha elegida",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = uiState.confirmedDate?.let { date ->
                                                    val day = confirmedFormat.format(date)
                                                    uiState.startTime
                                                        ?.let { "$day · ${startTimeFormat.format(it)}" }
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
                                AppleActionButton(
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
                                                    uiState.event?.name ?: "Sesión de rol"
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
                                        "Añadir a calendario",
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        FadeIn(delayMs = 300) {
                            AppleCard(
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
                                            text = "Codigo de la sesión",
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
                                            Intent.createChooser(sendIntent, "Compartir codigo")
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
                        AppleActionButton(
                            onClick = onEditAvailability,
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = editInteraction
                        ) {
                            Icon(Icons.Filled.CalendarMonth, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar mi disponibilidad",
                                color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val shareInteraction = remember { MutableInteractionSource() }
                        AppleActionButton(
                            onClick = {
                                val code = uiState.event?.code ?: return@AppleActionButton
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        context.getString(R.string.share_event, code)
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(
                                    Intent.createChooser(sendIntent, "Compartir codigo")
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            interactionSource = shareInteraction
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Compartir con el grupo",
                                color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(24.dp + bottomPadding.calculateBottomPadding()))
                    }
                }
            }
        }
        }
    }

    AppleTopBar(
        title = uiState.event?.name ?: "Sesion",
        hazeState = hazeState,
        onBack = onBack,
        onTrailingClick = { showMoreDialog = true },
        glass = overlayGlass
    )

    AnimatedVisibility(
        visible = showDeleteDialog,
        enter = fadeIn(tween(220)),
        exit  = fadeOut(tween(200))
    ) {
        val animScope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showDeleteDialog = false },
            contentAlignment = Alignment.Center
        ) {
            with(animScope) {
                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.86f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(180)),
                        exit = scaleOut(
                            targetScale   = 0.92f,
                            animationSpec = tween(160)
                        ) + fadeOut(tween(160))
                    )
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
            }
        }
    }

    AnimatedVisibility(
        visible = showMoreDialog,
        enter = fadeIn(tween(220)),
        exit  = fadeOut(tween(200))
    ) {
        val animScope = this
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { showMoreDialog = false },
            contentAlignment = Alignment.Center
        ) {
            with(animScope) {
                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.86f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness    = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(tween(180)),
                        exit = scaleOut(targetScale = 0.92f, animationSpec = tween(160)) + fadeOut(tween(160))
                    )
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
            }
        }
    }

    if (showConfirmDateDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showConfirmDateDialog = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 0.dp,
            dragHandle = null
        ) {
            ConfirmDateSheetContent(
                dateSummaries = uiState.dateSummaries,
                currentConfirmedDate = uiState.confirmedDate,
                hazeState = hazeState,
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

/** Duración por defecto del evento de calendario cuando la sesión tiene hora. */
private const val SESSION_DEFAULT_DURATION_MS = 3L * 60 * 60 * 1000

/** Selector de hora de inicio. Descartarlo confirma el día sin hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿A qué hora empezáis?") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Fijar hora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Solo el día") }
        }
    )
}

@Composable
private fun AppleActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.72f) else Color(0xFFFFFFFF)
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    Box(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(FullRoundShape)
            .background(fillColor)
            .border(1.dp, borderBrush, FullRoundShape)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = { content() }
        )
    }
}

@Composable
private fun DeleteSessionDialog(
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onConfirm: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)

    val tintColor = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.82f)
    else
        Color.White.copy(alpha = 0.82f)
    // Más velo que en la top bar: aquí hay texto que leer sobre el cristal.
    val glassTint = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.62f)
    else
        Color.White.copy(alpha = 0.68f)

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White, Color.Transparent)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .frostedSurface(
                glass = glass,
                hazeState = hazeState,
                cornerRadius = 28.dp,
                hazeBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
                hazeTint = tintColor,
                glassTint = glassTint,
            )
            .border(1.dp, borderBrush, dialogShape)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Borrar sesion",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Seguro que quieres borrar esta sesion? Esta accion no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    contentColor = Color(0xFFFD3744)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text("Borrar sesion", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
private fun MoreOptionsDialog(
    hazeState: HazeState,
    glass: LiquidGlassState?,
    isCreator: Boolean,
    onFixDate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
    // Más velo que en la top bar: aquí hay texto que leer sobre el cristal.
    val glassTint = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.62f)
    else
        Color.White.copy(alpha = 0.68f)
    val borderBrush = if (isDark)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))
    else
        Brush.verticalGradient(listOf(Color.White, Color.Transparent))

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .frostedSurface(
                glass = glass,
                hazeState = hazeState,
                cornerRadius = 28.dp,
                hazeBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
                hazeTint = tintColor,
                glassTint = glassTint,
            )
            .border(1.dp, borderBrush, dialogShape)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Fijar fecha y borrar son acciones del DM. Las reglas de Firestore
            // rechazan ambas para el resto, así que la UI no las ofrece.
            if (!isCreator) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Solo el DM puede fijar la fecha",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isCreator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFixDate
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Fijar fecha y hora",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDelete
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFD3744),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Borrar sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFFD3744)
                )
            }
            } // if (isCreator)
        }
    }
}

@Composable
private fun ConfirmDateSheetContent(
    dateSummaries: List<DateSummary>,
    currentConfirmedDate: LocalDate?,
    hazeState: HazeState,
    onDateSelected: (LocalDate) -> Unit,
    onClearDate: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
    val sorted = remember(dateSummaries) { dateSummaries.sortedByDescending { it.count } }
    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    val topBorderColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
    val innerBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.18f else 0.9f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(sheetShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 24.dp
                backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                tints = listOf(HazeTint(tintColor))
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(topBorderColor, Color.Transparent)
                ),
                shape = sheetShape
            )
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        // iOS pill handle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            )
        }

        Text(
            text = "Elige la fecha",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date list
        val listScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                    backgroundColor = if (isDark) Color(0xFF27272A) else Color(0xFFF0F0F2)
                    tints = listOf(
                        HazeTint(
                            if (isDark) Color(0xFF27272A).copy(alpha = 0.65f)
                            else Color(0xFFF0F0F2).copy(alpha = 0.65f)
                        )
                    )
                }
                .border(1.dp, innerBorderBrush, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 380.dp)
                    .nestedScroll(listScrollConnection)
            ) {
                itemsIndexed(sorted) { index, summary ->
                    val isConfirmed = summary.date == currentConfirmedDate
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = LocalIndication.current
                            ) { onDateSelected(summary.date) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isConfirmed) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0082F3),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = dateFormat.format(summary.date)
                                    .replaceFirstChar { it.uppercaseChar() },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isConfirmed) Color(0xFF0082F3)
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isConfirmed) FontWeight.SemiBold
                                             else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "${summary.count}/${summary.total}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }

        if (currentConfirmedDate != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClearDate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    contentColor = Color(0xFFFD3744)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    "Quitar fecha elegida",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier
            .navigationBarsPadding()
            .height(24.dp)
        )
    }
    } // hazeEffect Box
}


@Composable
private fun SessionCountdown(
    confirmedDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val daysLeft = ChronoUnit.DAYS.between(today, confirmedDate)
    val isPast = daysLeft < 0
    val daysAgo = -daysLeft

    Column(modifier = modifier) {
        Text(
            text = if (isPast) "SESIÓN PASADA" else "PRÓXIMA SESIÓN",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (isPast) {
            val pastLabel = confirmedDate.format(
                DateTimeFormatter.ofPattern("EEE d 'de' MMMM", Locale("es"))
            ).replaceFirstChar { it.uppercaseChar() }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pastLabel,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "hace $daysAgo día${if (daysAgo == 1L) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        } else {
            val dateLabel = confirmedDate.format(
                DateTimeFormatter.ofPattern("EEE d", Locale("es"))
            ).replaceFirstChar { it.uppercaseChar() }
            Row(verticalAlignment = Alignment.Bottom) {
                if (daysLeft == 0L) {
                    Text(
                        text = "HOY",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 72.sp,
                            letterSpacing = (-2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = String.format(Locale("es"), "%02d", daysLeft),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 88.sp,
                            letterSpacing = (-4).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(
                        text = if (daysLeft == 0L) "" else "días",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview(
    name = "TopBar (Dark)",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF0D0D0D
)
@Composable
private fun PreviewAppleTopBarDark() {
    SchedndTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            AppleTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(
    name = "TopBar (Light)",
    showBackground = true,
    backgroundColor = 0xFFF4F4F6
)
@Composable
private fun PreviewAppleTopBarLight() {
    SchedndTheme(darkTheme = false) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            AppleTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(name = "Countdown – próxima sesión (Light)", showBackground = true)
@Composable
private fun SessionCountdownPreviewLight() {
    SchedndTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            SessionCountdown(
                confirmedDate = LocalDate.now().plusDays(12),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Countdown – hoy (Dark)", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionCountdownTodayPreviewDark() {
    SchedndTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            SessionCountdown(
                confirmedDate = LocalDate.now(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Detalle – Con participantes (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun EventDetailBodyPreviewLight() {
    SchedndTheme(darkTheme = false) {
        val today = LocalDate.now()
        val dates = listOf(today.plusDays(3), today.plusDays(7), today.plusDays(14), today.plusDays(21))
        val participants = listOf(
            Participant(userId = "u1", name = "Pizpireto", availableDates = emptyList()),
            Participant(userId = "u2", name = "Gandalf", availableDates = emptyList()),
            Participant(userId = "u3", name = "Legolas", availableDates = emptyList()),
        )
        val participantAvailability = mapOf(
            "u1" to setOf(dates[0], dates[1]),
            "u2" to setOf(dates[1], dates[2], dates[3]),
            "u3" to setOf(dates[0], dates[1], dates[2]),
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SessionCountdown(
                    confirmedDate = today.plusDays(7),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)
                )
                com.schednd.ui.components.AvailabilityGrid(
                    dates = dates,
                    participants = participants,
                    participantAvailability = participantAvailability,
                )
                Spacer(modifier = Modifier.height(24.dp))
                AppleCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Codigo de la sesión",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ABC123",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Filled.ContentCopy,
                            "Copiar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            AppleTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(name = "Detalle – Sin participantes (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventDetailEmptyPreviewDark() {
    SchedndTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AppleCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aun no hay participantes. Comparte el codigo para que se unan.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                AppleCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Codigo de la sesión",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ABC123",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Filled.ContentCopy,
                            "Copiar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            AppleTopBar(
                title = "One-Shot Halloween",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

// ── MOCK SESIÓN 23 ────────────────────────────────────────────────────────────

@Preview(
    name = "Mock – Sesión 23 (Dark)",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF0D0D0F
)
@Composable
private fun MockSession23Dark() {
    SchedndTheme(darkTheme = true) {
        Session23MockContent(darkTheme = true)
    }
}

@Preview(
    name = "Mock – Sesión 23 (Light)",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    backgroundColor = 0xFFF4F4F6
)
@Composable
private fun MockSession23Light() {
    SchedndTheme(darkTheme = false) {
        Session23MockContent(darkTheme = false)
    }
}

@Composable
private fun Session23MockContent(darkTheme: Boolean) {
    val today = LocalDate.now()
    val confirmedDate = today.plusDays(5)

    val d1 = today.plusDays(5)
    val d2 = today.plusDays(12)
    val d3 = today.plusDays(19)
    val d4 = today.plusDays(26)
    val dates = listOf(d1, d2, d3, d4)

    val participants = listOf(
        Participant(userId = "u1", name = "Kira",    notes = listOf("Llego tarde el sábado", "Prefiero empezar a las 18h")),
        Participant(userId = "u2", name = "Aldric",  notes = listOf("Traigo snacks")),
        Participant(userId = "u3", name = "Veyra",   notes = emptyList()),
        Participant(userId = "u4", name = "Tormund", notes = listOf("Puedo DM si hace falta")),
        Participant(userId = "u5", name = "Sylwen",  notes = emptyList()),
    )

    val participantAvailability = mapOf(
        "u1" to setOf(d1, d2, d4),
        "u2" to setOf(d1, d2, d3),
        "u3" to setOf(d1, d3, d4),
        "u4" to setOf(d1, d2, d3, d4),
        "u5" to setOf(d2, d3),
    )

    val dateSummaries = listOf(
        DateSummary(date = d1, count = 4, total = 5, absentNames = listOf("Sylwen"), tier = AttendanceTier.VIABLE),
        DateSummary(date = d2, count = 4, total = 5, absentNames = listOf("Veyra"),  tier = AttendanceTier.VIABLE),
        DateSummary(date = d3, count = 4, total = 5, absentNames = listOf("Kira"),   tier = AttendanceTier.VIABLE),
        DateSummary(date = d4, count = 3, total = 5, absentNames = listOf("Aldric", "Sylwen"), tier = AttendanceTier.LIMITED),
    )

    val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))
    val confirmedFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale("es"))

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(72.dp))

            // ── Countdown próxima sesión ──────────────────────────────────────
            SessionCountdown(
                confirmedDate = confirmedDate,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)
            )

            // ── Grid de disponibilidad ────────────────────────────────────────
            AvailabilityGrid(
                dates = dates,
                participants = participants,
                participantAvailability = participantAvailability,
            )

            // ── Leyenda ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Disponibilidad:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Baja",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(7) { level ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(SquircleMiniShape)
                                .background(getHeatmapColor(level, 6))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Alta",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Fechas recomendadas ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            AppleCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fechas recomendadas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    dateSummaries.filter {
                        it.tier == AttendanceTier.FULL || it.tier == AttendanceTier.VIABLE
                    }.forEach { s ->
                        val label = if (s.absentNames.isEmpty())
                            "Asistencia completa · ${s.count}/${s.total}"
                        else
                            "Asisten ${s.count}/${s.total} · Falta: ${s.absentNames.joinToString(", ")}"
                        Text(
                            text = "· ${dateFormat.format(s.date)}  –  $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Fecha elegida ─────────────────────────────────────────────────
            AppleCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
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
                            text = "Fecha elegida",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = confirmedFormat.format(confirmedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Código de la sesión ───────────────────────────────────────────
            AppleCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Codigo de la sesión",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "SES023",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Filled.ContentCopy, "Copiar", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.Share, "Compartir", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Notas del grupo ───────────────────────────────────────────────
            AppleCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notas del grupo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    participants.filter { it.notes.isNotEmpty() }.forEachIndexed { pIndex, participant ->
                        if (pIndex > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        participant.notes.forEach { noteText ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                // Kira es el usuario actual — muestra botones de edición
                                if (participant.userId == "u1") {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFFD3744),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = FullRoundShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text("Añadir nota")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Botones de acción ─────────────────────────────────────────────
            AppleActionButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar mi disponibilidad", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
            AppleActionButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir con el grupo", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        AppleTopBar(
            title = "Sesión 23",
            hazeState = remember { HazeState() },
            onBack = {},
            onTrailingClick = {}
        )
    }
}