package com.schednd.presentation.detail

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.domain.model.DateSummary
import java.time.LocalDate
import java.time.LocalTime
import com.schednd.ui.components.DayDialogIcon
import com.schednd.ui.components.DigitalTimePicker
import com.schednd.ui.components.LiquidGlassState
import com.schednd.ui.components.frostedSurface
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.pressScale
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.res.stringResource
import com.schednd.R

/**
 * Selector de hora de inicio, con reloj digital. Descartarlo confirma el día sin hora.
 */
@Composable
internal fun StartTimePickerDialog(
    initialTime: LocalTime,
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onSkip: () -> Unit,
    onConfirm: (LocalTime) -> Unit
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

    var hour by remember { mutableIntStateOf(initialTime.hour) }
    var minute by remember { mutableIntStateOf(initialTime.minute) }

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
                text = stringResource(R.string.dialog_start_time_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.time_picker_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(18.dp))

            DigitalTimePicker(
                hour = hour,
                minute = minute,
                onHourChange = { hour = it },
                onMinuteChange = { minute = it },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.dialog_start_time_skip),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Button(
                    onClick = { onConfirm(LocalTime.of(hour, minute)) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0082F3),
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = stringResource(R.string.dialog_start_time_confirm),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

/**
 * Remate de fijar fecha: el DM acaba de cerrarla y aquí se le ofrece contárselo al grupo.
 *
 * Existe porque la app no manda push —eso pedía una Cloud Function y plan de pago—, así
 * que el aviso lo lleva el propio DM al chat donde el grupo ya vive. Es manual, pero llega
 * antes que abrir la app por su cuenta.
 */
@Composable
internal fun DateConfirmedDialog(
    whenText: String,
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogShape = RoundedCornerShape(28.dp)
    val tintColor = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
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
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DayDialogIcon()

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.dialog_date_confirmed_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(FullRoundShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 16.dp, vertical = 7.dp)
            ) {
                Text(
                    text = whenText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.dialog_date_confirmed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0082F3),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.detail_share_with_group),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.dialog_date_confirmed_dismiss),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
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
                text = stringResource(R.string.dialog_delete_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.dialog_delete_body),
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
                Text(stringResource(R.string.dialog_delete_confirm), fontWeight = FontWeight.SemiBold)
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
                        text = stringResource(R.string.dialog_options_dm_only),
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
                    text = stringResource(R.string.dialog_options_fix_date),
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
                    text = stringResource(R.string.dialog_options_delete),
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
    val dateFormat = DateTimeFormatter.ofPattern(stringResource(R.string.date_pattern_full_weekday_day_month), Locale.getDefault())
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
            text = stringResource(R.string.dialog_pick_date_title),
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
                    stringResource(R.string.dialog_pick_date_clear),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

    }
    } // Box de cristal
}
