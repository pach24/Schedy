package com.schednd.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.ContentTransform
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntSize
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

// ── Salida de una fila que se quita de un listado ────────────────────

/**
 * Cuánto tarda [RowRemovalExit] de principio a fin. Lo necesita quien dispare el borrado
 * de verdad: si los datos cambian antes, la fila desaparece a media caída.
 */
const val RowRemovalDurationMs = 300

/**
 * La fila se cae y se desvanece, y el hueco se cierra tras ella.
 *
 * La caída acelera —`FastOutLinearInEasing`, que sale lento y termina rápido— para que
 * parezca que se suelta, no que se desliza. El encogido entra algo más tarde: si el hueco
 * se cerrara a la vez, la caída no se llegaría a ver.
 */
val RowRemovalExit: ExitTransition =
    slideOutVertically(
        animationSpec = tween(RowRemovalDurationMs, easing = FastOutLinearInEasing)
    ) { it } +
        scaleOut(
            targetScale = 0.92f,
            animationSpec = tween(RowRemovalDurationMs)
        ) +
        fadeOut(tween(RowRemovalDurationMs - 80)) +
        shrinkVertically(
            animationSpec = tween(RowRemovalDurationMs - 60, delayMillis = 60),
            shrinkTowards = Alignment.Top
        )

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

// ── De hueco a dato ──────────────────────────────────────────────────

/**
 * El hueco se convierte en el dato: lo que había se deshace hacia fuera mientras lo que
 * llega entra creciendo, y la caja los acompaña estirándose de un alto al otro.
 *
 * Ese estirón es lo que lo hace parecer una pieza que cambia de forma y no dos pantallas
 * intercambiadas, así que el hueco conviene dibujarlo con la silueta de lo que va a caer
 * ahí: cuanto menos tenga que recorrer la caja, más se lee como una sola cosa.
 *
 * La salida es más corta que la entrada y esta arranca con retardo: se solapan lo justo
 * para que no haya un fotograma con las dos cosas encima, que es lo que ensucia el cambio.
 */
@Composable
fun <T> SkeletonMorph(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedContentScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        transitionSpec = {
            val enter = fadeIn(tween(260, delayMillis = 120)) +
                    scaleIn(
                        initialScale = 0.94f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
            val exit = fadeOut(tween(160)) +
                    scaleOut(targetScale = 1.04f, animationSpec = tween(220))
            ContentTransform(
                targetContentEnter = enter,
                initialContentExit = exit,
                // `clip = false` para que el rebote del muelle pueda asomar del alto final
                // en vez de quedarse cortado justo cuando se le va la mano, que es la gracia.
                sizeTransform = SizeTransform(clip = false) { _, _ -> MorphSizeSpec }
            )
        },
        // Anclado arriba: la caja crece hacia abajo, hacia el hueco libre, en vez de
        // repartir el estirón y llevarse por delante lo que tiene encima.
        contentAlignment = Alignment.TopStart,
        label = "SkeletonMorph",
        content = content
    )
}

private val MorphSizeSpec: FiniteAnimationSpec<IntSize> = spring(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow,
    // Por debajo de un píxel no hay nada que enseñar; sin este umbral el muelle se queda
    // rebotando en decimales de píxel mucho después de que el cambio se haya visto.
    visibilityThreshold = IntSize(1, 1)
)

/**
 * Lo mismo que hace [SkeletonMorph] con lo que sale, para huecos que no pueden vivir dentro
 * de él: las filas de una lista perezosa tienen que ser hijas directas de la lista para
 * animarse una a una, así que el hueco se queda al lado y se deshace por su cuenta.
 *
 * Encoge además de irse, y por eso arrastra consigo lo que tenga debajo: lo que llega sube
 * a ocupar su sitio en el mismo movimiento en vez de esperar a que termine de marcharse.
 */
val SkeletonDissolve: ExitTransition =
    fadeOut(tween(180)) +
        scaleOut(targetScale = 1.04f, animationSpec = tween(240)) +
        shrinkVertically(
            animationSpec = tween(300, delayMillis = 40),
            shrinkTowards = Alignment.Top
        )

/**
 * Con lo que entra a ocupar ese sitio. Va con retardo para no cruzarse con la salida: en el
 * fotograma en que se solapan las dos cosas es donde el cambio se ve sucio.
 */
val SkeletonReveal: FiniteAnimationSpec<Float> = tween(320, delayMillis = 90)

/**
 * Sale rápido y se pasa un pelo de largo antes de asentarse. Es la curva del muelle, pero
 * en `tween`, que es lo único que admite retardo: sin él no hay escalonado que valga.
 */
private val RevealEasing = CubicBezierEasing(0.16f, 1.15f, 0.32f, 1f)

/**
 * Entrada escalonada de los hijos de un [SkeletonMorph]. Se usa con `animateEnterExit`
 * dentro de su contenido: cada fila llega un poco después que la anterior, de arriba abajo,
 * en vez de aparecer el bloque entero de una pieza.
 */
fun staggeredReveal(index: Int, stepMs: Int = 55): EnterTransition {
    val delay = 120 + index * stepMs
    return fadeIn(tween(240, delayMillis = delay)) +
            slideInVertically(
                animationSpec = tween(
                    durationMillis = 380,
                    delayMillis = delay,
                    easing = RevealEasing
                )
            ) { (it * 0.4f).toInt() }
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
