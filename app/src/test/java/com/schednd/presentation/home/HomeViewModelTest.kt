package com.schednd.presentation.home

import com.google.firebase.Timestamp
import com.schednd.MainDispatcherRule
import com.schednd.domain.model.Event
import com.schednd.domain.model.Participant
import com.schednd.domain.usecase.auth.EnsureSignedInUseCase
import com.schednd.domain.usecase.auth.GetCurrentUserIdUseCase
import com.schednd.domain.usecase.note.ObserveNotesUseCase
import com.schednd.domain.usecase.player.GetPlayerNameUseCase
import com.schednd.domain.usecase.session.DeleteSessionUseCase
import com.schednd.domain.usecase.session.GetSavedSessionCodesUseCase
import com.schednd.domain.usecase.session.GetSessionsUseCase
import com.schednd.domain.usecase.session.LeaveSessionUseCase
import com.schednd.domain.usecase.session.ObserveParticipantsUseCase
import com.schednd.fakes.FakeAuthRepository
import com.schednd.fakes.FakeEventRepository
import com.schednd.fakes.FakeMessagingRepository
import com.schednd.fakes.FakeNoteRepository
import com.schednd.fakes.FakePlayerRepository
import com.schednd.fakes.FakeRecentEventsRepository
import com.schednd.fakes.FakeSessionReminderScheduler
import com.schednd.presentation.common.UiError
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
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
        nombre: String? = "Francisco",
        recientes: FakeRecentEventsRepository = FakeRecentEventsRepository(codes),
        avisos: FakeMessagingRepository = FakeMessagingRepository(),
        recordatorios: FakeSessionReminderScheduler = FakeSessionReminderScheduler(),
        auth: FakeAuthRepository = FakeAuthRepository()
    ): HomeViewModel {
        val noteRepo = FakeNoteRepository()
        return HomeViewModel(
            ensureSignedIn = EnsureSignedInUseCase(auth),
            getCurrentUserId = GetCurrentUserIdUseCase(auth),
            getSavedSessionCodes = GetSavedSessionCodesUseCase(recientes),
            getSessions = GetSessionsUseCase(eventRepo),
            observeParticipants = ObserveParticipantsUseCase(eventRepo),
            observeNotes = ObserveNotesUseCase(noteRepo),
            getPlayerName = GetPlayerNameUseCase(FakePlayerRepository(nombre)),
            deleteSession = DeleteSessionUseCase(eventRepo, recientes, avisos, recordatorios),
            leaveSession = LeaveSessionUseCase(eventRepo, recientes, avisos, recordatorios)
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
    fun `saluda con el nombre guardado y deja de cargar`() = runTest {
        val vm = viewModel(FakeEventRepository(), codes = emptyList())
        advanceUntilIdle()

        assertEquals("Francisco", vm.uiState.value.playerName)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `nada se lee antes de tener sesion anonima`() = runTest {
        // Sin uid las reglas de Firestore rechazan hasta la lectura: si el listado se
        // pidiera antes de registrarse, la respuesta sería un PERMISSION_DENIED.
        val auth = FakeAuthRepository()
        val repo = FakeEventRepository(listOf(sesion("ABC234", dias = 3)))
        var lecturasSinSesion = 0
        repo.beforeGetEvents = { if (auth.ensureSignedInCalls == 0) lecturasSinSesion++ }
        val vm = viewModel(repo, codes = listOf("ABC234"), auth = auth)
        advanceUntilIdle()

        // Y el refresco que la pantalla pide nada más componerse tampoco se adelanta.
        vm.refresh()
        advanceUntilIdle()

        assertEquals(0, lecturasSinSesion)
        assertEquals(listOf("ABC234"), vm.uiState.value.allSessions.map { it.code })
    }

    @Test
    fun `si la carga falla lo cuenta sin reventar y reintentar lo arregla`() = runTest {
        val repo = FakeEventRepository(listOf(sesion("ABC234", dias = 3)))
        // Un IOException y no una FirebaseFirestoreException: la suya no se puede ni
        // construir fuera del móvil, porque su enum de códigos arranca con un SparseArray.
        repo.failNextGetEvents = IOException("sin red")
        val vm = viewModel(repo, codes = listOf("ABC234"))
        advanceUntilIdle()

        assertEquals(UiError.OFFLINE, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.allSessions.isEmpty())

        vm.refresh()
        advanceUntilIdle()

        assertNull(vm.uiState.value.error)
        assertEquals(listOf("ABC234"), vm.uiState.value.allSessions.map { it.code })
    }

    @Test
    fun `la rueda solo sale en la primera carga`() = runTest {
        // Sin mesas guardadas el listado siempre viene vacío: si la rueda se guiara por eso,
        // cada vuelta a home taparía el hueco de «aún no tienes mesas» con un parpadeo.
        val vm = viewModel(FakeEventRepository(), codes = emptyList())
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)

        vm.refresh()

        assertFalse(vm.uiState.value.isLoading)
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
        val auth = FakeAuthRepository()
        val recientes = FakeRecentEventsRepository()
        val avisos = FakeMessagingRepository()
        val recordatorios = FakeSessionReminderScheduler()
        val vm = HomeViewModel(
            ensureSignedIn = EnsureSignedInUseCase(auth),
            getCurrentUserId = GetCurrentUserIdUseCase(auth),
            getSavedSessionCodes = GetSavedSessionCodesUseCase(recientes),
            getSessions = GetSessionsUseCase(repo),
            observeParticipants = ObserveParticipantsUseCase(repo),
            observeNotes = ObserveNotesUseCase(FakeNoteRepository()),
            getPlayerName = GetPlayerNameUseCase(jugador),
            deleteSession = DeleteSessionUseCase(repo, recientes, avisos, recordatorios),
            leaveSession = LeaveSessionUseCase(repo, recientes, avisos, recordatorios)
        )
        advanceUntilIdle()

        jugador.savePlayerName("Pizpireto")
        vm.refresh()
        advanceUntilIdle()

        assertEquals("Pizpireto", vm.uiState.value.playerName)
    }

    @Test
    fun `el DM borra la sesion desde el listado y no queda rastro en el movil`() = runTest {
        val repo = FakeEventRepository(listOf(sesion("ABC234", dias = 3)))
        val recientes = FakeRecentEventsRepository(listOf("ABC234"))
        val recordatorios = FakeSessionReminderScheduler()
        val vm = viewModel(
            repo,
            codes = listOf("ABC234"),
            recientes = recientes,
            recordatorios = recordatorios
        )
        advanceUntilIdle()

        vm.removeSession(vm.uiState.value.allSessions.single())
        advanceUntilIdle()

        assertEquals(listOf("ABC234"), repo.deletedCodes)
        assertTrue(recientes.getSavedCodes().isEmpty())
        assertEquals(listOf("ABC234"), recordatorios.cancelled)
        assertTrue(vm.uiState.value.allSessions.isEmpty())
    }

    @Test
    fun `quien no es el DM se sale sin llevarse la sesion por delante`() = runTest {
        val ajena = sesion("ABC234", dias = 3).copy(creatorId = "otro-dm")
        val repo = FakeEventRepository(
            events = listOf(ajena),
            participants = mapOf("ABC234" to listOf(Participant(userId = "uid-1", name = "Francisco")))
        )
        val recientes = FakeRecentEventsRepository(listOf("ABC234"))
        val vm = viewModel(repo, codes = listOf("ABC234"), recientes = recientes)
        advanceUntilIdle()

        vm.removeSession(vm.uiState.value.allSessions.single())
        advanceUntilIdle()

        assertEquals(listOf("ABC234" to "uid-1"), repo.removedParticipants)
        assertTrue(repo.deletedCodes.isEmpty())
        assertTrue(recientes.getSavedCodes().isEmpty())
        assertTrue(vm.uiState.value.allSessions.isEmpty())
    }
}
