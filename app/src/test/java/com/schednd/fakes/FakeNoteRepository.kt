package com.schednd.fakes

import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import com.schednd.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Notas en memoria. Al estar el repositorio detrás de una interfaz de dominio, no hace
 * falta librería de mocks: un doble escrito a mano es más legible y no miente sobre el
 * comportamiento, porque de verdad guarda y devuelve lo que se le pide.
 */
class FakeNoteRepository(initial: List<Note> = emptyList()) : NoteRepository {

    private val notes = MutableStateFlow(initial)

    /** Para simular que otro miembro del grupo escribe mientras la pantalla está abierta. */
    fun emit(newNotes: List<Note>) {
        notes.value = newNotes
    }

    override fun observeNotes(code: String): Flow<List<Note>> =
        notes.map { list -> list.sortedByDescending { it.pinned } }

    override suspend fun createNote(
        code: String,
        authorId: String,
        authorName: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ): String {
        val id = "note-${notes.value.size + 1}"
        notes.value = notes.value + Note(
            id = id,
            authorId = authorId,
            authorName = authorName,
            title = title,
            body = body,
            tag = tag,
            pinned = pinned
        )
        return id
    }

    override suspend fun updateNote(
        code: String,
        noteId: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ) {
        notes.value = notes.value.map {
            if (it.id == noteId) it.copy(title = title, body = body, tag = tag, pinned = pinned)
            else it
        }
    }

    override suspend fun deleteNote(code: String, noteId: String) {
        notes.value = notes.value.filterNot { it.id == noteId }
    }

    override suspend fun togglePin(code: String, noteId: String, pinned: Boolean) {
        notes.value = notes.value.map { if (it.id == noteId) it.copy(pinned = pinned) else it }
    }
}
