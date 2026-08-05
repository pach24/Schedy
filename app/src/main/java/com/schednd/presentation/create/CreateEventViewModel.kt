package com.schednd.presentation.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.domain.usecase.auth.EnsureSignedInUseCase
import com.schednd.domain.usecase.player.GetPlayerNameUseCase
import com.schednd.domain.usecase.player.SavePlayerNameUseCase
import com.schednd.domain.usecase.session.CreateSessionUseCase
import com.schednd.domain.usecase.session.ObserveParticipantsUseCase
import com.schednd.domain.usecase.session.SaveAvailabilityUseCase
import com.schednd.domain.usecase.session.SubscribeToSessionUseCase
import com.schednd.presentation.common.UiError
import com.schednd.presentation.common.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

data class CreateEventUiState(
    val eventName: String = "",
    val creatorName: String = "",
    val selectedDates: Set<LocalDate> = emptySet(),
    val dateAttendeeCount: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val createdCode: String? = null,
    val isDone: Boolean = false,
    val error: UiError? = null
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val ensureSignedIn: EnsureSignedInUseCase,
    private val createSession: CreateSessionUseCase,
    private val observeParticipantsUseCase: ObserveParticipantsUseCase,
    private val saveAvailability: SaveAvailabilityUseCase,
    private val subscribeToSession: SubscribeToSessionUseCase,
    private val getPlayerName: GetPlayerNameUseCase,
    private val savePlayerName: SavePlayerNameUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState

    init {
        // Prefill con el nombre guardado en el onboarding para no re-teclearlo
        getPlayerName()?.let { saved ->
            _uiState.update { it.copy(creatorName = saved) }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(eventName = name) }
    }

    fun onCreatorNameChanged(name: String) {
        _uiState.update { it.copy(creatorName = name) }
    }

    fun onDateToggled(date: LocalDate) {
        _uiState.update { state ->
            val dates = state.selectedDates.toMutableSet()
            if (date in dates) dates.remove(date) else dates.add(date)
            state.copy(selectedDates = dates)
        }
    }

    fun onCreate() {
        val state = _uiState.value
        if (state.eventName.isBlank() || state.creatorName.isBlank()) return

        // Recordar el nombre para futuras sesiones
        savePlayerName(state.creatorName.trim())

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = ensureSignedIn()
                val code = createSession(
                    name = state.eventName.trim(),
                    creatorId = userId
                )
                subscribeToSession(code)
                _uiState.update { it.copy(isLoading = false, createdCode = code) }
                observeParticipants(code)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUiError()) }
            }
        }
    }

    fun onSaveAvailability() {
        val state = _uiState.value
        val code = state.createdCode ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = ensureSignedIn()
                if (state.selectedDates.isNotEmpty()) {
                    saveAvailability(
                        code = code,
                        userId = userId,
                        name = state.creatorName.trim(),
                        dates = state.selectedDates.sorted()
                    )
                }
                _uiState.update { it.copy(isLoading = false, isDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUiError()) }
            }
        }
    }

    private fun observeParticipants(code: String) {
        viewModelScope.launch {
            observeParticipantsUseCase(code).collect { participants ->
                val today = LocalDate.now()
                val counts = mutableMapOf<LocalDate, Int>()
                participants.forEach { p ->
                    p.availableDates.forEach { ts ->
                        val date = ts.toLocalDate()
                        if (!date.isBefore(today)) {
                            counts[date] = (counts[date] ?: 0) + 1
                        }
                    }
                }
                _uiState.update { it.copy(dateAttendeeCount = counts) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun Timestamp.toLocalDate(): LocalDate =
        Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
}
