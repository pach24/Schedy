package com.schednd.domain.usecase.session

import com.schednd.fakes.FakeEventRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ConfirmSessionDateUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = ConfirmSessionDateUseCase(repository)

    private val hoy: LocalDate = LocalDate.now()
    private val manana: LocalDate = hoy.plusDays(1)
    private val ayer: LocalDate = hoy.minusDays(1)

    @Test
    fun `fija una fecha futura con su hora`() = runTest {
        useCase("MESA01", manana, LocalTime.of(18, 30))

        val fijada = repository.confirmedDates.single()
        assertEquals("MESA01", fijada.first)
        assertEquals(manana, fijada.second)
        assertEquals(LocalTime.of(18, 30), fijada.third)
    }

    @Test
    fun `hoy sigue siendo fijable`() = runTest {
        useCase("MESA01", hoy)

        assertEquals(hoy, repository.confirmedDates.single().second)
    }

    @Test
    fun `rechaza una fecha pasada`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase("MESA01", ayer) }
        }
    }

    @Test
    fun `una fecha pasada no llega al repositorio`() = runTest {
        runCatching { useCase("MESA01", ayer) }

        assertTrue(repository.confirmedDates.isEmpty())
    }
}
