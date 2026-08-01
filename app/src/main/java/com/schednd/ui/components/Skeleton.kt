package com.schednd.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.schednd.ui.theme.SquircleShape

/** Redondeo de un hueco suelto: una barra de texto, no una tarjeta. */
val SkeletonShape = SquircleShape(6.dp)

/** Lo que tarda el reflejo en cruzar la pantalla. */
private const val SweepMs = 1150

/**
 * Descanso entre pasadas. Sin él el reflejo se lee como una cinta girando sin parar; con él
 * respira, que es lo que hace que la espera no se note larga.
 */
private const val PauseMs = 380

/**
 * Lo que el reflejo tiene que sobrevivir a la llegada de los datos: lo que tarda el hueco
 * más lento en deshacerse. Ver el parámetro `active` de [SkeletonSweep].
 */
const val SkeletonSweepOutroMs = 400L

private val LocalSkeletonSweep = compositionLocalOf<State<Float>?> { null }

/**
 * Reparte una única pasada de luz entre todos los huecos que envuelva.
 *
 * Va compartida a propósito: cada hueco animando por su cuenta arranca cuando le toca
 * componerse, y basta con que dos vayan desfasados para que la pantalla parezca un montón
 * de piezas parpadeando en vez de una superficie con un reflejo pasando por encima.
 *
 * @param active mientras haya algo que iluminar. Un `rememberInfiniteTransition` pide
 *   fotograma tras fotograma mientras esté compuesto, aunque nadie lea su valor, así que
 *   una pantalla ya cargada que lo deje encendido no llega a quedarse quieta nunca. Conviene
 *   apagarlo con algo de retraso: mientras el hueco se deshace todavía se le ve.
 */
@Composable
fun SkeletonSweep(active: Boolean = true, content: @Composable () -> Unit) {
    val sweep = if (active) rememberSkeletonSweep() else null
    CompositionLocalProvider(LocalSkeletonSweep provides sweep, content = content)
}

@Composable
private fun rememberSkeletonSweep(): State<Float> {
    val transition = rememberInfiniteTransition(label = "skeleton")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = SweepMs + PauseMs
                0f at 0 using LinearEasing
                1f at SweepMs
                1f at SweepMs + PauseMs
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "skeletonSweep"
    )
}

/**
 * Pinta el hueco de un dato que aún no ha llegado, con el reflejo pasándole por encima.
 *
 * El degradado no se ancla a la pieza sino a la pantalla: cada hueco se pregunta dónde
 * está y dibuja el trozo de reflejo que le corresponde. Anclado a la pieza, un hueco
 * estrecho vería la pasada entera en el mismo tiempo que uno ancho y se encendería solo,
 * fuera de compás. Así la luz cruza por delante de todos como si fueran una sola cosa.
 *
 * La pasada baja un poco al avanzar, de modo que lo de abajo se enciende algo después que
 * lo de arriba.
 */
@Composable
fun Modifier.skeletonSurface(shape: Shape = SkeletonShape): Modifier {
    // Sin [SkeletonSweep] alrededor el hueco se anima igual, por su cuenta: es preferible a
    // dejarlo apagado y que parezca contenido de verdad, pintado en gris.
    val sweep = LocalSkeletonSweep.current ?: rememberSkeletonSweep()

    // En oscuro el reflejo aclara el hueco, como una luz que le pasa por delante. En claro
    // no puede aclararlo más que la tarjeta que lo sostiene, así que lo que hace la cresta
    // es lo contrario: soltar el velo y dejar asomar la tarjeta. Se lee igual de luminoso.
    val isDark = isSystemInDarkTheme()
    val tint = MaterialTheme.colorScheme.onSurface
    val trough = tint.copy(alpha = if (isDark) 0.09f else 0.13f)
    val crest = tint.copy(alpha = if (isDark) 0.20f else 0.02f)

    val screenWidth = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    var origin by remember { mutableStateOf(Offset.Zero) }

    return this
        .clip(shape)
        .onGloballyPositioned { origin = it.positionInRoot() }
        .drawWithCache {
            val band = screenWidth * 0.5f
            val brush = Brush.linearGradient(
                0f to trough,
                0.5f to crest,
                1f to trough,
                start = Offset.Zero,
                end = Offset(band, band * 0.15f)
            )
            // La pasada entra por fuera del borde izquierdo y sale por fuera del derecho:
            // así ningún hueco arranca ya encendido.
            val travel = screenWidth + band * 2f
            onDrawBehind {
                // Mover el lienzo y devolver el rectángulo a su sitio deja el degradado
                // desplazado sin tener que rehacerlo en cada fotograma.
                val dx = sweep.value * travel - band - origin.x
                val dy = -origin.y
                translate(dx, dy) {
                    drawRect(brush = brush, topLeft = Offset(-dx, -dy), size = size)
                }
            }
        }
}

/** Hueco suelto. El tamaño lo pone quien lo usa, que es quien sabe qué dato va a caer ahí. */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = SkeletonShape
) {
    Box(modifier = modifier.skeletonSurface(shape))
}
