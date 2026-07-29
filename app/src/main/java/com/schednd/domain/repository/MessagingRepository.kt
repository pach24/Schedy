package com.schednd.domain.repository

/** Alta y baja en los avisos de una sesión. */
interface MessagingRepository {

    suspend fun subscribeToEvent(code: String)

    suspend fun unsubscribeFromEvent(code: String)
}
