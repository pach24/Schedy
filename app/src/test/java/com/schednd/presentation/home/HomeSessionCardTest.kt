package com.schednd.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class HomeSessionCardTest {

    private fun carta(
        fecha: LocalDate? = null,
        hora: LocalTime? = null
    ) = HomeSessionCard(
        code = "ABC234",
        name = "Sesión",
        confirmedDate = fecha,
        participantsCount = 3,
        totalParticipants = 3,
        participantInitials = emptyList(),
        startTime = hora
    )

    private val martes: LocalDate = LocalDate.of(2026, 8, 4)

    @Test
    fun `sin fecha confirmada no hay momento de inicio`() {
        assertNull(carta().startDateTime)
    }

    @Test
    fun `sin hora fijada la sesion empieza a medianoche`() {
        assertEquals(martes.atStartOfDay(), carta(fecha = martes).startDateTime)
    }

    @Test
    fun `con hora fijada el momento de inicio la respeta`() {
        assertEquals(
            LocalDateTime.of(2026, 8, 4, 20, 30),
            carta(fecha = martes, hora = LocalTime.of(20, 30)).startDateTime
        )
    }

    @Test
    fun `una sesion sin fecha nunca es pasada`() {
        // Está pendiente de cuadrar, no caducada: no debe caer al bloque de pasadas.
        assertFalse(carta().isPast(LocalDateTime.of(2030, 1, 1, 0, 0)))
    }

    @Test
    fun `una sesion sin hora sigue siendo proxima durante todo su dia`() {
        val sesion = carta(fecha = martes)
        assertFalse(sesion.isPast(LocalDateTime.of(2026, 8, 4, 0, 1)))
        assertFalse(sesion.isPast(LocalDateTime.of(2026, 8, 4, 23, 58)))
        assertTrue(sesion.isPast(LocalDateTime.of(2026, 8, 5, 0, 1)))
    }

    @Test
    fun `con hora fijada pasa a pasada en cuanto se cruza esa hora`() {
        val sesion = carta(fecha = martes, hora = LocalTime.of(20, 0))
        assertFalse(sesion.isPast(LocalDateTime.of(2026, 8, 4, 19, 59)))
        assertTrue(sesion.isPast(LocalDateTime.of(2026, 8, 4, 20, 1)))
    }
}
