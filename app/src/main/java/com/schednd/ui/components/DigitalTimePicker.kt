package com.schednd.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.schednd.R
import com.schednd.ui.theme.SquircleShape
import com.schednd.ui.theme.pressScale
import kotlinx.coroutines.delay

/**
 * Reloj digital de hora y minutos. Cada cifra sale por un lado y entra por el otro según
 * la dirección del cambio, así subir y bajar se distinguen sin llegar a leer el número.
 *
 * Se maneja arrastrando las cifras (una unidad por tramo) o con las flechas, que saltan
 * al múltiplo siguiente: cuadrar una sesión son horas en punto y medias, no minutos sueltos.
 */
@Composable
fun DigitalTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF0082F3)
) {
    val digitStyle = MaterialTheme.typography.headlineLarge.copy(
        fontSize = 56.sp,
        lineHeight = 62.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center
    )
    // Golos no es monoespaciada: sin un ancho fijo por cifra, pasar de 1 a 8 movería
    // todo el reloj de sitio. Medimos una vez y reservamos ese hueco para cada dígito.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitWidth = remember(measurer, digitStyle, density) {
        with(density) { measurer.measure("8", digitStyle).size.width.toDp() + 2.dp }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        JuicyEntrance(delayMillis = 0) {
            TimeUnitStepper(
                value = hour,
                unitCount = 24,
                buttonStep = 1,
                dragStep = 26.dp,
                digitStyle = digitStyle,
                digitWidth = digitWidth,
                accent = accent,
                increaseDescription = stringResource(R.string.time_picker_hour_up),
                decreaseDescription = stringResource(R.string.time_picker_hour_down),
                onChange = onHourChange
            )
        }
        JuicyEntrance(delayMillis = 70) {
            ClockColon(style = digitStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        JuicyEntrance(delayMillis = 140) {
            TimeUnitStepper(
                value = minute,
                unitCount = 60,
                buttonStep = 5,
                dragStep = 14.dp,
                digitStyle = digitStyle,
                digitWidth = digitWidth,
                accent = accent,
                increaseDescription = stringResource(R.string.time_picker_minute_up),
                decreaseDescription = stringResource(R.string.time_picker_minute_down),
                onChange = onMinuteChange
            )
        }
    }
}

/** Una cifra de dos dígitos con sus flechas: la unidad mínima del reloj. */
@Composable
private fun TimeUnitStepper(
    value: Int,
    unitCount: Int,
    buttonStep: Int,
    dragStep: Dp,
    digitStyle: TextStyle,
    digitWidth: Dp,
    accent: Color,
    increaseDescription: String,
    decreaseDescription: String,
    onChange: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    // El gesto vive más que la composición que lo creó: sin esto leería el valor
    // congelado en el primer arrastre y cada tramo repetiría el mismo salto.
    val currentValue by rememberUpdatedState(value)
    val currentOnChange by rememberUpdatedState(onChange)

    var goingUp by remember { mutableStateOf(true) }
    var dragging by remember { mutableStateOf(false) }

    fun step(delta: Int, snapToStep: Boolean) {
        val next = if (snapToStep && buttonStep > 1) {
            val remainder = Math.floorMod(currentValue, buttonStep)
            if (delta > 0) {
                Math.floorMod(currentValue - remainder + buttonStep, unitCount)
            } else {
                Math.floorMod(currentValue - if (remainder == 0) buttonStep else remainder, unitCount)
            }
        } else {
            Math.floorMod(currentValue + delta, unitCount)
        }
        if (next == currentValue) return
        goingUp = delta > 0
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        currentOnChange(next)
    }

    val stepPx = with(LocalDensity.current) { dragStep.toPx() }
    var accumulated by remember { mutableFloatStateOf(0f) }

    // Rebote corto en cada cambio: el número no solo aparece, aterriza.
    val pop = remember { Animatable(1f) }
    var isFirstValue by remember { mutableStateOf(true) }
    LaunchedEffect(value) {
        if (isFirstValue) {
            isFirstValue = false
            return@LaunchedEffect
        }
        pop.animateTo(1.08f, spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessHigh))
        pop.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium))
    }
    val dragScale by animateFloatAsState(
        targetValue = if (dragging) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "dragScale"
    )
    val pillColor by animateColorAsState(
        targetValue = if (dragging) accent.copy(alpha = 0.18f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        animationSpec = tween(220),
        label = "pillColor"
    )
    val digitColor by animateColorAsState(
        targetValue = if (dragging) accent else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(220),
        label = "digitColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        StepChevron(
            icon = Icons.Rounded.KeyboardArrowUp,
            description = increaseDescription,
            onStep = { step(1, snapToStep = true) }
        )
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = pop.value * dragScale
                    scaleY = pop.value * dragScale
                }
                // El recorte va antes del relleno para que las cifras se asomen y
                // desaparezcan por el borde de la pastilla, como una ventanilla.
                .clip(SquircleShape(22.dp))
                .background(pillColor)
                // Arrastre que emite pasos completos: acumula el recorrido y descuenta un
                // tramo cada vez que se llena, así el dedo y las cifras van al mismo ritmo.
                .pointerInput(stepPx) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            accumulated = 0f
                            dragging = true
                        },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false }
                    ) { change, dragAmount ->
                        change.consume()
                        // Arrastrar hacia arriba sube la cifra: el número sigue al dedo.
                        accumulated -= dragAmount
                        while (accumulated >= stepPx) {
                            accumulated -= stepPx
                            step(1, snapToStep = false)
                        }
                        while (accumulated <= -stepPx) {
                            accumulated += stepPx
                            step(-1, snapToStep = false)
                        }
                    }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            RollingNumber(
                value = value,
                goingUp = goingUp,
                style = digitStyle,
                color = digitColor,
                digitWidth = digitWidth
            )
        }
        StepChevron(
            icon = Icons.Rounded.KeyboardArrowDown,
            description = decreaseDescription,
            onStep = { step(-1, snapToStep = true) }
        )
    }
}

/** Dos dígitos con relleno de cero, cada uno con su propia rueda. */
@Composable
private fun RollingNumber(
    value: Int,
    goingUp: Boolean,
    style: TextStyle,
    color: Color,
    digitWidth: Dp
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RollingDigit(value / 10, goingUp, style, color, digitWidth)
        RollingDigit(value % 10, goingUp, style, color, digitWidth)
    }
}

@Composable
private fun RollingDigit(
    digit: Int,
    goingUp: Boolean,
    style: TextStyle,
    color: Color,
    digitWidth: Dp
) {
    AnimatedContent(
        targetState = digit,
        transitionSpec = {
            val direction = if (goingUp) 1 else -1
            val enter = slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.62f,
                    stiffness = Spring.StiffnessMedium
                )
            ) { height -> direction * height } +
                    fadeIn(tween(160)) +
                    scaleIn(
                        initialScale = 0.55f,
                        animationSpec = spring(
                            dampingRatio = 0.5f,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            val exit = slideOutVertically(
                animationSpec = tween(200)
            ) { height -> -direction * height } +
                    fadeOut(tween(150)) +
                    scaleOut(targetScale = 0.55f, animationSpec = tween(200))
            // Sin SizeTransform el hueco se animaría en cada cambio; aquí todas las
            // cifras miden lo mismo, así que el contenedor no debe moverse.
            enter togetherWith exit using SizeTransform(clip = false)
        },
        label = "rollingDigit"
    ) { target ->
        Text(
            text = target.toString(),
            style = style,
            color = color,
            modifier = Modifier.width(digitWidth)
        )
    }
}

/** Los dos puntos parpadean como los de un despertador: marcan que el reloj está vivo. */
@Composable
private fun ClockColon(style: TextStyle, color: Color) {
    val transition = rememberInfiniteTransition(label = "colon")
    val alpha = transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colonAlpha"
    )
    Text(
        text = ":",
        style = style,
        color = color,
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .graphicsLayer { this.alpha = alpha.value }
    )
}

/** Flecha de paso: un toque salta una vez, mantenerla pulsada acelera. */
@Composable
private fun StepChevron(
    icon: ImageVector,
    description: String,
    onStep: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val currentOnStep by rememberUpdatedState(onStep)
    // El toque suelto salta una vez al levantar el dedo. Si la pulsación ya ha entrado en
    // repetición, ese último clic sobra: la marca lo descuenta.
    var repeating by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (!isPressed) return@LaunchedEffect
        repeating = false
        delay(420)
        repeating = true
        var interval = 150L
        while (true) {
            currentOnStep()
            delay(interval)
            interval = (interval * 82 / 100).coerceAtLeast(45L)
        }
    }

    Box(
        modifier = Modifier
            .pressScale(interactionSource)
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                if (repeating) repeating = false else onStep()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

/** Entrada con rebote: sube, se pasa de tamaño y se asienta. */
@Composable
private fun JuicyEntrance(
    delayMillis: Int,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow)
        )
    }
    Box(
        modifier = Modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            scaleX = 0.6f + 0.4f * p
            scaleY = 0.6f + 0.4f * p
            translationY = (1f - p) * 34.dp.toPx()
        }
    ) {
        content()
    }
}
