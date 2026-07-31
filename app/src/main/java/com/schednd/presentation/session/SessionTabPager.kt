package com.schednd.presentation.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min

@Composable
internal fun rememberTabNavigator(pagerState: PagerState): TabNavigator {
    val scope = rememberCoroutineScope()
    return remember(pagerState, scope) { TabNavigator(pagerState, scope) }
}

/**
 * Mueve el contenido y la bolita de la barra, que ya no van pegados.
 *
 * **Al deslizar** la bolita lee el pager y va exactamente donde va el dedo, que es lo que
 * hace que el salto se sienta agarrado.
 *
 * **Al tocar una pestaña** se despega y hace un único viaje a su propia velocidad. Antes
 * compartían reloj: tocar la última pestaña se veía como la bolita cayendo en todas las
 * muescas del camino a toda prisa, porque el mismo muelle tenía que cubrir tres pestañas
 * en el tiempo de una.
 */
@Stable
internal class TabNavigator(
    private val pagerState: PagerState,
    private val scope: CoroutineScope
) {
    private val ball = Animatable(pagerState.currentPage.toFloat())
    private var travelling by mutableStateOf(false)
    private var travelFrom by mutableFloatStateOf(0f)
    private var travelTo by mutableFloatStateOf(0f)
    private var job: Job? = null

    /** Posición continua de la bolita, medida en pestañas: 0 la primera, 1.5 entre dos. */
    fun position(): Float =
        if (travelling) ball.value
        else pagerState.currentPage + pagerState.currentPageOffsetFraction

    /**
     * Cuánto lleva de salto: 0 posada en una muesca, 1 en lo alto del arco.
     *
     * Se mide contra el viaje en curso, no contra la muesca más cercana. Deslizando el
     * viaje es siempre de una pestaña a la de al lado y sale la misma cuenta de siempre;
     * tocando puede ser de tres, y entonces es **un solo arco** de punta a punta en vez de
     * uno por pestaña cruzada.
     */
    fun hop(): Float {
        val pos = position()
        val from = if (travelling) travelFrom else floor(pos)
        val to = if (travelling) travelTo else floor(pos) + 1f
        val low = minOf(from, to)
        val high = maxOf(from, to)
        val half = (high - low) / 2f
        if (half <= 0f) return 0f
        return (min(pos - low, high - pos) / half).coerceIn(0f, 1f)
    }

    /** Lleva pestaña y bolita a [page]. Un toque nuevo cancela el viaje anterior. */
    fun goTo(page: Int) {
        val previous = job
        job = scope.launch {
            // Antes de tocar nada: si no, el `finally` del viaje anterior podría apagar
            // el `travelling` que acaba de encender este.
            previous?.cancelAndJoin()

            val from = pagerState.currentPage + pagerState.currentPageOffsetFraction
            if (from == page.toFloat()) return@launch

            ball.snapTo(from)
            travelFrom = from
            travelTo = page.toFloat()
            travelling = true
            try {
                coroutineScope {
                    launch { ball.animateTo(page.toFloat(), BallTravelSpring) }
                    // En el cuerpo y no en otro `launch`: si el dedo interrumpe el scroll,
                    // la excepción sale de aquí y se lleva por delante a la bolita, que
                    // vuelve a engancharse al pager en vez de seguir sola.
                    pagerState.settleOnTabPage(page)
                }
            } finally {
                travelling = false
            }
        }
    }
}

/**
 * Lleva el pager a [page] sin que desfilen las pestañas de en medio: si el destino está a
 * más de una, se planta primero en su vecina y solo anima el último tramo.
 *
 * Es lo que hace `animateScrollToPage` por su cuenta cuando el destino queda lejos, pero su
 * umbral son tres páginas y aquí, con cuatro pestañas, no se alcanza nunca.
 */
private suspend fun PagerState.settleOnTabPage(page: Int) {
    val current = currentPage + currentPageOffsetFraction
    if (abs(page - current) > 1f) {
        scrollToPage(if (page > current) page - 1 else page + 1)
    }
    animateScrollToPage(page, animationSpec = BallTravelSpring)
}

/**
 * Recorrido de un toque. Bastante más blando que el muelle que movía antes el pager
 * (`stiffness` 1200): aquel tenía que cruzar tres pestañas y se veía disparado. Con el
 * arco entero por delante, el viaje pide tiempo para leerse.
 */
private val BallTravelSpring: AnimationSpec<Float> =
    spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow, visibilityThreshold = .001f)
