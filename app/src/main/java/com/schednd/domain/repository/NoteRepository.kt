package com.schednd.domain.repository

import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import kotlinx.coroutines.flow.Flow

/** Las notas de una sesión, compartidas por todo el grupo. */
interface NoteRepository {

    fun observeNotes(code: String): Flow<List<Note>>

    suspend fun createNote(
        code: String,
        authorId: String,
        authorName: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ): String

    suspend fun updateNote(
        code: String,
        noteId: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    )

    suspend fun deleteNote(code: String, noteId: String)

    suspend fun togglePin(code: String, noteId: String, pinned: Boolean)
}
