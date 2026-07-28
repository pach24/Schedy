package com.schednd.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.border
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import dev.chrisbanes.haze.HazeState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schednd.domain.model.DateSummary
import java.time.LocalDate
import java.time.LocalTime
import com.schednd.ui.components.LiquidGlassState
import com.schednd.ui.components.frostedSurface
import com.schednd.ui.theme.pressScale
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Selector de hora de inicio. Descartarlo confirma el día sin hora. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartTimePickerDialog(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿A qué hora empezáis?") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text("Fijar hora")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Solo el día") }
        }
    )
}

@Composable
internal fun DeleteSessionDialog(
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onConfirm: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)

    val tintColor = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.82f)
    else
        Color.White.copy(alpha = 0.82f)
    // Más velo que en la top bar: aquí hay texto que leer sobre el cristal.
    val glassTint = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.62f)
    else
        Color.White.copy(alpha = 0.68f)

    val borderBrush = if (isDark) {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
        )
    } else {
        Brush.verticalGradient(
            listOf(Color.White, Color.Transparent)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .frostedSurface(
                glass = glass,
                hazeState = hazeState,
                cornerRadius = 28.dp,
                hazeBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
                hazeTint = tintColor,
                glassTint = glassTint,
            )
            .border(1.dp, borderBrush, dialogShape)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Borrar sesion",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Seguro que quieres borrar esta sesion? Esta accion no se puede deshacer.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    contentColor = Color(0xFFFD3744)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text("Borrar sesion", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


@Composable
internal fun MoreOptionsDialog(
    hazeState: HazeState,
    glass: LiquidGlassState?,
    isCreator: Boolean,
    onFixDate: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
    // Más velo que en la top bar: aquí hay texto que leer sobre el cristal.
    val glassTint = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.62f)
    else
        Color.White.copy(alpha = 0.68f)
    val borderBrush = if (isDark)
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent))
    else
        Brush.verticalGradient(listOf(Color.White, Color.Transparent))

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .frostedSurface(
                glass = glass,
                hazeState = hazeState,
                cornerRadius = 28.dp,
                hazeBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
                hazeTint = tintColor,
                glassTint = glassTint,
            )
            .border(1.dp, borderBrush, dialogShape)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            // Fijar fecha y borrar son acciones del DM. Las reglas de Firestore
            // rechazan ambas para el resto, así que la UI no las ofrece.
            if (!isCreator) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Solo el DM puede fijar la fecha",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isCreator) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onFixDate
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Fijar fecha y hora",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDelete
                    )
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFD3744),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Borrar sesión",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFFD3744)
                )
            }
            } // if (isCreator)
        }
    }
}

@Composable
internal fun ConfirmDateDialog(
    dateSummaries: List<DateSummary>,
    currentConfirmedDate: LocalDate?,
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onDateSelected: (LocalDate) -> Unit,
    onClearDate: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dateFormat = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
    val sorted = remember(dateSummaries) { dateSummaries.sortedByDescending { it.count } }
    val dialogShape = RoundedCornerShape(28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.55f) else Color.White.copy(alpha = 0.55f)
    // Más velo que en la top bar: aquí hay texto que leer sobre el cristal.
    val glassTint = if (isDark)
        Color(0xFF1C1C1E).copy(alpha = 0.62f)
    else
        Color.White.copy(alpha = 0.68f)
    val topBorderColor = if (isDark) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.85f)
    val innerBorderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.18f else 0.9f),
            Color.Transparent
        )
    )
    // Cristal sobre cristal no tiene sentido físico: la lista interior muestrearía el
    // mismo fondo que el diálogo, ignorando que este está delante. Va con relleno plano.
    val innerFill = if (isDark) Color(0xFF27272A) else Color(0xFFF0F0F2)

    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .wrapContentHeight()
            .clip(dialogShape)
            .frostedSurface(
                glass = glass,
                hazeState = hazeState,
                cornerRadius = 28.dp,
                hazeBackground = if (isDark) Color(0xFF1C1C1E) else Color.White,
                hazeTint = tintColor,
                glassTint = glassTint,
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(topBorderColor, Color.Transparent)
                ),
                shape = dialogShape
            )
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Elige la fecha",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(innerFill.copy(alpha = 0.65f))
                .border(1.dp, innerBorderBrush, RoundedCornerShape(16.dp))
        ) {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                itemsIndexed(sorted) { index, summary ->
                    val isConfirmed = summary.date == currentConfirmedDate
                    val interaction = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(interaction)
                            .clickable(
                                interactionSource = interaction,
                                indication = LocalIndication.current
                            ) { onDateSelected(summary.date) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isConfirmed) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF0082F3),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = dateFormat.format(summary.date)
                                    .replaceFirstChar { it.uppercaseChar() },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isConfirmed) Color(0xFF0082F3)
                                        else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isConfirmed) FontWeight.SemiBold
                                             else FontWeight.Normal
                            )
                        }
                        Text(
                            text = "${summary.count}/${summary.total}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < sorted.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }

        if (currentConfirmedDate != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClearDate,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    contentColor = Color(0xFFFD3744)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    "Quitar fecha elegida",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

    }
    } // Box de cristal
}
