package com.schednd.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.schednd.domain.usecase.player.SavePlayerNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val savePlayerNameUseCase: SavePlayerNameUseCase
) : ViewModel() {

    fun savePlayerName(name: String) = savePlayerNameUseCase(name)
}
