package com.schednd.presentation.session

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.schednd.ui.theme.pressScale
import kotlin.math.abs
import kotlin.math.roundToInt

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
 * Todo el movimiento sale de [position], la posición continua de la bolita medida en
 * pestañas: 0 es la primera, 1.5 el punto medio entre la segunda y la tercera. La barra no
 * la anima, la lee — quien la mueve es el pager del contenido, así que el salto va pegado
 * al dedo al deslizar y acompaña al recorrido al tocar, sin dos relojes que sincronizar.
 *
 * Es una lambda y no un `Float` a propósito: así se consulta dentro de las lambdas de
 * layout, `graphicsLayer` y dibujo, nunca durante la composición. La barra no recompone ni
 * un solo frame del recorrido; solo se reasignan transformaciones y se rehace un path ya
 * reservado.
 */
@Composable
fun SessionBottomBar(
    position: () -> Float,
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

    val density = LocalDensity.current
    val ballSizePx = with(density) { BallSize.toPx() }
    val barHeightPx = with(density) { BottomBarHeight.toPx() }
    var barWidthPx by remember { mutableFloatStateOf(0f) }
    var barTopPx by remember { mutableFloatStateOf(Float.NaN) }

    // Altura del salto: lo lejos que está la bolita de la muesca más cercana. Sube al
    // salir de una y baja al entrar en la siguiente, sin depender de ningún reloj propio,
    // así que al deslizar el salto va exactamente donde va el dedo.
    fun hopArc(): Float {
        val current = position()
        val toNearestTab = abs(current - current.roundToInt())
        return smoothStep((toNearestTab * 2f).coerceIn(0f, 1f))
    }

    /**
     * Profundidad de la muesca. El hueco no salta entre posiciones: viaja con la bolita, se
     * va cerrando cuando ella sube y se hunde otra vez cuando baja.
     *
     * Lo que no hace es llegar a cerrarse del todo. En lo alto del salto la bolita está
     * estirada, y su borde de abajo se queda por debajo del de la barra: con el borde recto
     * lo que se ve es la bolita cortada por una línea. Este mínimo le deja hueco siempre.
     */
    fun holeDepth(): Float = MinHoleDepth + (1f - MinHoleDepth) * (1f - hopArc())

    fun ballCenterX(): Float = (position() + .5f) / items.size * barWidthPx

    /** Centro del hueco de una pestaña. Con `SpaceAround` cae justo donde para la bolita. */
    fun tabCenterX(index: Int): Float = (index + .5f) / items.size * barWidthPx

    /**
     * Cuánto va este icono montado en la bolita: 1 encima de ella, 0 quieto en su hueco.
     *
     * El relevo ocurre al cruzar el punto medio y dura poco. Mientras cruza, los dos iconos
     * están a la vez encima de la bolita, uno apagándose y el otro encendiéndose, así que se
     * lee como que el que viajaba se convierte en el de destino al llegar.
     */
    fun ride(index: Int): Float {
        val distance = abs(position() - index)
        val handoff = (distance - RideHandoffStart) / (RideHandoffEnd - RideHandoffStart)
        return 1f - smoothStep(handoff.coerceIn(0f, 1f))
    }

    // Una fuente de interacción por pestaña: la alimenta la zona pulsable y la consume el
    // icono. Van separadas porque la respuesta al dedo tiene que viajar con el icono, y la
    // zona pulsable no se mueve.
    val interactionSources = remember(items.size) {
        List(items.size) { MutableInteractionSource() }
    }

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
            depthProgress = holeDepth(),
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
                    deepProgress = holeDepth(),
                )
                drawPath(barPath, containerColor)
            }

            // Las zonas pulsables van aparte de los iconos y no se mueven nunca: el icono
            // elegido se va de viaje con la bolita, y una diana que se marcha con él —o que
            // se planta encima de la pestaña vecina— no hay quien la acierte.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, tab ->
                    val label = tab.label
                    Box(
                        modifier = Modifier
                            .size(TabTouchSize)
                            .clickable(
                                interactionSource = interactionSources[index],
                                // Sin ripple. Se dibuja donde está la zona pulsable, que se
                                // queda quieta, mientras el icono se marcha con la bolita:
                                // no hay tamaño ni posición que lo hagan cuadrar. Quien
                                // responde al dedo es el icono, encogiéndose.
                                indication = null,
                                onClick = { onTabSelected(tab) }
                            )
                            .semantics { contentDescription = label }
                    )
                }
            }

            // Encima de todo y sin recortar: el icono que viaja se sale de su hueco, y
            // cualquier clip por el camino se lo comería a medio recorrido.
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, tab ->
                    val painter = tab.icon
                    Box(
                        modifier = Modifier.size(TabTouchSize),
                        contentAlignment = Alignment.Center
                    ) {
                        // Dos copias cruzándose por alfa: el tinte de `Icon` es un
                        // parámetro, y animarlo obligaría a recomponer en cada frame.
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = idleIconColor,
                            modifier = Modifier
                                .graphicsLayer { alpha = 1f - ride(index) }
                                .pressScale(interactionSources[index])
                        )
                        // La copia elegida no se limita a levantarse en su sitio: se pega a
                        // la bolita y hace con ella todo el trayecto, salto incluido.
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            tint = activeIconColor,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = ballCenterX() - tabCenterX(index)
                                    translationY = -barHeightPx / 2f -
                                        ballSizePx * ArcHeightFactor * hopArc()
                                    alpha = ride(index)
                                    scaleX = SelectedIconScale
                                    scaleY = SelectedIconScale
                                }
                                // Después de la capa de viaje, no antes: así se encoge
                                // sobre su propio centro y no se desplaza al hacerlo.
                                .pressScale(interactionSources[index])
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
 * Distancias, en pestañas, entre las que el icono pasa de ir montado en la bolita a quedarse
 * en su hueco. La ventana es estrecha y va centrada en el punto medio del trayecto, que es
 * donde los dos iconos se cruzan encima de la bolita.
 */
private const val RideHandoffStart = .42f
private const val RideHandoffEnd = .58f
/**
 * Lo que le queda de muesca a la barra en lo alto del salto, donde antes se quedaba recta.
 *
 * Sale de la cuenta: medido en [BallSize], el borde de abajo de la bolita está a
 * `ArcHeightFactor - (ArcHeightFactor - StretchFactor / 2)` del borde de la barra —o sea,
 * .06 por debajo— y el fondo de la muesca cae a .68 de su profundidad. Con .18 le quedan
 * unos 3 dp de aire, suficiente para que no se vea el corte.
 */
private const val MinHoleDepth = .18f
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
        SessionBottomBar(position = { 0f }, onTabSelected = {})
    }
}
