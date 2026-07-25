package com.schednd.domain.usecase

import com.schednd.domain.model.AttendanceTier
import com.schednd.model.Participant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ComputeDateSummariesUseCaseTest {

    private val useCase = ComputeDateSummariesUseCase()

    private val today: LocalDate = LocalDate.now()
    private val manana: LocalDate = today.plusDays(1)
    private val enUnaSemana: LocalDate = today.plusDays(7)
    private val ayer: LocalDate = today.minusDays(1)

    private fun jugador(id: String, nombre: String) = Participant(userId = id, name = nombre)

    private val kira = jugador("u1", "Kira")
    private val aldric = jugador("u2", "Aldric")
    private val veyra = jugador("u3", "Veyra")

    @Test
    fun `sin participantes no hay resumenes`() {
        val resultado = useCase(
            dates = listOf(manana),
            participants = emptyList(),
            availability = emptyMap()
        )
        assertTrue(resultado.isEmpty())
    }

    @Test
    fun `cuenta solo a quien marco la fecha`() {
        val resultado = useCase(
            dates = listOf(manana),
            participants = listOf(kira, aldric, veyra),
            availability = mapOf(
                "u1" to setOf(manana),
                "u2" to setOf(manana),
                "u3" to setOf(enUnaSemana)
            )
        )

        assertEquals(1, resultado.size)
        val resumen = resultado.single()
        assertEquals(manana, resumen.date)
        assertEquals(2, resumen.count)
        assertEquals(3, resumen.total)
        assertEquals(listOf("Veyra"), resumen.absentNames)
    }

    @Test
    fun `un participante sin entrada en el mapa cuenta como ausente`() {
        val resultado = useCase(
            dates = listOf(manana),
            participants = listOf(kira, aldric),
            availability = mapOf("u1" to setOf(manana))
        )

        val resumen = resultado.single()
        assertEquals(1, resumen.count)
        assertEquals(listOf("Aldric"), resumen.absentNames)
    }

    @Test
    fun `descarta fechas pasadas pero conserva hoy`() {
        val resultado = useCase(
            dates = listOf(ayer, today, manana),
            participants = listOf(kira),
            availability = mapOf("u1" to setOf(ayer, today, manana))
        )

        assertEquals(listOf(today, manana), resultado.map { it.date }.sorted())
    }

    @Test
    fun `ordena de mas a menos asistentes`() {
        val resultado = useCase(
            dates = listOf(manana, enUnaSemana),
            participants = listOf(kira, aldric, veyra),
            availability = mapOf(
                "u1" to setOf(manana, enUnaSemana),
                "u2" to setOf(enUnaSemana),
                "u3" to setOf(enUnaSemana)
            )
        )

        assertEquals(listOf(enUnaSemana, manana), resultado.map { it.date })
        assertEquals(3, resultado.first().count)
    }

    @Test
    fun `asigna el tier correspondiente a la asistencia`() {
        val resultado = useCase(
            dates = listOf(manana),
            participants = listOf(kira, aldric, veyra),
            availability = mapOf(
                "u1" to setOf(manana),
                "u2" to setOf(manana),
                "u3" to setOf(manana)
            )
        )

        assertEquals(AttendanceTier.FULL, resultado.single().tier)
    }

    @Test
    fun `una fecha que nadie marco sigue apareciendo con cero`() {
        val resultado = useCase(
            dates = listOf(manana),
            participants = listOf(kira, aldric),
            availability = mapOf("u1" to setOf(enUnaSemana))
        )

        val resumen = resultado.single()
        assertEquals(0, resumen.count)
        assertEquals(AttendanceTier.INSUFFICIENT, resumen.tier)
        assertEquals(listOf("Kira", "Aldric"), resumen.absentNames)
    }
}
