package com.schednd.ui.session

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.schednd.ui.components.TravelingHoleShape
import com.schednd.ui.theme.SchedndTheme
import kotlin.math.PI
import kotlin.math.sin

enum class SessionTab(val label: String, val icon: ImageVector) {
    HOME("Inicio", Icons.Outlined.Home),
    SESSIONS("Sesiones", Icons.AutoMirrored.Outlined.FormatListBulleted),
    CALENDAR("Calendario", Icons.Outlined.CalendarMonth),
    PROFILE("Perfil", Icons.Outlined.Person)
}

/**
 * Barra inferior con "bolita saltarina": el indicador describe una parábola hacia
 * la pestaña destino mientras el fondo cierra el hueco anterior y abre el nuevo.
 * Adaptado de path_power (enmanuel52).
 */
@Composable
fun SessionBottomBar(
    selectedTab: SessionTab,
    onTabSelected: (SessionTab) -> Unit,
    modifier: Modifier = Modifier,
    items: List<SessionTab> = SessionTab.entries
) {
    val containerColor = MaterialTheme.colorScheme.surface
    val ballColor = MaterialTheme.colorScheme.onSurface

    var previousTab by remember { mutableStateOf(selectedTab) }
    // Un único progreso 0..1 orquesta el recorrido y la deformación del fondo.
    val animationProgress = remember { Animatable(1f) }

    // Se dispara con cualquier cambio de pestaña, venga de un toque en la barra o de
    // otro sitio de la pantalla (p. ej. "Todas las sesiones").
    LaunchedEffect(selectedTab) {
        if (previousTab == selectedTab) return@LaunchedEffect
        animationProgress.snapTo(0f)
        try {
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(SlideDurationMillis, easing = FastOutSlowInEasing)
            )
        } finally {
            // Aunque se cancele a medias, el origen del siguiente recorrido es este destino.
            previousTab = selectedTab
        }
    }

    val density = LocalDensity.current
    val ballSizePx = with(density) { BallSize.toPx() }
    var barWidthPx by remember { mutableFloatStateOf(0f) }

    // Arco sinusoidal: sin picos, sube y baja sin que se note el vértice.
    val arc by remember(animationProgress.value) {
        derivedStateOf { sin(animationProgress.value * PI.toFloat()) }
    }

    val ballOffset by remember(animationProgress.value, barWidthPx, arc) {
        derivedStateOf {
            val currentIndex = items.indexOf(selectedTab)
            val previousIndex = items.indexOf(previousTab)

            val distance =
                (currentIndex - previousIndex) / items.size.toFloat() * animationProgress.value
            val xProgress = previousIndex.plus(1f) / items.size + distance

            Offset(
                x = xProgress * barWidthPx,
                y = -ballSizePx * ArcHeightFactor * arc
            )
        }
    }

    // El hueco no salta entre posiciones: viaja con la bolita. Se aplana cuando ella
    // está arriba y vuelve a hundirse a medida que baja.
    val holeDepth by remember(arc) {
        derivedStateOf { 1f - smoothStep(arc) }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
        ) {
            // La bolita va debajo del fondo: así parece hundirse en el hueco.
            Surface(
                modifier = Modifier
                    .size(BallSize)
                    .graphicsLayer {
                        val xAnchor = 1f / items.size * barWidthPx / 2 + ballSizePx / 2
                        translationX = ballOffset.x - xAnchor
                        translationY = ballOffset.y - ballSizePx / 2
                        // Se estira levemente en el tramo alto del recorrido.
                        scaleY = 1f + StretchFactor * arc
                        scaleX = 1f - StretchFactor * arc
                    },
                shape = CircleShape,
                color = ballColor,
                tonalElevation = BottomTonalElevation
            ) {}

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = TravelingHoleShape(
                    holeSizePx = ballSizePx,
                    holeCenterX = ballOffset.x - 1f / items.size * barWidthPx / 2,
                    holeDepth = holeDepth
                ),
                color = containerColor,
                tonalElevation = BottomTonalElevation
            ) {}

            // Fuera del Surface con forma: si no, los iconos quedarían recortados.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { tab ->
                    val transition = updateTransition(
                        targetState = tab == selectedTab,
                        label = "tabSelection"
                    )
                    val bottomBarHeightPx = with(density) { BottomBarHeight.toPx() }

                    // El icono viaja con la bolita: mismo easing que el recorrido.
                    val elevationOffset by transition.animateFloat(
                        label = "tabElevation",
                        transitionSpec = { tween(SlideDurationMillis, easing = FastOutSlowInEasing) }
                    ) { isSelected -> if (isSelected) -bottomBarHeightPx / 2 else 0f }

                    val iconScale by transition.animateFloat(
                        label = "tabScale",
                        transitionSpec = { tween(SlideDurationMillis, easing = FastOutSlowInEasing) }
                    ) { isSelected -> if (isSelected) SelectedIconScale else 1f }

                    val iconColor by transition.animateColor(
                        label = "tabColor",
                        transitionSpec = { tween(SlideDurationMillis) }
                    ) { isSelected ->
                        if (isSelected) containerColor else MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    IconButton(
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier.graphicsLayer {
                            translationY = elevationOffset
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconColor
                        )
                    }
                }
            }
        }

        // Prolonga el fondo por detrás de la barra de navegación del sistema.
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(containerColor)
        )
    }
}

/** Suaviza los extremos (smoothstep) para que huecos y curvas no arranquen ni paren en seco. */
private fun smoothStep(t: Float): Float = t * t * (3f - 2f * t)

private const val SlideDurationMillis = 340
private const val ArcHeightFactor = .5f
private const val StretchFactor = .12f
private const val SelectedIconScale = 1.12f
private val BallSize = 52.dp
private val BottomBarHeight = 72.dp
private val BottomTonalElevation = 3.dp

@Preview(showBackground = true)
@Composable
private fun SessionBottomBarPreview() {
    SchedndTheme(darkTheme = false) {
        var selected by remember { mutableStateOf(SessionTab.HOME) }
        SessionBottomBar(selectedTab = selected, onTabSelected = { selected = it })
    }
}
