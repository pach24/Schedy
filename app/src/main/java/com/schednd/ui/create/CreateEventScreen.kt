package com.schednd.ui.create

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.ui.components.AppleTextField
import com.schednd.ui.components.CalendarGrid
import com.schednd.ui.components.LoadingDots
import com.schednd.ui.theme.CardShape
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SchedndTheme
import com.schednd.ui.theme.pressScale
import java.time.LocalDate
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onEventCreated: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isDone) {
        if (uiState.isDone) {
            onEventCreated(uiState.createdCode!!)
        }
    }

    CreateEventContent(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onCreatorNameChanged = viewModel::onCreatorNameChanged,
        onDateToggled = viewModel::onDateToggled,
        onCreate = viewModel::onCreate,
        onSaveAvailability = viewModel::onSaveAvailability,
        onSkip = { onEventCreated(uiState.createdCode!!) },
        onBack = onBack,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventContent(
    uiState: CreateEventUiState,
    onNameChanged: (String) -> Unit,
    onCreatorNameChanged: (String) -> Unit,
    onDateToggled: (LocalDate) -> Unit,
    onCreate: () -> Unit,
    onSaveAvailability: () -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
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

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.createdCode == null) "Crear sesion" else "Tus fechas disponibles")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        AnimatedContent(
            targetState = uiState.createdCode != null,
            transitionSpec = { PhaseEnterTransition togetherWith PhaseExitTransition },
            label = "PhaseTransition"
        ) { isPhase2 ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                if (!isPhase2) {
                    // Phase 1: event details
                    FadeIn(delayMs = 0) {
                        AppleTextField(
                            value = uiState.eventName,
                            onValueChange = onNameChanged,
                            label = "Nombre de la sesion",
                            placeholder = "Ej: Sesion D&D semanal",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FadeIn(delayMs = 100) {
                        AppleTextField(
                            value = uiState.creatorName,
                            onValueChange = onCreatorNameChanged,
                            label = "Tu nombre",
                            placeholder = "Ej: Pizpireto",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FadeIn(delayMs = 200) {
                        val createInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = onCreate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale(createInteraction),
                            shape = FullRoundShape,
                            interactionSource = createInteraction,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            enabled = uiState.eventName.isNotBlank()
                                    && uiState.creatorName.isNotBlank()
                                    && !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                LoadingDots(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                            Text("Crear sesion", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                } else {
                    // Phase 2: share code + select own dates
                    val code = uiState.createdCode!!

                    FadeIn(delayMs = 0) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = CardShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Codigo de la sesion",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = code,
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 4.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(code))
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
                                                if (isCopied) "Copiado" else "Copiar",
                                                tint = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Unete a mi sesion de D&D en Schednd con el codigo: $code"
                                        )
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Compartir codigo"))
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

                    Spacer(modifier = Modifier.height(24.dp))

                    FadeIn(delayMs = 150) {
                        Column {
                            Text(
                                text = "Selecciona los dias que puedes",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${uiState.selectedDates.size} dia(s) seleccionado(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FadeIn(delayMs = 250) {
                        CalendarGrid(
                            selectedDates = uiState.selectedDates,
                            onDateToggled = onDateToggled,
                            dateAttendeeCount = uiState.dateAttendeeCount
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    FadeIn(delayMs = 350) {
                        Column {
                            val saveInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = onSaveAvailability,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(saveInteraction),
                                shape = FullRoundShape,
                                interactionSource = saveInteraction,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                ),
                                enabled = uiState.selectedDates.isNotEmpty() && !uiState.isLoading
                            ) {
                                if (uiState.isLoading) {
                                    LoadingDots(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(end = 10.dp)
                                    )
                                }
                                Text("Guardar mi disponibilidad", modifier = Modifier.padding(vertical = 4.dp))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val skipInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = onSkip,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(skipInteraction),
                                shape = FullRoundShape,
                                interactionSource = skipInteraction
                            ) {
                                Text("Saltar por ahora", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Crear – Fase 1 formulario (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun CreatePhase1Preview() {
    SchedndTheme(darkTheme = false) {
        CreateEventContent(
            uiState = CreateEventUiState(
                eventName = "Partida de D&D: El Resurgir",
                creatorName = "Pizpireto"
            ),
            onNameChanged = {},
            onCreatorNameChanged = {},
            onDateToggled = {},
            onCreate = {},
            onSaveAvailability = {},
            onSkip = {},
            onBack = {},
            onClearError = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Crear – Fase 2 código + calendario (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CreatePhase2Preview() {
    SchedndTheme(darkTheme = true) {
        val today = LocalDate.now()
        CreateEventContent(
            uiState = CreateEventUiState(
                eventName = "Partida de D&D: El Resurgir",
                creatorName = "Pizpireto",
                createdCode = "ABC123",
                selectedDates = setOf(today.plusDays(2), today.plusDays(5), today.plusDays(9)),
                dateAttendeeCount = mapOf(
                    today.plusDays(2) to 1,
                    today.plusDays(5) to 1
                )
            ),
            onNameChanged = {},
            onCreatorNameChanged = {},
            onDateToggled = {},
            onCreate = {},
            onSaveAvailability = {},
            onSkip = {},
            onBack = {},
            onClearError = {}
        )
    }
}
