package com.schednd.presentation.detail

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class SessionStageTest {

    private val fecha: LocalDate = LocalDate.of(2026, 8, 2)
    private val hora: LocalTime = LocalTime.of(18, 0)

    private fun instante(dia: Int, hora: Int, minuto: Int = 0): LocalDateTime =
        LocalDateTime.of(2026, 8, dia, hora, minuto)

    @Test
    fun `antes de la hora la sesion sigue siendo proxima`() {
        assertEquals(SessionStage.UPCOMING, stageAt(instante(2, 17, 59), fecha, hora))
    }

    @Test
    fun `al dar la hora exacta la sesion pasa a en curso`() {
        assertEquals(SessionStage.LIVE, stageAt(instante(2, 18), fecha, hora))
    }

    @Test
    fun `sigue en curso mientras dura la ventana`() {
        assertEquals(SessionStage.LIVE, stageAt(instante(2, 20, 59), fecha, hora))
    }

    @Test
    fun `al agotarse la ventana la sesion queda pasada aunque sea el mismo dia`() {
        // Este es el hueco que antes se quedaba clavado en ceros hasta medianoche.
        assertEquals(SessionStage.PAST, stageAt(instante(2, 21), fecha, hora))
        assertEquals(SessionStage.PAST, stageAt(instante(2, 23, 59), fecha, hora))
    }

    @Test
    fun `al dia siguiente la sesion esta pasada`() {
        assertEquals(SessionStage.PAST, stageAt(instante(3, 10), fecha, hora))
    }

    @Test
    fun `sin hora fijada el dia entero cuenta como proxima`() {
        assertEquals(SessionStage.UPCOMING, stageAt(instante(2, 0), fecha, null))
        assertEquals(SessionStage.UPCOMING, stageAt(instante(2, 23, 59), fecha, null))
    }

    @Test
    fun `sin hora fijada la sesion pasa al cruzar la medianoche`() {
        assertEquals(SessionStage.PAST, stageAt(instante(3, 0), fecha, null))
    }

    @Test
    fun `los dias sueltos redondean hacia arriba`() {
        assertEquals(0, ceilDays(Duration.ZERO))
        assertEquals(1, ceilDays(Duration.ofHours(4)))
        assertEquals(1, ceilDays(Duration.ofDays(1)))
        assertEquals(2, ceilDays(Duration.ofDays(1).plusSeconds(1)))
        assertEquals(0, ceilDays(Duration.ofHours(-3)))
    }
}
