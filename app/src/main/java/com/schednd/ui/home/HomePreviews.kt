package com.schednd.ui.home

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.schednd.ui.theme.SchedyTheme
import java.time.LocalDate

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(name = "Home – Vacío (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun HomePreviewEmpty() {
    SchedyTheme(darkTheme = false) {
        HomeContent(
            uiState = HomeUiState(isAuthReady = true),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}

@Preview(name = "Home – Con sesiones (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun HomePreviewLight() {
    SchedyTheme(darkTheme = false) {
        val today = LocalDate.now()
        val sample = listOf(
            HomeSessionCard(
                code = "ABC123",
                name = "D&D semanal",
                confirmedDate = today.plusDays(6),
                participantsCount = 5,
                totalParticipants = 5,
                participantInitials = listOf("A", "M", "D", "P", "O")
            ),
            HomeSessionCard(
                code = "VAM999",
                name = "Vampiro",
                confirmedDate = today.plusDays(20),
                participantsCount = 4,
                totalParticipants = 4,
                participantInitials = listOf("J", "L", "K", "T")
            ),
            HomeSessionCard(
                code = "HAL666",
                name = "One-Shot Halloween",
                confirmedDate = null,
                participantsCount = 0,
                totalParticipants = 0,
                participantInitials = emptyList()
            )
        )
        HomeContent(
            uiState = HomeUiState(
                isAuthReady = true,
                allSessions = sample,
                upcomingSessions = sample,
                nextSession = sample.first()
            ),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}

@Preview(name = "Home – Con sesiones (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomePreviewDark() {
    SchedyTheme(darkTheme = true) {
        val today = LocalDate.now()
        val sample = listOf(
            HomeSessionCard(
                code = "ABC123",
                name = "D&D semanal",
                confirmedDate = today.plusDays(6),
                participantsCount = 5,
                totalParticipants = 5,
                participantInitials = listOf("A", "M", "D", "P", "O")
            )
        )
        HomeContent(
            uiState = HomeUiState(
                isAuthReady = true,
                allSessions = sample,
                upcomingSessions = sample,
                nextSession = sample.first()
            ),
            onCreateEvent = {},
            onJoinEvent = {},
            onOpenEvent = {}
        )
    }
}
