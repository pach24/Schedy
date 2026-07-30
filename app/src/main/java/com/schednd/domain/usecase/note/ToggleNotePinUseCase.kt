package com.schednd.domain.usecase.note

import com.schednd.domain.repository.NoteRepository
import javax.inject.Inject

class ToggleNotePinUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(code: String, noteId: String, pinned: Boolean) =
        noteRepository.togglePin(code, noteId, pinned)
}
