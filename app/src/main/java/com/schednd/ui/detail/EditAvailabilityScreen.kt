package com.schednd.ui.detail

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.schednd.ui.theme.SchedyTheme
import java.time.LocalDate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.schednd.ui.components.GenTextField
import com.schednd.ui.components.CalendarGrid
import com.schednd.ui.components.LoadingDots
import com.schednd.ui.theme.FadeIn
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.pressScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import com.schednd.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAvailabilityScreen(
    viewModel: EventDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    EditAvailabilityContent(
        uiState = uiState,
        onMyNameChanged = viewModel::onMyNameChanged,
        onMyDateToggled = viewModel::onMyDateToggled,
        onSave = viewModel::saveMyAvailability,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAvailabilityContent(
    uiState: EventDetailUiState,
    onMyNameChanged: (String) -> Unit,
    onMyDateToggled: (LocalDate) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_availability_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBackIos, stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
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
                Text(
                    text = stringResource(R.string.edit_availability_pick_days),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            FadeIn(delayMs = 80) {
                GenTextField(
                    value = uiState.myName,
                    onValueChange = onMyNameChanged,
                    label = "Tu nombre",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            FadeIn(delayMs = 150) {
                Text(
                    text = pluralStringResource(
                        R.plurals.days_selected,
                        uiState.myDraftDates.size,
                        uiState.myDraftDates.size
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            FadeIn(delayMs = 220) {
                val attendeeCounts = uiState.participantAvailability.values
                    .flatMap { it }
                    .groupingBy { it }
                    .eachCount()
                Box(modifier = Modifier.fillMaxWidth()) {
                    CalendarGrid(
                        selectedDates = uiState.myDraftDates,
                        onDateToggled = onMyDateToggled,
                        dateAttendeeCount = attendeeCounts,
                        mySavedDates = uiState.mySavedDates
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            FadeIn(delayMs = 320) {
                val saveInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = {
                        onSave()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(saveInteraction),
                    shape = FullRoundShape,
                    interactionSource = saveInteraction,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    enabled = uiState.myName.isNotBlank()
                            && uiState.myDraftDates.isNotEmpty()
                            && !uiState.isSavingAvailability
                ) {
                    if (uiState.isSavingAvailability) {
                        LoadingDots(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                    }
                    Text(stringResource(R.string.action_save))
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Editar disponibilidad (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun EditAvailabilityPreviewLight() {
    SchedyTheme(darkTheme = false) {
        val today = LocalDate.now()
        EditAvailabilityContent(
            uiState = EventDetailUiState(
                myName = "Pizpireto",
                myDraftDates = setOf(today.plusDays(2), today.plusDays(5), today.plusDays(9)),
                mySavedDates = setOf(today.plusDays(2), today.plusDays(5)),
                participantAvailability = mapOf(
                    "u1" to setOf(today.plusDays(2), today.plusDays(5)),
                    "u2" to setOf(today.plusDays(5), today.plusDays(9)),
                    "u3" to setOf(today.plusDays(2), today.plusDays(5), today.plusDays(9))
                ),
                isLoading = false
            ),
            onMyNameChanged = {},
            onMyDateToggled = {},
            onSave = {},
            onBack = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Editar disponibilidad (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EditAvailabilityPreviewDark() {
    SchedyTheme(darkTheme = true) {
        val today = LocalDate.now()
        EditAvailabilityContent(
            uiState = EventDetailUiState(
                myName = "Gandalf",
                myDraftDates = setOf(today.plusDays(1), today.plusDays(3), today.plusDays(6), today.plusDays(10)),
                mySavedDates = setOf(today.plusDays(1), today.plusDays(3)),
                participantAvailability = mapOf(
                    "u1" to setOf(today.plusDays(1), today.plusDays(3)),
                    "u2" to setOf(today.plusDays(3), today.plusDays(6)),
                    "u3" to setOf(today.plusDays(1), today.plusDays(6), today.plusDays(10))
                ),
                isLoading = false
            ),
            onMyNameChanged = {},
            onMyDateToggled = {},
            onSave = {},
            onBack = {}
        )
    }
}
