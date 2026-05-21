package com.schednd.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.schednd.R
import com.schednd.model.Event
import com.schednd.ui.components.AppleCard
import com.schednd.ui.theme.CardShape
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.PhaseEnterTransition
import com.schednd.ui.theme.PhaseExitTransition
import com.schednd.ui.theme.SchedndTheme
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.launch

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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            FadeIn(delayMs = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_app),
                        contentDescription = null,
                        modifier = Modifier.size(96.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "S&D",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Organiza tus sesiones de D&D",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            AnimatedContent(
                targetState = uiState.isAuthReady || uiState.error != null,
                transitionSpec = { PhaseEnterTransition togetherWith PhaseExitTransition },
                label = "AuthContent"
            ) { isReady ->
                if (!isReady) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                } else if (uiState.error != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        FadeIn(delayMs = 150) {
                            val createInteraction = remember { MutableInteractionSource() }
                            Button(
                                onClick = onCreateEvent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(createInteraction),
                                shape = FullRoundShape,
                                interactionSource = createInteraction,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                Text("Crear sesión", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FadeIn(delayMs = 250) {
                            val joinInteraction = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = onJoinEvent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pressScale(joinInteraction),
                                shape = FullRoundShape,
                                interactionSource = joinInteraction
                            ) {
                                Text("Unirse a sesión", modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }

                        if (uiState.recentEvents.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(40.dp))

                            FadeIn(delayMs = 350) {
                                Text(
                                    text = "Mis sesiones",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val isPreview = LocalInspectionMode.current
                            uiState.recentEvents.forEachIndexed { index, event ->
                                val cardAlpha = remember { Animatable(if (isPreview) 1f else 0f) }
                                val cardOffsetY = remember { Animatable(if (isPreview) 0f else 40f) }
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay((350 + index * 70).toLong())
                                    launch { cardAlpha.animateTo(1f, tween(400)) }
                                    cardOffsetY.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = 0.72f,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }

                                val cardInteraction = remember { MutableInteractionSource() }
                                AppleCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .graphicsLayer {
                                            alpha = cardAlpha.value
                                            translationY = cardOffsetY.value
                                        }
                                        .pressScale(cardInteraction)
                                        .clip(CardShape)
                                        .clickable(
                                            interactionSource = cardInteraction,
                                            indication = LocalIndication.current
                                        ) { onOpenEvent(event.code) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = event.code,
                                                style = MaterialTheme.typography.labelMedium.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 2.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Home – Sin sesiones (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
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

@Preview(name = "Home – Con sesiones (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreviewWithEvents() {
    SchedndTheme(darkTheme = true) {
        HomeContent(
            uiState = HomeUiState(
                isAuthReady = true,
                recentEvents = listOf(
                    Event(code = "ABC123", name = "Partida de D&D: El Resurgir"),
                    Event(code = "XYZ789", name = "Sesión semanal"),
                    Event(code = "HAL666", name = "One-Shot Halloween")
                )
            ),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}
