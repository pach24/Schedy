package com.schednd.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schednd.domain.repository.AuthRepository
import com.schednd.domain.repository.EventRepository
import com.schednd.domain.repository.NoteRepository
import com.schednd.domain.repository.NotificationRepository
import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context
import com.schednd.R
import dagger.hilt.android.qualifiers.ApplicationContext

data class NoteEditorUiState(
    val noteId: String? = null,
    val title: String = "",
    val body: String = "",
    val tag: NoteTag = NoteTag.TRAMA,
    val pinned: Boolean = false,
    val notifyGroup: Boolean = true,
    val authorName: String = "",
    val sessionName: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val isDone: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    val canSave: Boolean get() = title.trim().isNotEmpty() && !isSaving
    val isEditMode: Boolean get() = noteId != null
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val eventRepository: EventRepository,
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val code: String = savedStateHandle.get<String>("code")!!
    private val noteIdArg: String? = savedStateHandle.get<String?>("noteId")?.takeIf { it.isNotBlank() }
    private val titleSeed: String = savedStateHandle.get<String?>("titleSeed").orEmpty()
    private val bodySeed: String = savedStateHandle.get<String?>("bodySeed").orEmpty()
    private val tagSeed: String? = savedStateHandle.get<String?>("tagSeed")

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState

    init {
        viewModelScope.launch {
            try {
                authRepository.ensureSignedIn()
                _uiState.update { it.copy(isLoading = true) }

                val event = eventRepository.getEvent(code)
                val sessionName = event?.name.orEmpty()
                val myUserId = authRepository.getCurrentUserId()
                val participants = eventRepository.observeParticipants(code).first()
                val me = participants.find { it.userId == myUserId }
                val myName = me?.name.orEmpty()

                if (noteIdArg != null) {
                    val existing = noteRepository.observeNotes(code).first()
                        .firstOrNull { it.id == noteIdArg }
                    if (existing != null) {
                        _uiState.update {
                            it.copy(
                                noteId = existing.id,
                                title = existing.title,
                                body = existing.body,
                                tag = existing.tag,
                                pinned = existing.pinned,
                                notifyGroup = false,
                                authorName = existing.authorName,
                                sessionName = sessionName,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Nota no encontrada") }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            title = titleSeed,
                            body = bodySeed,
                            tag = tagSeed?.let { t -> NoteTag.fromString(t) } ?: NoteTag.TRAMA,
                            authorName = myName,
                            sessionName = sessionName,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onBodyChange(v: String) {
        val trimmed = if (v.length > 1000) v.substring(0, 1000) else v
        _uiState.update { it.copy(body = trimmed) }
    }
    fun onTagChange(t: NoteTag) = _uiState.update { it.copy(tag = t) }
    fun onTogglePinned() = _uiState.update { it.copy(pinned = !it.pinned) }
    fun onToggleNotify() = _uiState.update { it.copy(notifyGroup = !it.notifyGroup) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val userId = authRepository.ensureSignedIn()
                val title = state.title.trim()
                val body = state.body.trim()
                val resultId = if (state.noteId != null) {
                    noteRepository.updateNote(code, state.noteId, title, body, state.tag, state.pinned)
                    state.noteId
                } else {
                    noteRepository.createNote(
                        code = code,
                        authorId = userId,
                        authorName = state.authorName.ifBlank { appContext.getString(R.string.anonymous_player) },
                        title = title,
                        body = body,
                        tag = state.tag,
                        pinned = state.pinned
                    )
                }
                if (state.notifyGroup) {
                    runCatching {
                        notificationRepository.notifyNewNote(
                            code = code,
                            senderId = userId,
                            noteId = resultId,
                            title = title,
                            authorName = state.authorName.ifBlank { appContext.getString(R.string.anonymous_player) }
                        )
                    }
                }
                _uiState.update { it.copy(isSaving = false, isDone = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun delete() {
        val id = _uiState.value.noteId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            try {
                noteRepository.deleteNote(code, id)
                _uiState.update { it.copy(isDeleting = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, error = e.message) }
            }
        }
    }
}
