package com.schednd.presentation.notes

import androidx.lifecycle.SavedStateHandle
import com.schednd.MainDispatcherRule
import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import com.schednd.domain.usecase.note.DeleteNoteUseCase
import com.schednd.domain.usecase.note.ObserveNotesUseCase
import com.schednd.domain.usecase.note.ToggleNotePinUseCase
import com.schednd.fakes.FakeNoteRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NotesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val notaInicial = Note(id = "1", title = "Trama", tag = NoteTag.TRAMA)

    private fun viewModel(repo: FakeNoteRepository) = NotesViewModel(
        savedStateHandle = SavedStateHandle(mapOf("code" to "ABC234")),
        observeNotes = ObserveNotesUseCase(repo),
        toggleNotePin = ToggleNotePinUseCase(repo),
        deleteNoteUseCase = DeleteNoteUseCase(repo)
    )

    @Test
    fun `arranca cargando y deja de cargar al llegar las notas`() = runTest {
        val repo = FakeNoteRepository(listOf(notaInicial))
        val vm = viewModel(repo)

        assertTrue(vm.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(listOf("1"), vm.uiState.value.notes.map { it.id })
    }

    @Test
    fun `refleja lo que escribe otro miembro del grupo sin volver a pedirlo`() = runTest {
        val repo = FakeNoteRepository(listOf(notaInicial))
        val vm = viewModel(repo)
        advanceUntilIdle()

        repo.emit(listOf(notaInicial, Note(id = "2", title = "Loot", tag = NoteTag.LOOT)))
        advanceUntilIdle()

        assertEquals(listOf("1", "2"), vm.uiState.value.notes.map { it.id }.sorted())
    }

    @Test
    fun `fijar una nota invierte su estado`() = runTest {
        val repo = FakeNoteRepository(listOf(notaInicial))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.togglePin(noteId = "1", currentlyPinned = false)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.notes.single { it.id == "1" }.pinned)
    }

    @Test
    fun `borrar una nota la saca del estado`() = runTest {
        val repo = FakeNoteRepository(listOf(notaInicial, Note(id = "2")))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.deleteNote("1")
        advanceUntilIdle()

        assertEquals(listOf("2"), vm.uiState.value.notes.map { it.id })
    }

    @Test
    fun `los filtros solo tocan el estado, no la lista de origen`() = runTest {
        val repo = FakeNoteRepository(listOf(notaInicial, Note(id = "2", tag = NoteTag.LOOT)))
        val vm = viewModel(repo)
        advanceUntilIdle()

        vm.setQuery("trama")
        vm.setSelectedTag(NoteTag.TRAMA)

        assertEquals("trama", vm.uiState.value.query)
        assertEquals(NoteTag.TRAMA, vm.uiState.value.selectedTag)
        assertEquals(2, vm.uiState.value.notes.size)
        assertEquals(listOf("1"), vm.uiState.value.filteredNotes().map { it.id })
    }
}
