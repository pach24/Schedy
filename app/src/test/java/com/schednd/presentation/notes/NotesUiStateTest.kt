package com.schednd.presentation.notes

import com.schednd.domain.model.Note
import com.schednd.domain.model.NoteTag
import org.junit.Assert.assertEquals
import org.junit.Test

class NotesUiStateTest {

    private fun nota(
        id: String,
        titulo: String = "",
        cuerpo: String = "",
        autor: String = "Máster",
        etiqueta: NoteTag = NoteTag.OTROS
    ) = Note(id = id, title = titulo, body = cuerpo, authorName = autor, tag = etiqueta)

    private val notas = listOf(
        nota("1", titulo = "El collar de Vecna", etiqueta = NoteTag.TRAMA),
        nota("2", titulo = "Botín del dragón", cuerpo = "3 pociones", etiqueta = NoteTag.LOOT),
        nota("3", titulo = "Tabernero", autor = "Legolas", etiqueta = NoteTag.NPC),
        nota("4", titulo = "Otra de trama", etiqueta = NoteTag.TRAMA)
    )

    private val estado = NotesUiState(notes = notas)

    @Test
    fun `sin filtros devuelve todas`() {
        assertEquals(4, estado.filteredNotes().size)
    }

    @Test
    fun `filtra por etiqueta`() {
        val resultado = estado.copy(selectedTag = NoteTag.TRAMA).filteredNotes()
        assertEquals(listOf("1", "4"), resultado.map { it.id })
    }

    @Test
    fun `busca en titulo cuerpo y autor`() {
        assertEquals(listOf("1"), estado.copy(query = "vecna").filteredNotes().map { it.id })
        assertEquals(listOf("2"), estado.copy(query = "pociones").filteredNotes().map { it.id })
        assertEquals(listOf("3"), estado.copy(query = "legolas").filteredNotes().map { it.id })
    }

    @Test
    fun `la busqueda ignora mayusculas y espacios de sobra`() {
        assertEquals(listOf("1"), estado.copy(query = "  VECNA  ").filteredNotes().map { it.id })
    }

    @Test
    fun `etiqueta y busqueda se combinan`() {
        val resultado = estado.copy(selectedTag = NoteTag.TRAMA, query = "otra").filteredNotes()
        assertEquals(listOf("4"), resultado.map { it.id })
    }

    @Test
    fun `una busqueda sin resultados devuelve lista vacia`() {
        assertEquals(emptyList<Note>(), estado.copy(query = "beholder").filteredNotes())
    }

    @Test
    fun `cuenta las notas de cada etiqueta, incluidas las que no tienen ninguna`() {
        val cuentas = estado.tagCounts()
        assertEquals(NoteTag.entries.size, cuentas.size)
        assertEquals(2, cuentas[NoteTag.TRAMA])
        assertEquals(1, cuentas[NoteTag.LOOT])
        assertEquals(0, cuentas[NoteTag.PERSONAJE])
    }
}
