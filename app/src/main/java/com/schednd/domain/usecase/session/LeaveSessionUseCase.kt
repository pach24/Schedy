package com.schednd.domain.usecase.session

import com.schednd.domain.repository.EventRepository
import com.schednd.domain.repository.MessagingRepository
import com.schednd.domain.repository.RecentEventsRepository
import com.schednd.domain.repository.SessionReminderScheduler
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Salirse de una sesión: borra el propio documento de participante —lo único que las
 * reglas de Firestore dejan tocar de una sesión ajena— y deshace el rastro que la sesión
 * dejó en este móvil.
 *
 * Va todo junto porque se sale desde dos sitios, la propia sesión y el listado, y hacerlo
 * a medias deja secuelas raras: la sesión reapareciendo en la lista o un recordatorio de
 * una mesa a la que ya no vas.
 */
class LeaveSessionUseCase @Inject constructor(
    private val eventRepository: EventRepository,
    private val recentEventsRepository: RecentEventsRepository,
    private val messagingRepository: MessagingRepository,
    private val reminderScheduler: SessionReminderScheduler
) {
    suspend operator fun invoke(code: String, userId: String) {
        eventRepository.removeParticipant(code, userId)
        recentEventsRepository.removeEvent(code)
        reminderScheduler.cancel(code)
        try {
            messagingRepository.unsubscribeFromEvent(code)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Ya me he ido: que la baja del topic falle no es motivo para devolverme a la
            // sesión. FCM reintenta la suya en cuanto vuelve a haber red.
        }
    }
}
