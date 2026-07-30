package com.schednd.domain.usecase.note

import com.schednd.domain.model.NoteTag
import com.schednd.domain.repository.NoteRepository
import javax.inject.Inject

class CreateNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(
        code: String,
        authorId: String,
        authorName: String,
        title: String,
        body: String,
        tag: NoteTag,
        pinned: Boolean
    ): String = noteRepository.createNote(code, authorId, authorName, title, body, tag, pinned)
}
