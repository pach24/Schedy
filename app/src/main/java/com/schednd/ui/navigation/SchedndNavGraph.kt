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
import com.schednd.ui.notes.NoteEditorScreen
import com.schednd.ui.notes.NoteEditorViewModel
import com.schednd.ui.notes.NotesViewModel
import com.schednd.ui.onboarding.OnboardingScreen
import com.schednd.ui.session.SessionShellScreen
import com.schednd.ui.theme.NavEnterTransition
import com.schednd.ui.theme.NavExitTransition
import com.schednd.ui.theme.NavPopEnterTransition
import com.schednd.ui.theme.NavPopExitTransition
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

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
            composable("event/{code}/home") { backStackEntry ->
                val code = backStackEntry.arguments?.getString("code").orEmpty()
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("event/{code}")
                }
                val eventDetailViewModel: EventDetailViewModel = hiltViewModel(parentEntry)
                val notesViewModel: NotesViewModel = hiltViewModel(parentEntry)
                SessionShellScreen(
                    eventDetailViewModel = eventDetailViewModel,
                    notesViewModel = notesViewModel,
                    onLeaveSession = { navController.popBackStack("home", inclusive = false) },
                    onEditAvailability = { navController.navigate("event/$code/edit") },
                    onNewNote = { template ->
                        if (template != null) {
                            val tSeed = encode(template.titleSeed)
                            val bSeed = encode(template.bodySeed)
                            val tagSeed = template.tag.name
                            navController.navigate("event/$code/notes/new?titleSeed=$tSeed&bodySeed=$bSeed&tagSeed=$tagSeed")
                        } else {
                            navController.navigate("event/$code/notes/new")
                        }
                    },
                    onEditNote = { noteId ->
                        navController.navigate("event/$code/notes/edit/$noteId")
                    }
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
            composable(
                route = "event/{code}/notes/new?titleSeed={titleSeed}&bodySeed={bodySeed}&tagSeed={tagSeed}",
                arguments = listOf(
                    navArgument("titleSeed") { type = NavType.StringType; defaultValue = "" },
                    navArgument("bodySeed") { type = NavType.StringType; defaultValue = "" },
                    navArgument("tagSeed") { type = NavType.StringType; defaultValue = "" }
                )
            ) {
                val vm: NoteEditorViewModel = hiltViewModel()
                NoteEditorScreen(
                    viewModel = vm,
                    onClose = { navController.popBackStack() }
                )
            }
            composable(
                route = "event/{code}/notes/edit/{noteId}",
                arguments = listOf(navArgument("noteId") { type = NavType.StringType })
            ) {
                val vm: NoteEditorViewModel = hiltViewModel()
                NoteEditorScreen(
                    viewModel = vm,
                    onClose = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun encode(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8.name())
