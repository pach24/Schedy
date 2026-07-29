package com.schednd.presentation.home

import com.google.firebase.Timestamp
import com.schednd.MainDispatcherRule
import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import com.schednd.domain.usecase.auth.EnsureSignedInUseCase
import com.schednd.domain.usecase.note.ObserveNotesUseCase
import com.schednd.domain.usecase.player.GetPlayerNameUseCase
import com.schednd.domain.usecase.session.GetSavedSessionCodesUseCase
import com.schednd.domain.usecase.session.GetSessionsUseCase
import com.schednd.domain.usecase.session.ObserveParticipantsUseCase
import com.schednd.fakes.FakeAuthRepository
import com.schednd.fakes.FakeEventRepository
import com.schednd.fakes.FakeNoteRepository
import com.schednd.fakes.FakePlayerRepository
import com.schednd.fakes.FakeRecentEventsRepository
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val hoy: LocalDate = LocalDate.now()

    /** Las fechas se guardan a mediodía UTC para que ninguna zona horaria las desplace. */
    private fun fecha(dias: Long): Timestamp =
        Timestamp(Date.from(hoy.plusDays(dias).atTime(12, 0).toInstant(ZoneOffset.UTC)))

    private fun sesion(code: String, dias: Long?, hora: String? = null) = Event(
        code = code,
        name = "Sesión $code",
        creatorId = "uid-1",
        confirmedDate = dias?.let { fecha(it) },
        startTime = hora
    )

    private fun viewModel(
        eventRepo: FakeEventRepository,
        codes: List<String>,
        nombre: String? = "Francisco"
    ): HomeViewModel {
        val noteRepo = FakeNoteRepository()
        return HomeViewModel(
            ensureSignedIn = EnsureSignedInUseCase(FakeAuthRepository()),
            getSavedSessionCodes = GetSavedSessionCodesUseCase(FakeRecentEventsRepository(codes)),
            getSessions = GetSessionsUseCase(eventRepo),
            observeParticipants = ObserveParticipantsUseCase(eventRepo),
            observeNotes = ObserveNotesUseCase(noteRepo),
            getPlayerName = GetPlayerNameUseCase(FakePlayerRepository(nombre))
        )
    }

    @Test
    fun `sin sesiones guardadas el estado queda vacio`() = runTest {
        val vm = viewModel(FakeEventRepository(), codes = emptyList())
        advanceUntilIdle()

        assertTrue(vm.uiState.value.allSessions.isEmpty())
        assertNull(vm.uiState.value.nextSession)
    }

    @Test
    fun `saluda con el nombre guardado y marca la sesion lista`() = runTest {
        val vm = viewModel(FakeEventRepository(), codes = emptyList())
        advanceUntilIdle()

        assertEquals("Francisco", vm.uiState.value.playerName)
        assertTrue(vm.uiState.value.isAuthReady)
    }

    @Test
    fun `la proxima sesion es la confirmada mas cercana, no la primera de la lista`() = runTest {
        val repo = FakeEventRepository(
            listOf(sesion("LEJOS", dias = 20), sesion("CERCA", dias = 2))
        )
        val vm = viewModel(repo, codes = listOf("LEJOS", "CERCA"))
        advanceUntilIdle()

        assertEquals("CERCA", vm.uiState.value.nextSession?.code)
    }

    @Test
    fun `una sesion sin fecha nunca es la proxima`() = runTest {
        val repo = FakeEventRepository(listOf(sesion("SINFECHA", dias = null)))
        val vm = viewModel(repo, codes = listOf("SINFECHA"))
        advanceUntilIdle()

        assertNull(vm.uiState.value.nextSession)
        assertEquals(1, vm.uiState.value.upcomingSessions.size)
    }

    @Test
    fun `separa pasadas de proximas`() = runTest {
        val repo = FakeEventRepository(
            listOf(sesion("VIEJA", dias = -5), sesion("NUEVA", dias = 5))
        )
        val vm = viewModel(repo, codes = listOf("VIEJA", "NUEVA"))
        advanceUntilIdle()

        assertEquals(listOf("NUEVA"), vm.uiState.value.upcomingSessions.map { it.code })
        assertEquals(listOf("VIEJA"), vm.uiState.value.pastSessions.map { it.code })
    }

    @Test
    fun `las proximas van por cercania y las pendientes de fecha al final`() = runTest {
        val repo = FakeEventRepository(
            listOf(
                sesion("SINFECHA", dias = null),
                sesion("LEJOS", dias = 10),
                sesion("CERCA", dias = 1)
            )
        )
        val vm = viewModel(repo, codes = listOf("SINFECHA", "LEJOS", "CERCA"))
        advanceUntilIdle()

        assertEquals(
            listOf("CERCA", "LEJOS", "SINFECHA"),
            vm.uiState.value.upcomingSessions.map { it.code }
        )
    }

    @Test
    fun `las pasadas van de la mas reciente a la mas antigua`() = runTest {
        val repo = FakeEventRepository(
            listOf(sesion("ANTIGUA", dias = -30), sesion("RECIENTE", dias = -2))
        )
        val vm = viewModel(repo, codes = listOf("ANTIGUA", "RECIENTE"))
        advanceUntilIdle()

        assertEquals(
            listOf("RECIENTE", "ANTIGUA"),
            vm.uiState.value.pastSessions.map { it.code }
        )
    }

    @Test
    fun `resume los participantes con sus iniciales`() = runTest {
        val repo = FakeEventRepository(
            events = listOf(sesion("ABC234", dias = 3)),
            participants = mapOf(
                "ABC234" to listOf(
                    Participant(userId = "u1", name = "francisco"),
                    Participant(userId = "u2", name = " Ana"),
                    Participant(userId = "u3", name = "")
                )
            )
        )
        val vm = viewModel(repo, codes = listOf("ABC234"))
        advanceUntilIdle()

        val carta = vm.uiState.value.allSessions.single()
        assertEquals(3, carta.participantsCount)
        assertEquals(listOf("F", "A", "?"), carta.participantInitials)
    }

    @Test
    fun `refrescar relee el nombre por si cambio`() = runTest {
        val repo = FakeEventRepository()
        val jugador = FakePlayerRepository("Francisco")
        val vm = HomeViewModel(
            ensureSignedIn = EnsureSignedInUseCase(FakeAuthRepository()),
            getSavedSessionCodes = GetSavedSessionCodesUseCase(FakeRecentEventsRepository()),
            getSessions = GetSessionsUseCase(repo),
            observeParticipants = ObserveParticipantsUseCase(repo),
            observeNotes = ObserveNotesUseCase(FakeNoteRepository()),
            getPlayerName = GetPlayerNameUseCase(jugador)
        )
        advanceUntilIdle()

        jugador.savePlayerName("Pizpireto")
        vm.refresh()
        advanceUntilIdle()

        assertEquals("Pizpireto", vm.uiState.value.playerName)
    }
}
