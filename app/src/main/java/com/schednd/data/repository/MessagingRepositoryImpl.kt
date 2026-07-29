package com.schednd.data.repository

import com.schednd.domain.repository.MessagingRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessagingRepositoryImpl @Inject constructor(
    private val messaging: FirebaseMessaging
) : MessagingRepository {
    override suspend fun subscribeToEvent(code: String) {
        messaging.subscribeToTopic("event_$code").await()
    }

    override suspend fun unsubscribeFromEvent(code: String) {
        messaging.unsubscribeFromTopic("event_$code").await()
    }
}
