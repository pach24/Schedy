package com.schednd.ui.join

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.model.Event
import com.schednd.ui.components.AppleTextField
import com.schednd.ui.components.CalendarGrid
import com.schednd.ui.components.LoadingDots
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SchedndTheme
import com.schednd.ui.theme.pressScale
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventScreen(
    onJoined: (String) -> Unit,
    onBack: () -> Unit,
    prefilledCode: String = "",
    viewModel: JoinEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(prefilledCode) {
        if (prefilledCode.isNotEmpty()) viewModel.onCodeChanged(prefilledCode)
    }

    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            onJoined(uiState.code)
        }
    }

    JoinEventContent(
        uiState = uiState,
        onCodeChanged = viewModel::onCodeChanged,
        onNameChanged = viewModel::onNameChanged,
        onLookUp = viewModel::onLookUp,
        onDateToggled = viewModel::onDateToggled,
        onSubmit = viewModel::onSubmit,
        onBack = onBack,
        onClearError = viewModel::clearError
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinEventContent(
    uiState: JoinEventUiState,
    onCodeChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onLookUp: () -> Unit,
    onDateToggled: (LocalDate) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    onClearError: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                title = { Text("Unirse a sesion") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            FadeIn(delayMs = 0) {
                AppleTextField(
                    value = uiState.code,
                    onValueChange = onCodeChanged,
                    label = "Codigo de la sesión",
                    placeholder = "ABC123",
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FadeIn(delayMs = 100) {
                AppleTextField(
                    value = uiState.participantName,
                    onValueChange = onNameChanged,
                    label = "Tu nombre",
                    placeholder = "Ej: Pizpireto",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = uiState.event != null,
                transitionSpec = { PhaseEnterTransition togetherWith PhaseExitTransition },
                label = "JoinPhaseTransition"
            ) { hasEvent ->
                if (!hasEvent) {
                    FadeIn(delayMs = 200) {
                        val lookupInteraction = remember { MutableInteractionSource() }
                        Button(
                            onClick = onLookUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale(lookupInteraction),
                            shape = FullRoundShape,
                            interactionSource = lookupInteraction,
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            enabled = uiState.code.length == 6
                                    && uiState.participantName.isNotBlank()
                                    && !uiState.isLoading
                        ) {
                            if (uiState.isLoading) {
                                LoadingDots(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(end = 10.dp)
                                )
                            }
                            Text("Buscar sesion", modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                } else {
                    Column {
                        FadeIn(delayMs = 0) {
                            Column {
                                Text(
                                    text = uiState.event!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Selecciona los dias que puedes",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.selectedDates.size} dia(s) seleccionado(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        FadeIn(delayMs = 150) {
                            CalendarGrid(
                                selectedDates = uiState.selectedDates,
                                onDateToggled = onDateToggled,
                                dateAttendeeCount = uiState.dateAttendeeCount
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        FadeIn(delayMs = 300) {
                            val confirmInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = onSubmit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(confirmInteraction),
                                shape = FullRoundShape,
                                interactionSource = confirmInteraction,
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
                                Text("Confirmar disponibilidad", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Unirse – Buscar sesión (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun JoinPhase1Preview() {
    SchedndTheme(darkTheme = false) {
        JoinEventContent(
            uiState = JoinEventUiState(
                code = "ABC123",
                participantName = "Pizpireto"
            ),
            onCodeChanged = {},
            onNameChanged = {},
            onLookUp = {},
            onDateToggled = {},
            onSubmit = {},
            onBack = {},
            onClearError = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Unirse – Seleccionar fechas (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun JoinPhase2Preview() {
    SchedndTheme(darkTheme = true) {
        val today = LocalDate.now()
        JoinEventContent(
            uiState = JoinEventUiState(
                code = "XYZ789",
                participantName = "Gandalf",
                event = Event(code = "XYZ789", name = "Sesión semanal del viernes"),
                selectedDates = setOf(today.plusDays(1), today.plusDays(4), today.plusDays(7)),
                dateAttendeeCount = mapOf(
                    today.plusDays(1) to 3,
                    today.plusDays(4) to 5,
                    today.plusDays(7) to 2
                )
            ),
            onCodeChanged = {},
            onNameChanged = {},
            onLookUp = {},
            onDateToggled = {},
            onSubmit = {},
            onBack = {},
            onClearError = {}
        )
    }
}
