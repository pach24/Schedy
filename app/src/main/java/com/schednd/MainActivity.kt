package com.schednd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.schednd.data.repository.PlayerRepository
import com.schednd.ui.navigation.SchedndNavGraph
import com.schednd.ui.theme.SchedndTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var playerRepository: PlayerRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startDestination = if (playerRepository.isOnboardingComplete()) "home" else "onboarding"
        setContent {
            SchedndTheme {
                val navController = rememberNavController()
                SchedndNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}
