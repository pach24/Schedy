package com.schednd.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.domain.usecase.auth.EnsureSignedInUseCase
import com.schednd.domain.usecase.auth.GetCurrentUserIdUseCase
import com.schednd.domain.usecase.note.ObserveNotesUseCase
import com.schednd.domain.usecase.player.GetPlayerNameUseCase
import com.schednd.domain.usecase.session.DeleteSessionUseCase
import com.schednd.domain.usecase.session.GetSavedSessionCodesUseCase
import com.schednd.domain.usecase.session.GetSessionsUseCase
import com.schednd.domain.usecase.session.LeaveSessionUseCase
import com.schednd.domain.usecase.session.ObserveParticipantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import javax.inject.Inject

data class LatestNotePreview(
    val title: String,
    val authorName: String,
    val body: String,
    val totalNotes: Int
)

data class HomeSessionCard(
    val code: String,
    val name: String,
    val confirmedDate: LocalDate?,
    val participantsCount: Int,
    val totalParticipants: Int,
    val participantInitials: List<String>,
    val startTime: LocalTime? = null,
    val latestNote: LatestNotePreview? = null,
    /** La mesa es mía: al mantener pulsado se ofrece borrarla en vez de salirse. */
    val isCreator: Boolean = false
) {
    /** Momento de inicio; si no hay hora fijada se cuenta desde el inicio del día. */
    val startDateTime: LocalDateTime?
        get() = confirmedDate?.atTime(startTime ?: LocalTime.MIDNIGHT)

    /** Una sesión sin hora sigue siendo "próxima" durante todo su día. */
    fun isPast(now: LocalDateTime): Boolean {
        val date = confirmedDate ?: return false
        return date.atTime(startTime ?: LocalTime.MAX).isBefore(now)
    }
}

data class HomeUiState(
    val isAuthReady: Boolean = false,
    /** Nombre elegido en el onboarding; null en sesiones creadas antes de pedirlo. */
    val playerName: String? = null,
    val nextSession: HomeSessionCard? = null,
    val allSessions: List<HomeSessionCard> = emptyList(),
    val upcomingSessions: List<HomeSessionCard> = emptyList(),
    val pastSessions: List<HomeSessionCard> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ensureSignedIn: EnsureSignedInUseCase,
    private val getCurrentUserId: GetCurrentUserIdUseCase,
    private val getSavedSessionCodes: GetSavedSessionCodesUseCase,
    private val getSessions: GetSessionsUseCase,
    private val observeParticipants: ObserveParticipantsUseCase,
    private val observeNotes: ObserveNotesUseCase,
    private val getPlayerName: GetPlayerNameUseCase,
    private val deleteSession: DeleteSessionUseCase,
    private val leaveSession: LeaveSessionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                ensureSignedIn()
                _uiState.value = _uiState.value.copy(
                    isAuthReady = true,
                    playerName = getPlayerName()
                )
                loadRecentEvents()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // El nombre se relee al volver a home: si algún día se puede cambiar, el
            // saludo no se queda con el viejo hasta reiniciar la app.
            _uiState.value = _uiState.value.copy(playerName = getPlayerName())
            loadRecentEvents()
        }
    }

    /**
     * Quitarse una sesión de encima desde el listado. Borrarla es cosa del DM y se la lleva
     * para todos; el resto solo puede salirse. La lista se recarga al terminar, que es lo
     * que hace desaparecer la fila.
     */
    fun removeSession(session: HomeSessionCard) {
        viewModelScope.launch {
            try {
                if (session.isCreator) {
                    deleteSession(session.code)
                } else {
                    val userId = getCurrentUserId() ?: ensureSignedIn()
                    leaveSession(session.code, userId)
                }
                loadRecentEvents()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    private suspend fun loadRecentEvents() {
        val codes = getSavedSessionCodes()
        if (codes.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                allSessions = emptyList(),
                upcomingSessions = emptyList(),
                pastSessions = emptyList(),
                nextSession = null
            )
            return
        }
        val events = getSessions(codes)
        val cards = coroutineScope {
            events.map { event ->
                async {
                    val participants = runCatching {
                        observeParticipants(event.code).first()
                    }.getOrDefault(emptyList())
                    val allNotes = runCatching {
                        observeNotes(event.code).first()
                    }.getOrDefault(emptyList())
                    val latest = allNotes.firstOrNull()?.let { n ->
                        LatestNotePreview(
                            title = n.title,
                            authorName = n.authorName,
                            body = n.body,
                            totalNotes = allNotes.size
                        )
                    }
                    HomeSessionCard(
                        code = event.code,
                        name = event.name,
                        confirmedDate = event.confirmedDate?.toLocalDate(),
                        participantsCount = participants.size,
                        totalParticipants = participants.size,
                        participantInitials = participants.take(5).map {
                            it.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                        },
                        startTime = event.startLocalTime,
                        latestNote = latest,
                        isCreator = event.creatorId == getCurrentUserId()
                    )
                }
            }.awaitAll()
        }

        val now = LocalDateTime.now()
        // Solo cuenta como próxima una sesión con fecha confirmada que aún no ha pasado:
        // sin ella no hay cuenta atrás que mostrar.
        val nextSession = cards
            .filter { it.confirmedDate != null && !it.isPast(now) }
            .minByOrNull { it.startDateTime!! }

        val (past, upcoming) = cards.partition { it.isPast(now) }

        // Próxima sesión primero; las pendientes de fecha al final del bloque de próximas.
        val upcomingOrdered = upcoming.sortedWith(
            compareBy(
                { it.confirmedDate == null },
                { it.startDateTime ?: LocalDateTime.MAX }
            )
        )
        val pastOrdered = past.sortedByDescending { it.startDateTime }

        _uiState.value = _uiState.value.copy(
            allSessions = upcomingOrdered + pastOrdered,
            upcomingSessions = upcomingOrdered,
            pastSessions = pastOrdered,
            nextSession = nextSession
        )
    }

    private fun Timestamp.toLocalDate(): LocalDate =
        Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
}
