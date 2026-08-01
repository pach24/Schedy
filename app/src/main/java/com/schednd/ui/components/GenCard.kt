package com.schednd.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.schednd.ui.theme.CardShape
import com.schednd.ui.theme.LightCardSurface
import com.schednd.ui.theme.LightHeroSurface
import com.schednd.ui.theme.LightRaisedSurface

/**
 * Brillo del filo: una luz que entra por arriba y se apaga hacia abajo.
 *
 * Hace de rim light donde no puede haber cristal. Las piezas del shader tienen que
 * dibujarse fuera del contenido que refractan, así que todo lo que vive dentro de la
 * pantalla —tarjetas, listados— se queda sin él y lo suple con este filo pintado.
 *
 * Es blanco en los dos modos, y por eso ninguna superficie puede ser blanca: un filo
 * blanco sobre una tarjeta blanca no existe. La tarjeta es siempre un tono intermedio y el
 * blanco se reserva para el canto, igual que en oscuro la tarjeta es gris y el filo blanco.
 */
@Composable
fun rimHighlightBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    return Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (isDark) 0.22f else 1f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.35f)
        )
    )
}

/**
 * Relleno del widget de próxima sesión. Vive aquí, junto al filo, porque lo comparte todo
 * lo que tenga que parecerse a él: si se copiara a mano acabarían separándose.
 */
@Composable
fun heroSurfaceColor(): Color =
    if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    } else {
        LightHeroSurface
    }

/**
 * Superficie que se apoya en el fondo sin llegar a ser tarjeta: el escalón más bajo, el
 * mismo que la barra inferior.
 *
 * En oscuro sale de velar `surfaceVariant`, pero esa cuenta no se puede repetir en claro:
 * `surfaceVariant` sirve a la vez de caja levantada sobre el fondo y de hueco hundido
 * dentro de una tarjeta, y en claro esos dos piden direcciones contrarias. Velado sobre el
 * fondo claro se queda a un punto de él y la caja desaparece.
 */
@Composable
fun raisedSurfaceColor(): Color =
    if (isSystemInDarkTheme()) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        LightRaisedSurface
    }

/**
 * @param onClick si se pasa, la tarjeta responde al toque. Conviene dárselo a ella en vez
 *   de encadenar un `clickable` en el `modifier`: el toque tiene que ir por dentro del
 *   recorte, o el ripple se pinta sobre el rectángulo completo y asoma por las esquinas.
 * @param interactionSource compártelo con `pressScale` para que el hundido y el ripple
 *   respondan a la misma pulsación.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GenCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    // En claro no es blanca: el blanco es del filo. Ver [rimHighlightBrush].
    val fillColor = if (isDark) Color(0xFF27272A).copy(alpha = 0.72f) else LightCardSurface
    // Sin vibración, la pulsación larga no se distingue de una pulsación que no ha hecho
    // nada: el aviso llega cuando ya se ha cumplido el tiempo, no al levantar el dedo.
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(fillColor)
            .border(1.dp, rimHighlightBrush(), CardShape)
            .then(
                if (onClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onLongClick = onLongClick?.let { longClick ->
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                longClick()
                            }
                        },
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}
