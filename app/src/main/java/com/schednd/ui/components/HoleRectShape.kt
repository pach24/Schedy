package com.schednd.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Rectángulo con un único "hueco" curvo que viaja con la bolita de la barra inferior:
 * la deformación sigue su posición horizontal y su profundidad, en vez de saltar
 * entre posiciones fijas. Adaptado de path_power (enmanuel52).
 *
 * @param holeSizePx diámetro del hueco
 * @param holeCenterX centro del hueco en px, puede quedar fuera de la barra
 * @param holeDepth 0 = borde recto, 1 = hueco completo (>1 lo exagera al aterrizar)
 */
class TravelingHoleShape(
    private val holeSizePx: Float,
    private val holeCenterX: Float,
    private val holeDepth: Float,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ) = Outline.Generic(
        getTravelingHolePath(
            size = size,
            holeSizePx = holeSizePx,
            centerX = holeCenterX,
            deepProgress = holeDepth
        )
    )
}

internal fun getTravelingHolePath(
    size: Size,
    holeSizePx: Float,
    centerX: Float,
    deepProgress: Float,
): Path = Path().apply {
    val paddingPx = holeSizePx.times(.2f)
    val holeStart = centerX - (holeSizePx + paddingPx)
    val holeEnd = centerX + holeSizePx + paddingPx

    moveTo(0f, 0f)

    // Solo se dibuja la curva si el hueco tiene fondo y cae dentro de la barra.
    if (deepProgress > 0.001f && holeEnd > 0f && holeStart < size.width) {
        lineTo(holeStart.coerceIn(0f, size.width), 0f)
        holeCurve(
            holeSizePx = holeSizePx,
            centerX = centerX,
            deepProgress = deepProgress
        )
        lineTo(holeEnd.coerceIn(0f, size.width), 0f)
    }

    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/** Traza la curva del hueco continuando el contorno actual (no abre subpath). */
private fun Path.holeCurve(
    holeSizePx: Float,
    centerX: Float,
    deepProgress: Float,
) {
    val paddingPx = holeSizePx.times(.2f)

    cubicTo(
        x1 = centerX - holeSizePx.times(.33f) - paddingPx,
        y1 = holeSizePx.times(.1f) * deepProgress,
        x2 = centerX - holeSizePx.times(.66f) - paddingPx,
        y2 = holeSizePx * 2 / 4 * deepProgress,
        x3 = centerX - paddingPx,
        y3 = (holeSizePx / 2 + paddingPx.times(.8f)) * deepProgress,
    )

    quadraticTo(
        x1 = centerX,
        y1 = (holeSizePx / 2 + paddingPx) * deepProgress,
        x2 = centerX + paddingPx,
        y2 = (holeSizePx / 2 + paddingPx.times(.8f)) * deepProgress
    )

    cubicTo(
        x1 = centerX + holeSizePx.times(.66f) + paddingPx,
        y1 = holeSizePx / 2 * deepProgress,
        x2 = centerX + holeSizePx.times(.33f) + paddingPx,
        y2 = holeSizePx.times(.1f) * deepProgress,
        x3 = centerX + holeSizePx + paddingPx,
        y3 = 0f,
    )
}
