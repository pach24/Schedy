package com.schednd.ui.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schednd.data.repository.AuthRepository
import com.schednd.data.repository.EventRepository
import com.schednd.data.repository.MessagingRepository
import com.schednd.data.repository.PlayerRepository
import com.schednd.model.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import com.google.firebase.Timestamp
import javax.inject.Inject

data class JoinEventUiState(
    val code: String = "",
    val participantName: String = "",
    val event: Event? = null,
    val selectedDates: Set<LocalDate> = emptySet(),
    val dateAttendeeCount: Map<LocalDate, Int> = emptyMap(),
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class JoinEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val messagingRepository: MessagingRepository,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinEventUiState())
    val uiState: StateFlow<JoinEventUiState> = _uiState

    init {
        // Prefill con el nombre guardado en el onboarding para no re-teclearlo
        playerRepository.getPlayerName()?.let { saved ->
            _uiState.update { it.copy(participantName = saved) }
        }
    }

    fun onCodeChanged(code: String) {
        val sanitized = code.uppercase().take(6)
        _uiState.update { it.copy(code = sanitized) }
    }

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(participantName = name) }
    }

    fun onLookUp() {
        val code = _uiState.value.code
        if (code.length != 6) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val event = eventRepository.getEvent(code)
                if (event == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Sesión no encontrada") }
                } else {
                    _uiState.update { it.copy(isLoading = false, event = event) }
                    observeParticipants(code)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onDateToggled(date: LocalDate) {
        _uiState.update { state ->
            val dates = state.selectedDates.toMutableSet()
            if (date in dates) dates.remove(date) else dates.add(date)
            state.copy(selectedDates = dates)
        }
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.participantName.isBlank() || state.selectedDates.isEmpty()) return

        // Recordar el nombre para futuras sesiones
        playerRepository.savePlayerName(state.participantName.trim())

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = authRepository.ensureSignedIn()
                eventRepository.addOrUpdateAvailability(
                    code = state.code,
                    userId = userId,
                    name = state.participantName.trim(),
                    dates = state.selectedDates.sorted()
                )
                messagingRepository.subscribeToEvent(state.code)
                _uiState.update { it.copy(isLoading = false, isSubmitted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun observeParticipants(code: String) {
        viewModelScope.launch {
            eventRepository.observeParticipants(code).collect { participants ->
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
