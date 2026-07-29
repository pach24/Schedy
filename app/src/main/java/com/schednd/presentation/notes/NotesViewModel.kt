package com.schednd.presentation.notes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.schednd.domain.repository.NoteRepository
import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotesUiState(
    val notes: List<Note> = emptyList(),
    val query: String = "",
    val selectedTag: NoteTag? = null,
    val isLoading: Boolean = true,
    val participantsCount: Int = 0,
    val error: String? = null
)

@HiltViewModel
class NotesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val code: String = savedStateHandle.get<String>("code")!!

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState

    init {
        viewModelScope.launch {
            noteRepository.observeNotes(code)
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { notes ->
                    _uiState.update { it.copy(notes = notes, isLoading = false) }
                }
        }
    }

    fun setQuery(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    fun setSelectedTag(tag: NoteTag?) {
        _uiState.update { it.copy(selectedTag = tag) }
    }

    fun setParticipantsCount(n: Int) {
        _uiState.update { it.copy(participantsCount = n) }
    }

    fun togglePin(noteId: String, currentlyPinned: Boolean) {
        viewModelScope.launch {
            try {
                noteRepository.togglePin(code, noteId, !currentlyPinned)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNote(code, noteId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

fun NotesUiState.filteredNotes(): List<Note> {
    val q = query.trim().lowercase()
    return notes.filter { n ->
        (selectedTag == null || n.tag == selectedTag) &&
        (q.isEmpty() ||
            n.title.lowercase().contains(q) ||
            n.body.lowercase().contains(q) ||
            n.authorName.lowercase().contains(q))
    }
}

fun NotesUiState.tagCounts(): Map<NoteTag, Int> =
    NoteTag.entries.associateWith { tag -> notes.count { it.tag == tag } }
