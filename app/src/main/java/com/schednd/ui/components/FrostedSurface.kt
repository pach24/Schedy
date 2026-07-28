package com.schednd.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Superficie esmerilada: cristal propio —warp, rim y blur del shader— si [glass] tiene
 * soporte, y si no el blur de Haze de siempre.
 *
 * El camino de cristal exige que la pieza se dibuje fuera del contenido que lleva
 * `liquidGlassBackdrop`, y que ese contenido sea el que se quiere ver refractado.
 * Lo cumplen la top bar y los diálogos, que cuelgan del Box exterior de la pantalla.
 *
 * @param cornerRadius radio de la pieza; para un círculo, la mitad del lado
 * @param glassTint velo de la pieza en modo cristal. Va más bajo que el de Haze a
 *   propósito: el shader ya difumina por debajo, y un tinte alto taparía justo lo que
 *   tiene que dejar ver.
 */
@Composable
fun Modifier.frostedSurface(
    glass: LiquidGlassState?,
    hazeState: HazeState,
    cornerRadius: Dp,
    hazeBackground: Color,
    hazeTint: Color,
    glassTint: Color,
): Modifier = if (glass?.isSupported == true) {
    liquidGlassShape(glass, cornerRadius).background(glassTint)
} else {
    hazeEffect(state = hazeState) {
        blurRadius = 20.dp
        backgroundColor = hazeBackground
        tints = listOf(HazeTint(hazeTint))
    }
}
