package com.schednd.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.data.repository.AuthRepository
import com.schednd.data.repository.EventRepository
import com.schednd.data.repository.NotificationRepository
import com.schednd.data.repository.PlayerRepository
import com.schednd.data.repository.RecentEventsRepository
import com.schednd.data.work.SessionReminderScheduler
import com.schednd.domain.model.AttendanceTier
import com.schednd.domain.model.DateSummary
import com.schednd.domain.usecase.ComputeDateSummariesUseCase
import com.schednd.model.Event
import com.schednd.model.Participant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject


data class EventDetailUiState(
    val event: Event? = null,
    val participants: List<Participant> = emptyList(),
    val datesAsLocal: List<LocalDate> = emptyList(),
    val participantAvailability: Map<String, Set<LocalDate>> = emptyMap(),
    val dateSummaries: List<DateSummary> = emptyList(),
    val confirmedDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val isCreator: Boolean = false,
    val isDeleted: Boolean = false,
    val isLoading: Boolean = true,
    val myName: String = "",
    val myDraftDates: Set<LocalDate> = emptySet(),
    val mySavedDates: Set<LocalDate> = emptySet(),
    val isSavingAvailability: Boolean = false,
    val myUserId: String? = null,
    val error: String? = null
)

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val recentEventsRepository: RecentEventsRepository,
    private val playerRepository: PlayerRepository,
    private val notificationRepository: NotificationRepository,
    private val reminderScheduler: SessionReminderScheduler,
    private val computeDateSummaries: ComputeDateSummariesUseCase
) : ViewModel() {

    private val code: String = savedStateHandle.get<String>("code")!!
    private val myUserId: String? get() = authRepository.getCurrentUserId()

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    init {
        // Prefill con el nombre guardado; si ya soy participante, el collect lo sobrescribe con mi nombre real
        playerRepository.getPlayerName()?.let { saved ->
            _uiState.update { it.copy(myName = saved) }
        }
        viewModelScope.launch {
            try {
                authRepository.ensureSignedIn()
                eventRepository.observeEvent(code)
                    .combine(eventRepository.observeParticipants(code)) { event, participants ->
                        Pair(event, participants)
                    }
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { (event, participants) ->
                        if (event != null) {
                            recentEventsRepository.saveEvent(code)
                            val today = LocalDate.now()
                            val availability = participants.associate { p ->
                                p.userId to p.availableDates
                                    .map { it.toLocalDate() }
                                    .filter { !it.isBefore(today) }
                                    .toSet()
                            }
                            val datesLocal = availability.values
                                .flatMap { it }
                                .distinct()
                                .sorted()
                            val dateSummaries = computeDateSummaries(datesLocal, participants, availability)

                            val confirmedDate = event.confirmedDate?.toLocalDate()
                            val isCreator = event.creatorId == authRepository.getCurrentUserId()

                            // Mantener el recordatorio local alineado con lo que diga Firestore,
                            // aunque la fecha la haya cambiado otro dispositivo.
                            if (confirmedDate != null) {
                                reminderScheduler.schedule(
                                    code = code,
                                    sessionName = event.name,
                                    date = confirmedDate,
                                    startTime = event.startLocalTime
                                )
                            } else {
                                reminderScheduler.cancel(code)
                            }

                            val myParticipant = participants.find { it.userId == myUserId }
                            val mySavedDates = myParticipant?.availableDates
                                ?.map { it.toLocalDate() }
                                ?.filter { !it.isBefore(today) }
                                ?.toSet() ?: emptySet()

                            _uiState.update { current ->
                                current.copy(
                                    event = event,
                                    participants = participants,
                                    datesAsLocal = datesLocal,
                                    participantAvailability = availability,
                                    dateSummaries = dateSummaries,
                                    confirmedDate = confirmedDate,
                                    startTime = event.startLocalTime,
                                    isCreator = isCreator,
                                    isLoading = false,
                                    mySavedDates = mySavedDates,
                                    myUserId = authRepository.getCurrentUserId(),
                                    // Only sync draft name/dates from Firestore if not currently editing
                                    myName = if (!current.isSavingAvailability) myParticipant?.name ?: current.myName else current.myName,
                                    myDraftDates = if (!current.isSavingAvailability) mySavedDates else current.myDraftDates
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, error = "Sesión no encontrada")
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onMyNameChanged(name: String) {
        _uiState.update { it.copy(myName = name) }
    }

    fun onMyDateToggled(date: LocalDate) {
        _uiState.update { state ->
            val dates = state.myDraftDates.toMutableSet()
            if (date in dates) dates.remove(date) else dates.add(date)
            state.copy(myDraftDates = dates)
        }
    }

    fun saveMyAvailability() {
        val state = _uiState.value
        if (state.myName.isBlank() || state.myDraftDates.isEmpty()) return

        // Recordar el nombre para futuras sesiones
        playerRepository.savePlayerName(state.myName.trim())

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvailability = true, error = null) }
            try {
                val userId = authRepository.ensureSignedIn()
                val today = LocalDate.now()
                eventRepository.addOrUpdateAvailability(
                    code = code,
                    userId = userId,
                    name = state.myName.trim(),
                    dates = state.myDraftDates.filter { !it.isBefore(today) }.sorted()
                )
                runCatching {
                    notificationRepository.notifyAvailabilityUpdated(
                        code = code,
                        senderId = userId,
                        senderName = state.myName.trim()
                    )
                }
                _uiState.update { it.copy(isSavingAvailability = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingAvailability = false, error = e.message) }
            }
        }
    }

    /** Fijar día y hora es potestad del DM; la UI y las reglas de Firestore lo restringen igual. */
    fun confirmDate(date: LocalDate, startTime: LocalTime? = null) {
        if (!_uiState.value.isCreator) return
        viewModelScope.launch {
            try {
                eventRepository.confirmDate(code, date, startTime)
                val userId = authRepository.getCurrentUserId().orEmpty()
                val whenText = buildString {
                    append(date.format(DATE_FORMAT))
                    startTime?.let { append(" a las ").append(it.format(TIME_FORMAT)) }
                }
                runCatching {
                    notificationRepository.notifyDateConfirmed(code, userId, whenText)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setStartTime(startTime: LocalTime?) {
        if (!_uiState.value.isCreator) return
        viewModelScope.launch {
            try { eventRepository.setStartTime(code, startTime) }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearConfirmedDate() {
        if (!_uiState.value.isCreator) return
        viewModelScope.launch {
            try {
                eventRepository.clearConfirmedDate(code)
                reminderScheduler.cancel(code)
                val userId = authRepository.getCurrentUserId().orEmpty()
                runCatching { notificationRepository.notifyDateCleared(code, userId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            try {
                eventRepository.deleteEvent(code)
                recentEventsRepository.removeEvent(code)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun Timestamp.toLocalDate(): LocalDate {
        return Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
    }

    private companion object {
        val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM")
    }
}
