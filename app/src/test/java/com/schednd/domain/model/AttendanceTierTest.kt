package com.schednd.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AttendanceTierTest {

    @Test
    fun `sin participantes es insuficiente`() {
        assertEquals(AttendanceTier.INSUFFICIENT, computeAttendanceTier(count = 0, total = 0))
    }

    @Test
    fun `todos disponibles es FULL`() {
        assertEquals(AttendanceTier.FULL, computeAttendanceTier(count = 5, total = 5))
    }

    @Test
    fun `seis de siete se queda en VIABLE`() {
        // 6/7 = 0.857, justo por debajo del umbral 0.86 de FULL.
        assertEquals(AttendanceTier.VIABLE, computeAttendanceTier(count = 6, total = 7))
    }

    @Test
    fun `tres cuartos es VIABLE`() {
        assertEquals(AttendanceTier.VIABLE, computeAttendanceTier(count = 3, total = 4))
    }

    @Test
    fun `tres de cinco es LIMITED`() {
        assertEquals(AttendanceTier.LIMITED, computeAttendanceTier(count = 3, total = 5))
    }

    @Test
    fun `la mitad es insuficiente`() {
        assertEquals(AttendanceTier.INSUFFICIENT, computeAttendanceTier(count = 2, total = 4))
    }

    @Test
    fun `nadie disponible es insuficiente`() {
        assertEquals(AttendanceTier.INSUFFICIENT, computeAttendanceTier(count = 0, total = 4))
    }

    @Test
    fun `los umbrales son monotonos`() {
        val total = 10
        val orden = listOf(
            AttendanceTier.INSUFFICIENT,
            AttendanceTier.LIMITED,
            AttendanceTier.VIABLE,
            AttendanceTier.FULL
        )
        // Al subir la asistencia el tier nunca debe empeorar.
        var previo = 0
        for (count in 0..total) {
            val actual = orden.indexOf(computeAttendanceTier(count, total))
            assert(actual >= previo) {
                "computeAttendanceTier($count, $total) empeoró respecto a ${count - 1}"
            }
            previo = actual
        }
    }
}
