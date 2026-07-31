package com.schednd.domain.usecase.session

import com.schednd.fakes.FakeEventRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SaveAvailabilityUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = SaveAvailabilityUseCase(repository)

    private val hoy: LocalDate = LocalDate.now()
    private val manana: LocalDate = hoy.plusDays(1)
    private val ayer: LocalDate = hoy.minusDays(1)
    private val laSemanaPasada: LocalDate = hoy.minusDays(7)

    private suspend fun guardar(vararg fechas: LocalDate) =
        useCase(code = "MESA01", userId = "u1", name = "Kira", dates = fechas.toList())

    @Test
    fun `descarta las fechas pasadas y conserva hoy`() = runTest {
        guardar(laSemanaPasada, ayer, hoy, manana)

        assertEquals(listOf(hoy, manana), repository.savedAvailability.single().third)
    }

    @Test
    fun `deja intactas las fechas futuras`() = runTest {
        guardar(hoy, manana)

        assertEquals(listOf(hoy, manana), repository.savedAvailability.single().third)
    }

    @Test
    fun `si todas ya pasaron no queda ninguna`() = runTest {
        guardar(laSemanaPasada, ayer)

        assertTrue(repository.savedAvailability.single().third.isEmpty())
    }
}
