package com.schednd.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import com.schednd.R
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.AttendanceTier
import com.schednd.domain.model.DateSummary
import java.time.LocalDate
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.GenTopBar
import com.schednd.ui.components.AvailabilityGrid
import com.schednd.ui.components.getHeatmapColor
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.SquircleMiniShape
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.schednd.domain.model.Participant
import com.schednd.ui.theme.SchedyTheme

@Preview(
    name = "TopBar (Dark)",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF0D0D0D
)
@Composable
private fun PreviewGenTopBarDark() {
    SchedyTheme(darkTheme = true) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            GenTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(
    name = "TopBar (Light)",
    showBackground = true,
    backgroundColor = 0xFFF4F4F6
)
@Composable
private fun PreviewGenTopBarLight() {
    SchedyTheme(darkTheme = false) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            GenTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(name = "Countdown – próxima sesión (Light)", showBackground = true)
@Composable
private fun SessionCountdownPreviewLight() {
    SchedyTheme(darkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            SessionCountdown(
                confirmedDate = LocalDate.now().plusDays(12),
                startTime = java.time.LocalTime.of(20, 30),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Countdown – hoy (Dark)", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SessionCountdownTodayPreviewDark() {
    SchedyTheme(darkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            SessionCountdown(
                confirmedDate = LocalDate.now(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(name = "Detalle – Con participantes (Light)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun EventDetailBodyPreviewLight() {
    SchedyTheme(darkTheme = false) {
        val today = LocalDate.now()
        val dates = listOf(today.plusDays(3), today.plusDays(7), today.plusDays(14), today.plusDays(21))
        val participants = listOf(
            Participant(userId = "u1", name = "Pizpireto", availableDates = emptyList()),
            Participant(userId = "u2", name = "Gandalf", availableDates = emptyList()),
            Participant(userId = "u3", name = "Legolas", availableDates = emptyList()),
        )
        val participantAvailability = mapOf(
            "u1" to setOf(dates[0], dates[1]),
            "u2" to setOf(dates[1], dates[2], dates[3]),
            "u3" to setOf(dates[0], dates[1], dates[2]),
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SessionCountdown(
                    confirmedDate = today.plusDays(7),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)
                )
                com.schednd.ui.components.AvailabilityGrid(
                    dates = dates,
                    participants = participants,
                    participantAvailability = participantAvailability,
                )
                Spacer(modifier = Modifier.height(24.dp))
                GenCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Codigo de la sesión",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ABC123",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Filled.ContentCopy,
                            "Copiar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            GenTopBar(
                title = "Partida de D&D: El Resurgir",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

@Preview(name = "Detalle – Sin participantes (Dark)", showBackground = true, device = "spec:width=411dp,height=891dp", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EventDetailEmptyPreviewDark() {
    SchedyTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 80.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                GenCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Aun no hay participantes. Comparte el codigo para que se unan.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                GenCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Codigo de la sesión",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "ABC123",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 4.sp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Filled.ContentCopy,
                            "Copiar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            GenTopBar(
                title = "One-Shot Halloween",
                hazeState = remember { HazeState() },
                onBack = {},
                onTrailingClick = {}
            )
        }
    }
}

// ── MOCK SESIÓN 23 ────────────────────────────────────────────────────────────

@Preview(
    name = "Mock – Sesión 23 (Dark)",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
    backgroundColor = 0xFF0D0D0F
)
@Composable
private fun MockSession23Dark() {
    SchedyTheme(darkTheme = true) {
        Session23MockContent(darkTheme = true)
    }
}

@Preview(
    name = "Mock – Sesión 23 (Light)",
    showBackground = true,
    device = "spec:width=411dp,height=891dp",
    backgroundColor = 0xFFF4F4F6
)
@Composable
private fun MockSession23Light() {
    SchedyTheme(darkTheme = false) {
        Session23MockContent(darkTheme = false)
    }
}

@Composable
private fun Session23MockContent(darkTheme: Boolean) {
    val today = LocalDate.now()
    val confirmedDate = today.plusDays(5)

    val d1 = today.plusDays(5)
    val d2 = today.plusDays(12)
    val d3 = today.plusDays(19)
    val d4 = today.plusDays(26)
    val dates = listOf(d1, d2, d3, d4)

    val participants = listOf(
        Participant(userId = "u1", name = "Kira",    notes = listOf("Llego tarde el sábado", "Prefiero empezar a las 18h")),
        Participant(userId = "u2", name = "Aldric",  notes = listOf("Traigo snacks")),
        Participant(userId = "u3", name = "Veyra",   notes = emptyList()),
        Participant(userId = "u4", name = "Tormund", notes = listOf("Puedo DM si hace falta")),
        Participant(userId = "u5", name = "Sylwen",  notes = emptyList()),
    )

    val participantAvailability = mapOf(
        "u1" to setOf(d1, d2, d4),
        "u2" to setOf(d1, d2, d3),
        "u3" to setOf(d1, d3, d4),
        "u4" to setOf(d1, d2, d3, d4),
        "u5" to setOf(d2, d3),
    )

    val dateSummaries = listOf(
        DateSummary(date = d1, count = 4, total = 5, absentNames = listOf("Sylwen"), tier = AttendanceTier.VIABLE),
        DateSummary(date = d2, count = 4, total = 5, absentNames = listOf("Veyra"),  tier = AttendanceTier.VIABLE),
        DateSummary(date = d3, count = 4, total = 5, absentNames = listOf("Kira"),   tier = AttendanceTier.VIABLE),
        DateSummary(date = d4, count = 3, total = 5, absentNames = listOf("Aldric", "Sylwen"), tier = AttendanceTier.LIMITED),
    )

    val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault())
    val confirmedFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.statusBarsPadding().height(72.dp))

            // ── Countdown próxima sesión ──────────────────────────────────────
            SessionCountdown(
                confirmedDate = confirmedDate,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 20.dp)
            )

            // ── Grid de disponibilidad ────────────────────────────────────────
            AvailabilityGrid(
                dates = dates,
                participants = participants,
                participantAvailability = participantAvailability,
            )

            // ── Leyenda ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Disponibilidad:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Baja",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(7) { level ->
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(SquircleMiniShape)
                                .background(getHeatmapColor(level, 6))
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Alta",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Fechas recomendadas ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            GenCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fechas recomendadas",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    dateSummaries.filter {
                        it.tier == AttendanceTier.FULL || it.tier == AttendanceTier.VIABLE
                    }.forEach { s ->
                        val label = if (s.absentNames.isEmpty())
                            "Asistencia completa · ${s.count}/${s.total}"
                        else
                            "Asisten ${s.count}/${s.total} · Falta: ${s.absentNames.joinToString(", ")}"
                        Text(
                            text = "· ${dateFormat.format(s.date)}  –  $label",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Fecha elegida ─────────────────────────────────────────────────
            GenCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A95FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Fecha elegida",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = confirmedFormat.format(confirmedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // ── Código de la sesión ───────────────────────────────────────────
            GenCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Codigo de la sesión",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "SES023",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 4.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(Icons.Filled.ContentCopy, "Copiar", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Filled.Share, "Compartir", tint = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Notas del grupo ───────────────────────────────────────────────
            GenCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Notas del grupo",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    participants.filter { it.notes.isNotEmpty() }.forEachIndexed { pIndex, participant ->
                        if (pIndex > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 8.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        }
                        Text(
                            text = participant.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        participant.notes.forEach { noteText ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = noteText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                // Kira es el usuario actual — muestra botones de edición
                                if (participant.userId == "u1") {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFFD3744),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth(),
                        shape = FullRoundShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
                    ) {
                        Text("Añadir nota")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(20.dp))

            // ── Botones de acción ─────────────────────────────────────────────
            GenActionButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Editar mi disponibilidad", color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(modifier = Modifier.height(12.dp))
            GenActionButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Compartir con el grupo", color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        GenTopBar(
            title = "Sesión 23",
            hazeState = remember { HazeState() },
            onBack = {},
            onTrailingClick = {}
        )
    }
}