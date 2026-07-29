package com.schednd.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch

// ── Transiciones de navegación ───────────────────────────────────────
// Forward: new screen slides up from bottom, old screen scales down + dims
// Back:    current screen slides down, background screen scales back up

val NavEnterTransition: EnterTransition =
    slideInVertically(
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessLow
        )
    ) { it } + fadeIn(tween(300))

val NavExitTransition: ExitTransition =
    scaleOut(
        targetScale = 0.92f,
        animationSpec = tween(400)
    ) + fadeOut(tween(350, delayMillis = 50), targetAlpha = 0.4f)

val NavPopEnterTransition: EnterTransition =
    scaleIn(
        initialScale = 0.92f,
        animationSpec = tween(350)
    ) + fadeIn(tween(300), initialAlpha = 0.4f)

val NavPopExitTransition: ExitTransition =
    slideOutVertically(
        animationSpec = spring(
            dampingRatio = 0.86f,
            stiffness = Spring.StiffnessLow
        )
    ) { it } + fadeOut(tween(280))

// ── Phase transition specs (internal screen transitions) ─────────────
// Slide up from bottom + fade, para presentaciones tipo hoja

val PhaseEnterTransition: EnterTransition =
    slideInVertically(
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessLow
        )
    ) { (it * 0.3f).toInt() } + fadeIn(tween(350))

val PhaseExitTransition: ExitTransition =
    slideOutVertically(
        animationSpec = tween(280)
    ) { -(it * 0.15f).toInt() } + fadeOut(tween(250))

/**
 * Modifier that scales down with spring physics on press.
 * Escala de pulsación sutil y responsiva.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }

    LaunchedEffect(isPressed) {
        scale.animateTo(
            targetValue = if (isPressed) 0.965f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    return this.graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

/**
 * Composable wrapper — fade + spring slide-up on first appearance.
 * Revelado escalonado del contenido.
 */
@Composable
fun FadeIn(
    delayMs: Int = 0,
    durationMs: Int = 450,
    offsetY: Float = 30f,
    content: @Composable () -> Unit
) {
    // La entrada escalonada es para la primera vez que se ve el hueco, no para cada ida y
    // vuelta: dentro de un host de pestañas la anterior se descarta al cambiar, y sin esta
    // marca el contenido volvería a aparecer desde cero —con su retardo— en cada toque de
    // la barra. `rememberSaveable` es lo que la hace sobrevivir a ese descarte, siempre que
    // el hueco esté bajo un `SaveableStateHolder`.
    val played = rememberSaveable { mutableStateOf(false) }
    val skipEntrance = remember { played.value }

    val alpha = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val translationY = remember { Animatable(if (skipEntrance) 0f else offsetY) }

    LaunchedEffect(Unit) {
        if (skipEntrance) return@LaunchedEffect
        played.value = true
        kotlinx.coroutines.delay(delayMs.toLong())
        launch { alpha.animateTo(1f, tween(durationMs)) }
        translationY.animateTo(
            0f,
            spring(
                dampingRatio = 0.72f,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            this.translationY = translationY.value
        }
    ) {
        content()
    }
}

/**
 * Column with staggered child appearance.
 */
@Composable
fun StaggeredColumn(
    modifier: Modifier = Modifier,
    staggerDelayMs: Int = 60,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        content()
    }
}

/**
 * Crossfade con escala — transiciones de carga.
 * Content scales up slightly from 0.96 as it fades in.
 */
@Composable
fun <T> CrossfadeLoadingContent(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            (scaleIn(
                initialScale = 0.96f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(350))) togetherWith
                    (scaleOut(
                        targetScale = 0.96f,
                        animationSpec = tween(250)
                    ) + fadeOut(tween(250)))
        },
        label = "CrossfadeLoadingContent"
    ) { state ->
        content(state)
    }
}
