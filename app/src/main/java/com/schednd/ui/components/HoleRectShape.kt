package com.schednd.ui.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path

/**
 * Traza en [path] el contorno de la barra inferior: un rectángulo cuyo borde superior lleva
 * un único "hueco" curvo que viaja con la bolita, en vez de saltar entre posiciones fijas.
 * Adaptado de path_power (enmanuel52).
 *
 * Recibe el path y lo rebobina en lugar de devolver uno nuevo: esto se redibuja en cada
 * frame del salto, y crear un `Path` por frame es basura que no hace falta generar.
 *
 * @param holeSizePx diámetro del hueco
 * @param centerX centro del hueco en px, puede quedar fuera de la barra
 * @param deepProgress 0 = borde recto, 1 = hueco completo
 */
internal fun buildTravelingHolePath(
    path: Path,
    size: Size,
    holeSizePx: Float,
    centerX: Float,
    deepProgress: Float,
) {
    path.rewind()

    val paddingPx = holeSizePx.times(.2f)
    val holeStart = centerX - (holeSizePx + paddingPx)
    val holeEnd = centerX + holeSizePx + paddingPx

    path.moveTo(0f, 0f)

    // Solo se dibuja la curva si el hueco tiene fondo y cae dentro de la barra.
    if (deepProgress > 0.001f && holeEnd > 0f && holeStart < size.width) {
        path.lineTo(holeStart.coerceIn(0f, size.width), 0f)
        path.holeCurve(
            holeSizePx = holeSizePx,
            centerX = centerX,
            deepProgress = deepProgress
        )
        path.lineTo(holeEnd.coerceIn(0f, size.width), 0f)
    }

    path.lineTo(size.width, 0f)
    path.lineTo(size.width, size.height)
    path.lineTo(0f, size.height)
    path.close()
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
 * Es la misma curva que traza [buildTravelingHolePath], con los mismos puntos de control,
 * pero evaluada como función de x en vez de dibujada. El shader del cristal necesita saber
 * a qué altura queda el borde en cada columna de píxeles, y no sabe recorrer un path.
 *
 * Los cuatro puntos de control de cada tramo son proporcionales a `deepProgress`, luego la
 * curva entera lo es: se resuelve una vez a profundidad 1 y después basta con escalarla.
 * Así las 24 bisecciones por muestra salen del frame y solo se pagan al medir la barra.
 */
internal fun sampleTravelingHoleTop(
    out: FloatArray,
    holeSizePx: Float,
    deepProgress: Float,
) {
    val unit = unitHoleProfile(holeSizePx, out.size)
    for (index in out.indices) {
        out[index] = unit[index] * deepProgress
    }
}

// Solo se toca desde el hilo principal, al medir la barra o al mover la bolita.
private var cachedProfileHoleSize = Float.NaN
private var cachedProfile: FloatArray? = null

private fun unitHoleProfile(holeSizePx: Float, samples: Int): FloatArray {
    val cached = cachedProfile
    if (cached != null && cached.size == samples && cachedProfileHoleSize == holeSizePx) {
        return cached
    }
    val profile = FloatArray(samples)
    fillUnitHoleProfile(profile, holeSizePx)
    cachedProfile = profile
    cachedProfileHoleSize = holeSizePx
    return profile
}

/** El perfil de la muesca a profundidad 1, del que sale cualquier otro escalándolo. */
private fun fillUnitHoleProfile(out: FloatArray, holeSizePx: Float) {
    val pad = holeSizePx * .2f
    val half = holeSizePx + pad
    val yEdge = holeSizePx / 2 + pad * .8f
    val yPeak = holeSizePx / 2 + pad

    for (index in out.indices) {
        val x = -half + 2f * half * index / (out.size - 1).toFloat()
        out[index] = when {
            x <= -pad -> {
                val t = solveForX(x, -half, -(holeSizePx * .33f + pad), -(holeSizePx * .66f + pad), -pad)
                cubicAt(0f, holeSizePx * .1f, holeSizePx / 2, yEdge, t)
            }
            x >= pad -> {
                val t = solveForX(x, pad, holeSizePx * .66f + pad, holeSizePx * .33f + pad, half)
                cubicAt(yEdge, holeSizePx / 2, holeSizePx * .1f, 0f, t)
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
