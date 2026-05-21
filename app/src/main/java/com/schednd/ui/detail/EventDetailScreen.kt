package com.schednd.ui.detail

import android.content.Intent
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.schednd.domain.model.AttendanceTier
import com.schednd.domain.model.DateSummary
import java.time.LocalDate
import com.schednd.ui.components.AppleCard
import com.schednd.ui.components.AvailabilityGrid
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SquircleMiniShape
import com.schednd.ui.theme.pressScale
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
    onEditAvailability: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMoreDialog by remember { mutableStateOf(false) }
    var showConfirmDateDialog by remember { mutableStateOf(false) }
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

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

    Scaffold(
            modifier = Modifier.hazeSource(state = hazeState),
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
                            // 1. ELIMINAR statusBarsPadding() de aquí
                            // 2. ELIMINAR padding(top = 64.dp) de aquí
                            .navigationBarsPadding()
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
                                                text = uiState.confirmedDate?.let {
                                                    confirmedFormat.format(it)
                                                } ?: "",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
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
                                                "¡Únete a mi sesión de D&D en Schednd!\n\nEntra directamente aquí y solo pon tu nombre:\nschednd://join?code=$code\n\nO usa el código: $code"
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
                                        "¡Únete a mi sesión de D&D en Schednd!\n\nEntra directamente aquí y solo pon tu nombre:\nschednd://join?code=$code\n\nO usa el código: $code"
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

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
        }
    }

    AppleTopBar(
        title     = uiState.event?.name ?: "Sesion",
        isCreator = uiState.isCreator,
        hazeState = hazeState,
        onBack    = onBack,
        onMore    = { showMoreDialog = true }
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
                .background(Color.Black.copy(alpha = 0.25f))
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
                .background(Color.Black.copy(alpha = 0.25f))
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
                    viewModel.confirmDate(date)
                    scrollToConfirmed = true
                },
                onClearDate = {
                    showConfirmDateDialog = false
                    viewModel.clearConfirmedDate()
                }
            )
        }
    }

    } // Box
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
private fun AppleTopBar(
    title: String,
    isCreator: Boolean,
    hazeState: HazeState,
    onBack: () -> Unit,
    onMore: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) Color(0xFF1C1C1E) else Color.White
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.75f)
    val btnBorder = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
    val iconTint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ← Back button (circle)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                    backgroundColor = bgColor
                    tints = listOf(HazeTint(tintColor))
                }
                .border(1.dp, btnBorder, CircleShape)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = "Volver",
                tint = iconTint,
                modifier = Modifier
                    .size(16.dp)
                    .padding(start = 3.dp)
            )
        }

        // Title pill (center, flexible width)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                    backgroundColor = bgColor
                    tints = listOf(HazeTint(tintColor))
                }
                .border(1.dp, btnBorder, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = iconTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // ··· More button (circle) — visible para todos
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .hazeEffect(state = hazeState) {
                    blurRadius = 20.dp
                    backgroundColor = bgColor
                    tints = listOf(HazeTint(tintColor))
                }
                .border(1.dp, btnBorder, CircleShape)
                .clickable(
                    indication = LocalIndication.current,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onMore
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "Mas opciones",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DeleteSessionDialog(
    hazeState: HazeState,
    onConfirm: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)

    val tintColor = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.82f)
    else
        Color.White.copy(alpha = 0.82f)

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
            .hazeEffect(state = hazeState) {
                blurRadius = 20.dp
                backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                tints = listOf(HazeTint(tintColor))
            }
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
    isCreator: Boolean,
    onFixDate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
    val borderBrush = if (isDark)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))
    else
        Brush.verticalGradient(listOf(Color.White, Color.Transparent))

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 20.dp
                backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                tints = listOf(HazeTint(tintColor))
            }
            .border(1.dp, borderBrush, dialogShape)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
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
                    text = "Fijar fecha",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (isCreator) {
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
    val dateLabel = confirmedDate.format(
        DateTimeFormatter.ofPattern("EEE d", Locale("es"))
    ).replaceFirstChar { it.uppercaseChar() }

    Column(modifier = modifier) {
        Text(
            text = "PRÓXIMA SESIÓN",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            if (daysLeft <= 0) {
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
                    text = String.format("%02d", daysLeft),
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
                    text = if (daysLeft <= 0) "" else "días",
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
                isCreator = true,
                hazeState = remember { HazeState() },
                onBack = {},
                onMore = {}
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
                isCreator = false,
                hazeState = remember { HazeState() },
                onBack = {},
                onMore = {}
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
                isCreator = true,
                hazeState = remember { HazeState() },
                onBack = {},
                onMore = {}
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
                isCreator = true,
                hazeState = remember { HazeState() },
                onBack = {},
                onMore = {}
            )
        }
    }
}