package com.schednd.presentation.common

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.schednd.R
import java.io.IOException

/**
 * El fallo, contado en cristiano.
 *
 * Firebase lanza sus excepciones en inglés y en jerga («PERMISSION_DENIED: Missing or
 * insufficient permissions»), y hasta ahora ese texto se pintaba tal cual en la pantalla:
 * quien miraba se encontraba con el mensaje de una librería en rojo.
 *
 * El estado guarda el tipo de fallo, no el texto. Así no depende del idioma del móvil, se
 * puede afirmar en un test sin un Context delante, y quien lo pinta decide cómo enseñarlo.
 */
enum class UiError(@StringRes val messageRes: Int) {
    /** Sin red, o Firestore sin poder contestar a tiempo. */
    OFFLINE(R.string.error_offline),

    /** Las reglas dijeron que no: cosas de DM sin serlo, o una mesa que ya no es tuya. */
    NOT_ALLOWED(R.string.error_not_allowed),

    SESSION_NOT_FOUND(R.string.detail_session_not_found),

    NOTE_NOT_FOUND(R.string.error_note_not_found),

    UNKNOWN(R.string.error_unknown)
}

/** Traduce lo que sea que haya salido mal a lo poco que le importa a quien está mirando. */
fun Throwable.toUiError(): UiError = when (this) {
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED,
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> UiError.NOT_ALLOWED

        FirebaseFirestoreException.Code.UNAVAILABLE,
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> UiError.OFFLINE

        FirebaseFirestoreException.Code.NOT_FOUND -> UiError.SESSION_NOT_FOUND

        else -> UiError.UNKNOWN
    }

    is FirebaseNetworkException -> UiError.OFFLINE
    is IOException -> UiError.OFFLINE
    else -> UiError.UNKNOWN
}
