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
import com.schednd.presentation.common.UiError
import com.schednd.presentation.common.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    /**
     * Todavía no ha llegado a completarse ninguna carga: no hay nada que enseñar, ni
     * siquiera para decir que no hay nada. Los refrescos posteriores no lo vuelven a
     * levantar; se hacen por detrás, sin quitar de en medio lo que ya se estaba viendo.
     */
    val isLoading: Boolean = true,
    /** Nombre elegido en el onboarding; null en sesiones creadas antes de pedirlo. */
    val playerName: String? = null,
    val nextSession: HomeSessionCard? = null,
    val allSessions: List<HomeSessionCard> = emptyList(),
    val upcomingSessions: List<HomeSessionCard> = emptyList(),
    val pastSessions: List<HomeSessionCard> = emptyList(),
    val error: UiError? = null
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

    /** La carga en curso, para no lanzar dos a la vez. */
    private var loadJob: Job? = null

    /**
     * Ya se ha completado una carga alguna vez. A partir de ahí no se vuelve a poner la
     * rueda: volver a home con el listado ya traído lo relee por detrás, y quien no tenga
     * ninguna mesa ve su hueco de siempre en vez de un parpadeo.
     */
    private var everLoaded = false

    init {
        load()
    }

    /** Al volver a home, y al tocar «Reintentar» cuando la anterior se quedó a medias. */
    fun refresh() {
        load()
    }

    /**
     * Toda lectura pasa por aquí, y siempre detrás de la sesión anónima: sin uid las reglas
     * de Firestore rechazan hasta la lectura. La pantalla pide refrescar nada más
     * componerse, con el registro todavía en camino, y aquello se iba a Firestore sin
     * esperarlo: de ahí el «PERMISSION_DENIED» crudo en mitad de la pantalla.
     */
    private fun load() {
        // Arranque y primer refresco salen casi a la vez y traerían lo mismo.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !everLoaded, error = null) }
            try {
                ensureSignedIn()
                // El nombre se relee en cada carga: si algún día se puede cambiar, el
                // saludo no se queda con el viejo hasta reiniciar la app.
                _uiState.update { it.copy(playerName = getPlayerName()) }
                loadRecentEvents()
                everLoaded = true
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUiError()) }
            }
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
                // Igual que al leer: primero la sesión anónima, que es lo que hace que las
                // reglas dejen escribir. Si ya hay una, no cuesta nada.
                val userId = ensureSignedIn()
                if (session.isCreator) {
                    deleteSession(session.code)
                } else {
                    leaveSession(session.code, userId)
                }
                loadRecentEvents()
                _uiState.update { it.copy(error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUiError()) }
            }
        }
    }

    private suspend fun loadRecentEvents() {
        val codes = getSavedSessionCodes()
        if (codes.isEmpty()) {
            _uiState.update {
                it.copy(
                    allSessions = emptyList(),
                    upcomingSessions = emptyList(),
                    pastSessions = emptyList(),
                    nextSession = null
                )
            }
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

        _uiState.update {
            it.copy(
                allSessions = upcomingOrdered + pastOrdered,
                upcomingSessions = upcomingOrdered,
                pastSessions = pastOrdered,
                nextSession = nextSession
            )
        }
    }

    private fun Timestamp.toLocalDate(): LocalDate =
        Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
}
