package com.schednd.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.schednd.R
import com.schednd.ui.theme.SquircleShape
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Difuminado de la tarjeta, bastante por encima del resto de cristales de la app: aquí
 * el fondo es un calendario denso y con poco blur se leen las rejillas a través.
 */
val DialogBlurRadius = 12.dp

/** Duración de la salida: algo más viva que la entrada, que sí se recrea con muelles. */
private const val ExitMillis = 230
private const val ScrimAlpha = 0.4f
private const val EnterScale = 0.65f
private const val ExitScale = 0.62f

/**
 * Tarjeta de cristal de los diálogos de día del calendario.
 *
 * No usa `Dialog`: el cristal necesita refractar el contenido de la pantalla, y una
 * ventana aparte no tiene acceso a él. Se dibuja fuera del contenido que lleva
 * `liquidGlassBackdrop` —al lado de la barra inferior— y quien lo muestra es la pantalla,
 * no el calendario, que solo avisa del día pulsado.
 *
 * El contenido recibe el `dismiss` que anima la salida y solo entonces avisa a la
 * pantalla; llamar a `onDismiss` directamente se saltaría la animación.
 */
@Composable
fun DayDialogShell(
    hazeState: HazeState,
    glass: LiquidGlassState?,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    // La escala se anima aquí en vez de con `scaleIn`/`scaleOut` porque el cristal
    // necesita leerla: `graphicsLayer` no toca el layout, así que la pieza de refracción
    // se quedaría del tamaño medido mientras la tarjeta encoge, y ese desajuste entre el
    // borde pintado y el refractado se lee como si la tarjeta se fuera de lado.
    val cardScale = remember { Animatable(EnterScale) }
    val cardAlpha = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch { cardAlpha.animateTo(1f, tween(180)) }
        cardScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
        )
    }

    val dismiss: () -> Unit = { closing = true }

    // La salida no se veía: quien lo muestra lo saca de la composición en cuanto se pide
    // cerrar. Se anima primero y solo al terminar se avisa a la pantalla.
    LaunchedEffect(closing) {
        if (!closing) return@LaunchedEffect
        launch { cardAlpha.animateTo(0f, tween(ExitMillis - 90, delayMillis = 70)) }
        // Pop al revés: toma un poco de aire y se recoge sobre su propio centro.
        cardScale.animateTo(1.05f, tween(70, easing = FastOutSlowInEasing))
        cardScale.animateTo(ExitScale, tween(ExitMillis - 70, easing = FastOutLinearInEasing))
        onDismiss()
    }

    // Ya no es una ventana de sistema, así que el botón atrás hay que atenderlo aquí.
    BackHandler(onBack = dismiss)

    // El velo acompaña a la tarjeta en los dos sentidos; si desapareciera de golpe la
    // salida se leería como un corte.
    val scrimAlpha by animateFloatAsState(
        targetValue = if (closing) 0f else ScrimAlpha,
        animationSpec = tween(if (closing) ExitMillis else 220),
        label = "dayDialogScrim"
    )

    val isDark = isSystemInDarkTheme()
    val cardShape = SquircleShape(28.dp)
    // Mismo reparto que en los diálogos del detalle: más velo del que lleva la top bar,
    // porque aquí hay texto que leer sobre el cristal.
    val hazeTint = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.82f) else Color.White.copy(alpha = 0.82f)
    val glassTint = if (isDark) Color(0xFF1C1C1E).copy(alpha = 0.62f) else Color.White.copy(alpha = 0.68f)
    val borderBrush = Brush.verticalGradient(
        if (isDark) {
            listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
        } else {
            listOf(Color.White, Color.Transparent)
        }
    )

    // La escala se lee en composición, no dentro del bloque de `graphicsLayer`: de este
    // mismo valor sale también la pieza de cristal, y si cada uno leyera su propio
    // fotograma el cristal iría por detrás de la tarjeta.
    val scale = cardScale.value

    // El cristal no puede ir por `liquidGlassShape`: ese mide el layout, y aquí la
    // tarjeta se escala con un `graphicsLayer`, que el layout no recoge. Se registra a
    // mano, como la bolita de la barra.
    val glassActive = glass?.isSupported == true
    val cardRadiusPx = with(LocalDensity.current) { 28.dp.toPx() }
    var cardBounds by remember { mutableStateOf<GlassBounds?>(null) }
    val shapeId = remember { LiquidGlassState.newShapeId() }
    DisposableEffect(glass, shapeId) {
        onDispose { glass?.removeShape(shapeId) }
    }
    SideEffect {
        val bounds = cardBounds ?: return@SideEffect
        glass?.updateShape(
            id = shapeId,
            centerX = bounds.centerX,
            centerY = bounds.centerY,
            halfWidth = bounds.halfWidth * scale,
            halfHeight = bounds.halfHeight * scale,
            cornerRadius = cardRadiusPx * scale,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { dismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                // Antes del `graphicsLayer`, así que estas coordenadas son las del layout,
                // sin la escala. Es lo que hace falta: al escalar sobre el centro, el
                // centro no se mueve y solo hay que encoger el tamaño.
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    cardBounds = GlassBounds(
                        centerX = position.x + coordinates.size.width / 2f,
                        centerY = position.y + coordinates.size.height / 2f,
                        halfWidth = coordinates.size.width / 2f,
                        halfHeight = coordinates.size.height / 2f,
                    )
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = cardAlpha.value,
                    transformOrigin = TransformOrigin.Center
                )
                .clip(cardShape)
                .then(
                    if (glassActive) {
                        Modifier.background(glassTint)
                    } else {
                        Modifier.hazeEffect(state = hazeState) {
                            blurRadius = DialogBlurRadius
                            backgroundColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                            tints = listOf(HazeTint(hazeTint))
                        }
                    }
                )
                .border(1.dp, borderBrush, cardShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { }
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content(dismiss)
        }
    }
}

/** Icono de los diálogos de día: entra después que la tarjeta y con rebote. */
@Composable
fun DayDialogIcon() {
    val iconScale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        delay(160)
        iconScale.animateTo(
            targetValue = 1.18f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessHigh
            )
        )
        iconScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }

    Box(
        modifier = Modifier
            .size(80.dp)
            .graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            }
            .clip(SquircleShape(48.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_calendar),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(40.dp)
        )
    }
}

/** Rectángulo de la tarjeta según el layout, antes de aplicarle la escala del pop. */
internal data class GlassBounds(
    val centerX: Float,
    val centerY: Float,
    val halfWidth: Float,
    val halfHeight: Float,
)
