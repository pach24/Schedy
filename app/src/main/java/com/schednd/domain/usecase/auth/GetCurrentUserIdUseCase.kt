package com.schednd.domain.usecase.auth

import com.schednd.domain.repository.AuthRepository
import javax.inject.Inject

/** Quién soy, si es que ya hay sesión. */
class GetCurrentUserIdUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): String? = authRepository.getCurrentUserId()
}
