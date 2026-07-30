package com.schednd.domain.repository

/** Códigos de las sesiones a las que pertenece este móvil: no hay listado en el servidor. */
interface RecentEventsRepository {

    fun saveEvent(code: String)

    fun removeEvent(code: String)

    fun getSavedCodes(): List<String>
}
