package com.schednd.domain.usecase.auth

import com.schednd.domain.repository.AuthRepository
import javax.inject.Inject

/** Asegura sesión anónima: sin uid las reglas rechazan hasta la lectura. */
class EnsureSignedInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): String = authRepository.ensureSignedIn()
}
