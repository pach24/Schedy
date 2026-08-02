package com.schednd.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Cuánto está ampliado el calendario, y el gesto que lo amplía: se tira de él hacia abajo
 * cuando ya no queda nada que desplazar, y se recoge empujando hacia arriba.
 *
 * El gesto va por [nestedScrollConnection] y no por un `draggable` propio porque la página
 * ya se desplaza: un arrastre tiene que ser primero desplazamiento y solo convertirse en
 * ampliación cuando la lista se ha quedado sin recorrido. Eso es justo lo que cuenta el
 * scroll anidado, y montar un `draggable` encima obligaría a arbitrar entre los dos.
 *
 * [progress] se pasa de 1 mientras el muelle rebota, a propósito, y quien lo pinta lo
 * acompaña. Lo que sí se acota es a 0 por abajo: por debajo no hay nada que enseñar.
 */
@Stable
class CalendarExpansionState internal constructor(
    private val scope: CoroutineScope,
    private val dragDistancePx: Float
) {
    var progress by mutableFloatStateOf(0f)
        private set

    private var settleJob: Job? = null

    /**
     * Si el mes manda ya sobre el deslizar de lado. Es el mismo umbral que usa el pager
     * para apartarse, y a propósito: con dos distintos habría un tramo en el que deslizar
     * de lado no haría ni una cosa ni la otra.
     */
    val isOpen: Boolean get() = progress > OpenThreshold

    /** Consume del arrastre lo que quepa y devuelve cuánto ha usado, en píxeles. */
    private fun dragBy(deltaPx: Float): Float {
        settleJob?.cancel()
        val before = progress
        progress = (progress + deltaPx / dragDistancePx).coerceIn(0f, 1f)
        return (progress - before) * dragDistancePx
    }

    /**
     * Suelta el gesto: se va al extremo que pida la velocidad, y si no hay velocidad, al
     * que tenga más cerca. El muelle blando se pasa de rosca al llegar, que es lo que hace
     * que el mes parezca abrirse de golpe en vez de estirarse.
     */
    private fun settle(velocityPx: Float) {
        val target = when {
            velocityPx > FlingVelocityPx -> 1f
            velocityPx < -FlingVelocityPx -> 0f
            else -> if (progress > 0.5f) 1f else 0f
        }
        animateTo(target, velocityPx / dragDistancePx)
    }

    fun collapse() {
        if (progress == 0f) return
        animateTo(0f, initialVelocity = 0f)
    }

    private fun animateTo(target: Float, initialVelocity: Float) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue = progress,
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = spring(
                    dampingRatio = SettleDamping,
                    stiffness = Spring.StiffnessLow
                )
            ) { value, _ -> progress = value }
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {

        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Empujar hacia arriba con el calendario abierto lo recoge antes de mover
            // nada: si no, la página se iría hacia arriba dejando el mes estirado.
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (available.y >= 0f || progress <= 0f) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // Lo que sobra del arrastre hacia abajo es que la página ya está arriba del
            // todo. Ese sobrante es el que amplía.
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (available.y <= 0f) return Offset.Zero
            return Offset(0f, dragBy(available.y))
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            // A medio camino manda el calendario: gasta el impulso en terminar de abrirse
            // o de cerrarse, y la página no llega a lanzarse. Abierto o cerrado del todo
            // el impulso es de la página, que para eso sigue habiendo mes que recorrer.
            if (progress <= 0f || progress >= 1f) return Velocity.Zero
            settle(available.y)
            return available
        }
    }
}

@Composable
fun rememberCalendarExpansion(): CalendarExpansionState {
    val scope = rememberCoroutineScope()
    val dragDistancePx = with(LocalDensity.current) { DragDistance.toPx() }
    return remember(scope, dragDistancePx) { CalendarExpansionState(scope, dragDistancePx) }
}

/** Cuánto hay que arrastrar hacia abajo para abrir el mes del todo. */
private val DragDistance = 160.dp

/** A partir de aquí el impulso decide por su cuenta, sin mirar por dónde iba el gesto. */
private const val FlingVelocityPx = 600f

/** Por debajo de 1 el muelle se pasa de rosca; esto es lo que se nota sin marear. */
private const val SettleDamping = 0.62f

/** A partir de aquí el calendario cuenta como abierto para lo que toca al deslizar. */
private const val OpenThreshold = 0.5f
