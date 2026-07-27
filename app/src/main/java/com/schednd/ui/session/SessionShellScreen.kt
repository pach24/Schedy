package com.schednd.ui.session

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.schednd.ui.detail.EventDetailScreen
import com.schednd.ui.detail.EventDetailViewModel
import com.schednd.ui.session.tabs.CalendarTabScreen

@Composable
fun SessionShellScreen(
    eventDetailViewModel: EventDetailViewModel,
    onLeaveSession: () -> Unit,
    onEditAvailability: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(SessionTab.SESSION) }
    val eventState by eventDetailViewModel.uiState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = Color.Transparent,
        bottomBar = {
            SessionBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    if (tab == SessionTab.SESSION && selectedTab == SessionTab.SESSION) {
                        onLeaveSession()
                    } else {
                        selectedTab = tab
                    }
                }
            )
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = tween(durationMillis = 220),
            label = "shellCrossfade"
        ) { tab ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    SessionTab.SESSION -> EventDetailScreen(
                        viewModel = eventDetailViewModel,
                        bottomPadding = innerPadding,
                        onBack = onLeaveSession,
                        onEditAvailability = onEditAvailability
                    )
                    SessionTab.CALENDAR -> CalendarTabScreen(
                        bottomPadding = innerPadding,
                        sessionName = eventState.event?.name.orEmpty(),
                        dateSummaries = eventState.dateSummaries,
                        totalParticipants = eventState.participants.size,
                        confirmedDate = eventState.confirmedDate,
                        onBack = onLeaveSession
                    )
                }
            }
        }
    }
}
