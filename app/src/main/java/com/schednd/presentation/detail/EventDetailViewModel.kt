package com.schednd.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.domain.usecase.auth.EnsureSignedInUseCase
import com.schednd.domain.usecase.auth.GetCurrentUserIdUseCase
import com.schednd.domain.usecase.player.GetPlayerNameUseCase
import com.schednd.domain.usecase.player.SavePlayerNameUseCase
import com.schednd.domain.usecase.session.CancelSessionReminderUseCase
import com.schednd.domain.usecase.session.ClearSessionDateUseCase
import com.schednd.domain.usecase.session.ConfirmSessionDateUseCase
import com.schednd.domain.usecase.session.DeleteSessionUseCase
import com.schednd.domain.usecase.session.ForgetSessionUseCase
import com.schednd.domain.usecase.session.ObserveParticipantsUseCase
import com.schednd.domain.usecase.session.ObserveSessionUseCase
import com.schednd.domain.usecase.session.RememberSessionUseCase
import com.schednd.domain.usecase.session.SaveAvailabilityUseCase
import com.schednd.domain.usecase.session.ScheduleSessionReminderUseCase
import com.schednd.domain.usecase.session.SetSessionStartTimeUseCase
import com.schednd.domain.model.AttendanceTier
import com.schednd.domain.model.DateSummary
import com.schednd.domain.usecase.session.ComputeDateSummariesUseCase
import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import javax.inject.Inject
import android.content.Context
import com.schednd.R
import dagger.hilt.android.qualifiers.ApplicationContext


data class EventDetailUiState(
    val event: Event? = null,
    val participants: List<Participant> = emptyList(),
    val datesAsLocal: List<LocalDate> = emptyList(),
    val participantAvailability: Map<String, Set<LocalDate>> = emptyMap(),
    val dateSummaries: List<DateSummary> = emptyList(),
    val confirmedDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    /** Cuándo se creó la sesión: es el punto de partida de la barra de espera del hero. */
    val createdAt: LocalDateTime? = null,
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
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val ensureSignedIn: EnsureSignedInUseCase,
    private val getCurrentUserId: GetCurrentUserIdUseCase,
    private val observeSession: ObserveSessionUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val saveAvailability: SaveAvailabilityUseCase,
    private val confirmSessionDate: ConfirmSessionDateUseCase,
    private val setSessionStartTime: SetSessionStartTimeUseCase,
    private val clearSessionDate: ClearSessionDateUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val rememberSession: RememberSessionUseCase,
    private val forgetSession: ForgetSessionUseCase,
    private val scheduleSessionReminder: ScheduleSessionReminderUseCase,
    private val cancelSessionReminder: CancelSessionReminderUseCase,
    private val getPlayerName: GetPlayerNameUseCase,
    private val savePlayerName: SavePlayerNameUseCase,
    private val computeDateSummaries: ComputeDateSummariesUseCase
) : ViewModel() {

    private val code: String = savedStateHandle.get<String>("code")!!
    private val myUserId: String? get() = getCurrentUserId()

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState

    init {
        // Prefill con el nombre guardado; si ya soy participante, el collect lo sobrescribe con mi nombre real
        getPlayerName()?.let { saved ->
            _uiState.update { it.copy(myName = saved) }
        }
        viewModelScope.launch {
            try {
                ensureSignedIn()
                observeSession(code)
                    .combine(observeParticipants(code)) { event, participants ->
                        Pair(event, participants)
                    }
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, error = e.message) }
                    }
                    .collect { (event, participants) ->
                        if (event != null) {
                            rememberSession(code)
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
                            val isCreator = event.creatorId == getCurrentUserId()

                            // Mantener el recordatorio local alineado con lo que diga Firestore,
                            // aunque la fecha la haya cambiado otro dispositivo.
                            if (confirmedDate != null) {
                                scheduleSessionReminder(
                                    code = code,
                                    sessionName = event.name,
                                    date = confirmedDate,
                                    startTime = event.startLocalTime
                                )
                            } else {
                                cancelSessionReminder(code)
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
                                    createdAt = event.createdAt.toLocalDateTime(),
                                    isCreator = isCreator,
                                    isLoading = false,
                                    mySavedDates = mySavedDates,
                                    myUserId = getCurrentUserId(),
                                    // Only sync draft name/dates from Firestore if not currently editing
                                    myName = if (!current.isSavingAvailability) myParticipant?.name ?: current.myName else current.myName,
                                    myDraftDates = if (!current.isSavingAvailability) mySavedDates else current.myDraftDates
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(isLoading = false, error = appContext.getString(R.string.detail_session_not_found))
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
        savePlayerName(state.myName.trim())

        viewModelScope.launch {
            _uiState.update { it.copy(isSavingAvailability = true, error = null) }
            try {
                val userId = ensureSignedIn()
                // Las fechas pasadas las descarta el caso de uso, igual que al crear o unirse.
                saveAvailability(
                    code = code,
                    userId = userId,
                    name = state.myName.trim(),
                    dates = state.myDraftDates.sorted()
                )
                _uiState.update { it.copy(isSavingAvailability = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSavingAvailability = false, error = e.message) }
            }
        }
    }

    /** Fijar día y hora es potestad del DM; la UI y las reglas de Firestore lo restringen igual. */
    fun confirmDate(date: LocalDate, startTime: LocalTime? = null) {
        if (!_uiState.value.isCreator) return
        // El caso de uso rechaza las fechas pasadas lanzando, y aquí un error borra la
        // pantalla entera. Se corta antes: si el diálogo se quedó abierto cruzando la
        // medianoche, el día de ayer simplemente no responde.
        if (date.isBefore(LocalDate.now())) return
        viewModelScope.launch {
            try {
                confirmSessionDate(code, date, startTime)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setStartTime(startTime: LocalTime?) {
        if (!_uiState.value.isCreator) return
        viewModelScope.launch {
            try { setSessionStartTime(code, startTime) }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun clearConfirmedDate() {
        if (!_uiState.value.isCreator) return
        viewModelScope.launch {
            try {
                clearSessionDate(code)
                cancelSessionReminder(code)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteEvent() {
        viewModelScope.launch {
            try {
                deleteSession(code)
                forgetSession(code)
                _uiState.update { it.copy(isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private fun Timestamp.toLocalDate(): LocalDate {
        return Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
    }

    /**
     * La fecha elegida se guarda a mediodía UTC y se lee como día suelto, pero la creación
     * es un instante real: va a la zona del móvil para poder restarla del reloj local.
     */
    private fun Timestamp.toLocalDateTime(): LocalDateTime =
        Instant.ofEpochSecond(seconds, nanoseconds.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
}
