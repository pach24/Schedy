package com.schednd.presentation.onboarding

import androidx.lifecycle.ViewModel
import com.schednd.data.repository.PlayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val playerRepository: PlayerRepository
) : ViewModel() {

    fun savePlayerName(name: String) = playerRepository.savePlayerName(name)
}
