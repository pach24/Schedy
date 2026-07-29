package com.schednd.ui.session

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.schednd.R
import com.schednd.ui.components.LiquidGlassState
import com.schednd.ui.components.buildTravelingHolePath
import com.schednd.ui.components.liquidGlassShape
import com.schednd.ui.theme.LightRaisedSurface
import com.schednd.ui.theme.SchedyTheme
import kotlin.math.abs

enum class SessionTab(@StringRes val labelRes: Int) {
    HOME(R.string.tab_home),
    SESSIONS(R.string.tab_sessions),
    CALENDAR(R.string.tab_calendar),
    PROFILE(R.string.tab_profile);

    val label: String
        @Composable get() = stringResource(labelRes)

    /** Las cuatro son dibujos propios, de la misma familia de trazo. */
    val icon: Painter
        @Composable get() = when (this) {
            HOME -> painterResource(R.drawable.ic_home)
            CALENDAR -> painterResource(R.drawable.ic_calendar)
            SESSIONS -> painterResource(R.drawable.ic_list)
            PROFILE -> painterResource(R.drawable.ic_user)
        }
}

/**
 * Barra inferior con "bolita saltarina": el indicador viaja hacia la pestaña destino
 * mientras el fondo cierra el hueco anterior y abre el nuevo. Adaptado de path_power
 * (enmanuel52).
 *
 * Todo el movimiento sale de un único [Animatable] con la posición continua de la bolita,
 * medida en pestañas. Dos consecuencias, y las dos son el motivo de que esté así montado:
 *
 * 1. El muelle se puede reapuntar en marcha conservando la velocidad, así que tocar otra
 *    pestaña a mitad de trayecto desvía la bolita en vez de teleportarla al origen.
 * 2. Nada de esto se lee durante la composición: la posición se consulta dentro de las
 *    lambdas de layout, `graphicsLayer` y dibujo. La barra no recompone ni un solo frame
 *    del recorrido; solo se reasignan transformaciones y se rehace un path ya reservado.
 */
@Composable
fun SessionBottomBar(
    selectedTab: SessionTab,
    onTabSelected: (SessionTab) -> Unit,
    modifier: Modifier = Modifier,
    items: List<SessionTab> = SessionTab.entries,
    glass: LiquidGlassState? = null
) {
    val glassActive = glass?.isSupported == true
    val darkTheme = isSystemInDarkTheme()
    val barColor = if (darkTheme) MaterialTheme.colorScheme.surface else LightRaisedSurface
    // Con cristal la barra deja de ser un panel opaco: mantiene su color pero lo baja a
    // un velo, porque lo que se ve por debajo es el fondo ya difuminado por el shader.
    val containerColor = when {
        !glassActive ->
            if (darkTheme) MaterialTheme.colorScheme.surfaceColorAtElevation(BottomTonalElevation)
            else barColor
        darkTheme -> barColor.copy(alpha = GlassBarAlphaDark)
        else -> barColor.copy(alpha = GlassBarAlphaLight)
    }
    val ballColor = when {
        // Sin cristal, la bolita es sólida como toda la vida.
        !glassActive -> MaterialTheme.colorScheme.onSurface
        // Teñir con onSurface solo funciona en oscuro, donde es casi blanco. En claro es
        // casi negro y ensucia el cristal, así que en claro el tinte es blanco.
        darkTheme -> MaterialTheme.colorScheme.onSurface.copy(alpha = GlassBallAlphaDark)
        else -> Color.White.copy(alpha = GlassBallAlphaLight)
    }
    val idleIconColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Con cristal la bolita es transparente, así que el icono elegido no puede ir del
    // color de la barra: se perdería contra el fondo.
    val activeIconColor =
        if (glassActive) MaterialTheme.colorScheme.onSurface else containerColor

    // Posición de la bolita en pestañas, con decimales. Es la única fuente del movimiento.
    val targetIndex = items.indexOf(selectedTab).coerceAtLeast(0).toFloat()
    val position = remember { Animatable(targetIndex) }

    // La velocidad que lleva la bolita ahora mismo, para entregársela al siguiente tramo:
    // reapuntar a media trayectoria es entonces un desvío y no un frenazo con arranque.
    //
    // Hay que cogerla aquí, en la composición, porque cambiar la pestaña cancela el
    // `LaunchedEffect` de abajo y `Animatable` pone la velocidad a cero al cancelarse:
    // cuando el nuevo efecto arranca, ya se ha perdido. Y se lee sin observar porque
    // observarla traería de vuelta justo lo que se quiere evitar, recomponer por frame.
    val carriedVelocity = Snapshot.withoutReadObservation { position.velocity }

    // Se dispara con cualquier cambio de pestaña, venga de un toque en la barra o de otro
    // sitio de la pantalla (p. ej. "Todas las sesiones").
    LaunchedEffect(targetIndex) {
        if (position.value != targetIndex) {
            position.animateTo(targetIndex, HopSpring, initialVelocity = carriedVelocity)
        }
    }

    val density = LocalDensity.current
    val ballSizePx = with(density) { BallSize.toPx() }
    val barHeightPx = with(density) { BottomBarHeight.toPx() }
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var barTopPx by remember { mutableFloatStateOf(Float.NaN) }

    // Altura del salto. Sale de la velocidad del muelle, que es continua por construcción:
    // vale 0 en reposo, sube al lanzarse y baja al posarse, y nunca da un tirón aunque se
    // cambie de destino en pleno vuelo.
    fun hopArc(): Float =
        smoothStep((abs(position.velocity) / HopPeakVelocity).coerceIn(0f, 1f))

    fun ballCenterX(): Float = (position.value + .5f) / items.size * barWidthPx

    /** Cuánto le toca a esta pestaña de estar "elegida": 1 con la bolita encima, 0 lejos. */
    fun tabSelection(index: Int): Float =
        (1f - abs(position.value - index)).coerceIn(0f, 1f)

    // La bolita no puede usar `liquidGlassShape`: se mueve por `graphicsLayer` sin
    // recomponer ni relayoutar, así que publica su geometría a mano.
    val ballShapeId = remember { LiquidGlassState.newShapeId() }

    DisposableEffect(glass, ballShapeId) {
        onDispose { glass?.removeShape(ballShapeId) }
    }

    /**
     * Pasa al shader la bolita y la muesca de este frame. Se llama desde la fase de layout
     * a propósito: el fondo de cristal se dibuja antes que la barra, así que si se
     * publicara al dibujar la bolita, la refracción iría siempre un frame por detrás.
     */
    fun publishGlassGeometry() {
        val state = glass ?: return
        if (!state.isSupported || barWidthPx <= 0f || barTopPx.isNaN()) return

        val arc = hopArc()
        val centerX = ballCenterX()
        state.updateHole(
            centerX = centerX,
            topY = barTopPx,
            holeSize = ballSizePx,
            // El hueco no salta entre posiciones: viaja con la bolita. Se aplana cuando
            // ella está arriba y vuelve a hundirse a medida que baja.
            depthProgress = 1f - arc,
        )
        val halfWidth = ballSizePx / 2f * (1f - StretchFactor * arc)
        val halfHeight = ballSizePx / 2f * (1f + StretchFactor * arc)
        state.updateShape(
            id = ballShapeId,
            centerX = centerX,
            centerY = barTopPx - ballSizePx * ArcHeightFactor * arc,
            halfWidth = halfWidth,
            halfHeight = halfHeight,
            // Radio al máximo: el rectángulo redondeado se convierte en el círculo (o la
            // elipse, al estirarse) de la bolita.
            cornerRadius = minOf(halfWidth, halfHeight),
        )
    }

    // El path de la barra se reserva una vez y se rebobina en cada frame.
    val barPath = remember { Path() }

    // Toda la barra es una pieza de cristal, insets del sistema incluidos: si la franja
    // de abajo se quedara fuera, el velo se vería sin refracción justo ahí. El hueco de la
    // bolita se le resta en el shader, para que la refracción y el filo se hundan con él
    // en vez de cruzar rectos por encima.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlassShape(glass, cornerRadius = 0.dp, carveHole = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .onSizeChanged { barWidthPx = it.width.toFloat() }
                .onGloballyPositioned { coordinates ->
                    barTopPx = coordinates.positionInRoot().y
                }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        publishGlassGeometry()
                        placeable.place(0, 0)
                    }
                }
        ) {
            // La bolita va debajo del fondo: así parece hundirse en el hueco.
            Spacer(
                modifier = Modifier
                    .size(BallSize)
                    .graphicsLayer {
                        val arc = hopArc()
                        translationX = ballCenterX() - ballSizePx / 2f
                        translationY = -ballSizePx / 2f - ballSizePx * ArcHeightFactor * arc
                        // Se estira levemente en el tramo alto del recorrido.
                        scaleY = 1f + StretchFactor * arc
                        scaleX = 1f - StretchFactor * arc
                    }
                    .background(ballColor, CircleShape)
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                buildTravelingHolePath(
                    path = barPath,
                    size = size,
                    holeSizePx = ballSizePx,
                    centerX = ballCenterX(),
                    deepProgress = 1f - hopArc(),
                )
                drawPath(barPath, containerColor)
            }

            // Fuera del fondo con forma: si no, los iconos quedarían recortados.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, tab ->
                    val painter = tab.icon
                    val label = tab.label
                    IconButton(
                        onClick = { onTabSelected(tab) },
                        modifier = Modifier
                            .size(TabTouchSize)
                            .semantics { contentDescription = label }
                            // El icono viaja con la bolita: se eleva a medida que ella se
                            // le acerca, así que el relevo entre pestañas es continuo.
                            .graphicsLayer {
                                val selection = tabSelection(index)
                                translationY = -barHeightPx / 2f * selection
                                scaleX = 1f + (SelectedIconScale - 1f) * selection
                                scaleY = scaleX
                            }
                    ) {
                        // Dos copias cruzándose por alfa: el tinte de `Icon` es un
                        // parámetro, y animarlo obligaría a recomponer en cada frame.
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = idleIconColor,
                            modifier = Modifier.graphicsLayer {
                                alpha = 1f - tabSelection(index)
                            }
                        )
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = activeIconColor,
                            modifier = Modifier.graphicsLayer {
                                alpha = tabSelection(index)
                            }
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

/**
 * Rígido y casi sin rebote: el trayecto de una pestaña se resuelve en poco más de 100 ms,
 * pero sigue siendo un muelle, así que admite que le cambien el destino sin cortes.
 */
private val HopSpring: SpringSpec<Float> = spring(dampingRatio = .9f, stiffness = 1200f)

/**
 * Velocidad, en pestañas por segundo, a la que el salto llega a su altura máxima. Está
 * puesta un poco por debajo del pico que alcanza [HopSpring] al saltar una pestaña, para
 * que ese salto —el habitual— llegue arriba del todo; los de dos o tres pestañas van más
 * rápidos y lo único que cambia es que se mantienen arriba más rato.
 */
private const val HopPeakVelocity = 11f
private const val ArcHeightFactor = .5f
private const val StretchFactor = .12f
private const val SelectedIconScale = 1.12f
// La bolita es cristal, no relleno: si tapa el fondo no se ve su propia refracción.
private const val GlassBallAlphaDark = .22f
private const val GlassBallAlphaLight = .5f
private const val GlassBarAlphaDark = .68f
private const val GlassBarAlphaLight = .72f
/** Área pulsable de cada pestaña. Por encima de los 48 dp mínimos de Material. */
private val TabTouchSize = 64.dp
private val BallSize = 52.dp
private val BottomBarHeight = 72.dp
private val BottomTonalElevation = 3.dp

/** Alto de la barra sin los insets del sistema, para reservarle sitio desde fuera. */
val SessionBottomBarHeight = BottomBarHeight

@Preview(showBackground = true)
@Composable
private fun SessionBottomBarPreview() {
    SchedyTheme(darkTheme = false) {
        var selected by remember { mutableStateOf(SessionTab.HOME) }
        SessionBottomBar(selectedTab = selected, onTabSelected = { selected = it })
    }
}
