package com.schednd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.schednd.domain.usecase.player.IsOnboardingCompleteUseCase
import com.schednd.presentation.navigation.SchedyNavGraph
import com.schednd.ui.theme.SchedyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var isOnboardingComplete: IsOnboardingCompleteUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startDestination = if (isOnboardingComplete()) "home" else "onboarding"
        setContent {
            SchedyTheme {
                val navController = rememberNavController()
                SchedyNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
