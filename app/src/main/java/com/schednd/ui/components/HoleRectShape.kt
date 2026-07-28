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

/** Semiancho de la muesca en múltiplos de su diámetro: `holeSizePx + paddingPx`. */
internal const val TravelingHoleSpanFactor = 1.2f

/**
 * Rellena [out] con la profundidad de la muesca en tantos puntos como quepan, repartidos
 * uniformemente entre sus dos extremos: `centerX ± holeSizePx * TravelingHoleSpanFactor`.
 *
 * Es la misma curva que traza [getTravelingHolePath], con los mismos puntos de control,
 * pero evaluada como función de x en vez de dibujada. El shader del cristal necesita saber
 * a qué altura queda el borde en cada columna de píxeles, y no sabe recorrer un path.
 *
 * Invertir x(t) es legítimo porque los tres tramos avanzan en x sin retroceder, aunque sus
 * puntos de control vayan cruzados.
 */
internal fun sampleTravelingHoleTop(
    out: FloatArray,
    holeSizePx: Float,
    deepProgress: Float,
) {
    val pad = holeSizePx * .2f
    val half = holeSizePx + pad
    val yEdge = (holeSizePx / 2 + pad * .8f) * deepProgress
    val yPeak = (holeSizePx / 2 + pad) * deepProgress

    for (index in out.indices) {
        val x = -half + 2f * half * index / (out.size - 1).toFloat()
        out[index] = when {
            x <= -pad -> {
                val t = solveForX(x, -half, -(holeSizePx * .33f + pad), -(holeSizePx * .66f + pad), -pad)
                cubicAt(0f, holeSizePx * .1f * deepProgress, holeSizePx / 2 * deepProgress, yEdge, t)
            }
            x >= pad -> {
                val t = solveForX(x, pad, holeSizePx * .66f + pad, holeSizePx * .33f + pad, half)
                cubicAt(yEdge, holeSizePx / 2 * deepProgress, holeSizePx * .1f * deepProgress, 0f, t)
            }
            else -> {
                // El tramo central es simétrico, así que x avanza lineal con t.
                quadraticAt(yEdge, yPeak, yEdge, (x + pad) / (2f * pad))
            }
        }
    }
}

/** Bisección sobre t: x(t) es monótona en cada tramo, así que basta con partir por la mitad. */
private fun solveForX(x: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
    var low = 0f
    var high = 1f
    repeat(24) {
        val mid = (low + high) / 2f
        if (cubicAt(p0, p1, p2, p3, mid) < x) low = mid else high = mid
    }
    return (low + high) / 2f
}

private fun cubicAt(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
    val u = 1f - t
    return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3
}

private fun quadraticAt(p0: Float, p1: Float, p2: Float, t: Float): Float {
    val u = 1f - t
    return u * u * p0 + 2f * u * t * p1 + t * t * p2
}
