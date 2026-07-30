package com.schednd.domain.usecase.note

import com.schednd.domain.model.Note
import com.schednd.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveNotesUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    operator fun invoke(code: String): Flow<List<Note>> = noteRepository.observeNotes(code)
}
