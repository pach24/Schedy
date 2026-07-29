package com.schednd.domain.usecase.note

import com.schednd.domain.model.NoteTag
import com.schednd.domain.repository.NoteRepository
import javax.inject.Inject

class UpdateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        code: String,
        noteId: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ) = noteRepository.updateNote(code, noteId, title, body, tag, pinned)
}
