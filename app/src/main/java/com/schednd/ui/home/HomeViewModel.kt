package com.schednd.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.schednd.data.repository.AuthRepository
import com.schednd.data.repository.EventRepository
import com.schednd.data.repository.NoteRepository
import com.schednd.data.repository.RecentEventsRepository
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
    val latestNote: LatestNotePreview? = null
)

data class HomeUiState(
    val isAuthReady: Boolean = false,
    val nextSession: HomeSessionCard? = null,
    val allSessions: List<HomeSessionCard> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val recentEventsRepository: RecentEventsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                authRepository.ensureSignedIn()
                _uiState.value = _uiState.value.copy(isAuthReady = true)
                loadRecentEvents()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { loadRecentEvents() }
    }

    private suspend fun loadRecentEvents() {
        val codes = recentEventsRepository.getSavedCodes()
        if (codes.isEmpty()) {
            _uiState.value = _uiState.value.copy(allSessions = emptyList(), nextSession = null)
            return
        }
        val events = eventRepository.getEvents(codes)
        val cards = coroutineScope {
            events.map { event ->
                async {
                    val participants = runCatching {
                        eventRepository.observeParticipants(event.code).first()
                    }.getOrDefault(emptyList())
                    val allNotes = runCatching {
                        noteRepository.observeNotes(event.code).first()
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
                        latestNote = latest
                    )
                }
            }.awaitAll()
        }

        val today = LocalDate.now()
        val nextSession = cards
            .filter { it.confirmedDate != null && !it.confirmedDate.isBefore(today) }
            .minByOrNull { it.confirmedDate!! }
            ?: cards.firstOrNull()

        // Próxima sesión siempre primera, el resto en orden de acceso reciente
        val recentOrdered = codes.mapNotNull { code -> cards.find { it.code == code } }
        val ordered = if (nextSession != null) {
            listOf(nextSession) + recentOrdered.filter { it.code != nextSession.code }
        } else {
            recentOrdered
        }

        _uiState.value = _uiState.value.copy(
            allSessions = ordered,
            nextSession = nextSession
        )
    }

    private fun Timestamp.toLocalDate(): LocalDate =
        Instant.ofEpochSecond(seconds).atZone(ZoneOffset.UTC).toLocalDate()
}
