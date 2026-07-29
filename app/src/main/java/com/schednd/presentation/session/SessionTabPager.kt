package com.schednd.presentation.session

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.pager.PagerState

/**
 * Lleva el pager de pestañas a [page] recorriendo el camino.
 *
 * No usa `animateScrollToPage` porque este, cuando el destino queda a tres páginas o más,
 * salta primero a una página del destino y anima solo el último tramo. Es lo correcto para
 * un carrusel de cientos de páginas, pero aquí son cuatro y la bolita de la barra va pegada
 * a la posición del pager: ese salto se vería como un tirón en mitad del recorrido.
 */
internal suspend fun PagerState.animateToTabPage(page: Int) {
    val pageSizePx = (layoutInfo.pageSize + layoutInfo.pageSpacing).toFloat()
    if (pageSizePx <= 0f) {
        // Todavía sin medir: no hay recorrido que animar.
        scrollToPage(page)
        return
    }

    val distance = (page - (currentPage + currentPageOffsetFraction)) * pageSizePx
    scroll {
        var consumed = 0f
        animate(0f, distance, animationSpec = TabScrollSpring) { value, _ ->
            consumed += scrollBy(value - consumed)
        }
        // El muelle para dentro de su umbral, no exactamente encima. El resto se remata
        // aquí dentro, no después: si el dedo interrumpe el scroll, esto se cancela con él
        // en vez de devolver la página de un tirón.
        scrollBy(distance - consumed)
    }
}

/** El mismo tacto que tenía el salto de la bolita cuando lo movía ella sola. */
private val TabScrollSpring: AnimationSpec<Float> =
    spring(dampingRatio = .9f, stiffness = 1200f, visibilityThreshold = .5f)
