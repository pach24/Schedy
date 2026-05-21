package com.schednd.ui.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.data.repository.AuthRepository
import com.schednd.data.repository.EventRepository
import com.schednd.data.repository.MessagingRepository
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
    val error: String? = null
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val messagingRepository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState

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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = authRepository.ensureSignedIn()
                val code = eventRepository.createEvent(
                    name = state.eventName.trim(),
                    creatorId = userId
                )
                messagingRepository.subscribeToEvent(code)
                _uiState.update { it.copy(isLoading = false, createdCode = code) }
                observeParticipants(code)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onSaveAvailability() {
        val state = _uiState.value
        val code = state.createdCode ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val userId = authRepository.ensureSignedIn()
                if (state.selectedDates.isNotEmpty()) {
                    eventRepository.addOrUpdateAvailability(
                        code = code,
                        userId = userId,
                        name = state.creatorName.trim(),
                        dates = state.selectedDates.sorted()
                    )
                }
                _uiState.update { it.copy(isLoading = false, isDone = true) }
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
