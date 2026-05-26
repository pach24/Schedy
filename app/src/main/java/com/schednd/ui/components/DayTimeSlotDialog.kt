package com.schednd.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.schednd.model.DayTimeSlot
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DayTimeSlotDialog(
    date: LocalDate,
    currentSlot: DayTimeSlot?,
    onSlotSelected: (DayTimeSlot) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.85f,
                    animationSpec = spring(
                        dampingRatio = 0.72f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(tween(200)) + slideInVertically(
                    animationSpec = spring(
                        dampingRatio = 0.78f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) { it / 8 },
                exit = scaleOut(targetScale = 0.9f, animationSpec = tween(160)) +
                        fadeOut(tween(160))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth()
                        .clip(SquircleShape(28.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { }
                        .padding(20.dp)
                ) {
                    DialogHeader(date = date)
                    Spacer(modifier = Modifier.height(16.dp))

                    SlotOptionRow(
                        label = "Solo mañana",
                        icon = Icons.Filled.LightMode,
                        selected = currentSlot == DayTimeSlot.MORNING,
                        onClick = { onSlotSelected(DayTimeSlot.MORNING) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SlotOptionRow(
                        label = "Solo tarde",
                        icon = Icons.Filled.DarkMode,
                        selected = currentSlot == DayTimeSlot.AFTERNOON,
                        onClick = { onSlotSelected(DayTimeSlot.AFTERNOON) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SlotOptionRow(
                        label = "Todo el día",
                        icon = Icons.Filled.Schedule,
                        selected = currentSlot == DayTimeSlot.BOTH,
                        onClick = { onSlotSelected(DayTimeSlot.BOTH) }
                    )

                    if (currentSlot != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        RemoveRow(onClick = onRemove)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogHeader(date: LocalDate) {
    val locale = Locale("es")
    val dayName = date.dayOfWeek
        .getDisplayName(TextStyle.FULL, locale)
        .replaceFirstChar { it.uppercase() }
    val month = date.month.getDisplayName(TextStyle.FULL, locale)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$dayName ${date.dayOfMonth}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$month ${date.year}".replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SlotOptionRow(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }

    val background = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val foreground = if (selected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .pressScale(interactionSource)
            .clip(SquircleShape(16.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null) {
                scope.launch {
                    pulse.animateTo(
                        targetValue = 1.06f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                    pulse.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                    onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = foreground
        )
    }
}

@Composable
private fun RemoveRow(onClick: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(1f) }
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pulse.value
                scaleY = pulse.value
            }
            .pressScale(interactionSource)
            .clip(SquircleShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
            .clickable(interactionSource = interactionSource, indication = null) {
                scope.launch {
                    pulse.animateTo(
                        0.96f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh)
                    )
                    pulse.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                    onClick()
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Quitar del día",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

