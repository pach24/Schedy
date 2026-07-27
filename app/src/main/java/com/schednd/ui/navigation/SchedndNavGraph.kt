package com.schednd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.schednd.ui.create.CreateEventScreen
import com.schednd.ui.detail.EditAvailabilityScreen
import com.schednd.ui.detail.EventDetailViewModel
import com.schednd.ui.home.HomeScreen
import com.schednd.ui.join.JoinEventScreen
import com.schednd.ui.onboarding.OnboardingScreen
import com.schednd.ui.session.SessionShellScreen
import com.schednd.ui.theme.NavEnterTransition
import com.schednd.ui.theme.NavExitTransition
import com.schednd.ui.theme.NavPopEnterTransition
import com.schednd.ui.theme.NavPopExitTransition

@Composable
fun SchedndNavGraph(
    navController: NavHostController,
    startDestination: String = "home"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { NavEnterTransition },
        exitTransition = { NavExitTransition },
        popEnterTransition = { NavPopEnterTransition },
        popExitTransition = { NavPopExitTransition }
    ) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("home") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onCreateEvent = { navController.navigate("create") },
                onJoinEvent = { navController.navigate("join") },
                onOpenEvent = { code -> navController.navigate("event/$code") }
            )
        }
        composable("create") {
            CreateEventScreen(
                onEventCreated = { code ->
                    navController.navigate("event/$code") {
                        popUpTo("home")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "join?code={code}",
            arguments = listOf(navArgument("code") {
                type = NavType.StringType
                defaultValue = ""
            }),
            deepLinks = listOf(navDeepLink { uriPattern = "schednd://join?code={code}" })
        ) { backStackEntry ->
            val prefilledCode = backStackEntry.arguments?.getString("code").orEmpty()
            JoinEventScreen(
                prefilledCode = prefilledCode,
                onJoined = { code ->
                    navController.navigate("event/$code") {
                        popUpTo("home")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        navigation(
            startDestination = "event/{code}/home",
            route = "event/{code}",
            arguments = listOf(navArgument("code") { type = NavType.StringType })
        ) {
            composable(
                route = "event/{code}/home",
                // Entrada desde el push del grupo y desde el recordatorio local.
                deepLinks = listOf(navDeepLink { uriPattern = "schednd://event/{code}" })
            ) { backStackEntry ->
                val code = backStackEntry.arguments?.getString("code").orEmpty()
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("event/{code}")
                }
                val eventDetailViewModel: EventDetailViewModel = hiltViewModel(parentEntry)
                SessionShellScreen(
                    eventDetailViewModel = eventDetailViewModel,
                    onLeaveSession = { navController.popBackStack("home", inclusive = false) },
                    onEditAvailability = { navController.navigate("event/$code/edit") }
                )
            }
            composable("event/{code}/edit") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("event/{code}")
                }
                val viewModel: EventDetailViewModel = hiltViewModel(parentEntry)
                EditAvailabilityScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
