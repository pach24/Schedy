package com.schednd.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.schednd.presentation.common.UiError
import com.schednd.ui.theme.FullRoundShape
import com.schednd.ui.theme.pressScale

/**
 * Lo que salió mal, con la salida al lado.
 *
 * Es una tarjeta más del listado en vez de un texto rojo suelto: casi todo lo que falla
 * aquí es pasajero, y lo único que hace falta es poder volver a intentarlo o quitárselo
 * de en medio.
 *
 * @param actionLabel qué pone el botón. Sin él —y sin [onAction]— el aviso es solo un
 *   aviso, que es lo que toca cuando no hay nada que reintentar.
 */
@Composable
fun ErrorNotice(
    error: UiError,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    GenCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 16.dp,
                    top = 14.dp,
                    end = if (actionLabel != null) 10.dp else 16.dp,
                    bottom = 14.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(error.messageRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.width(8.dp))
                val interaction = remember { MutableInteractionSource() }
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .pressScale(interaction)
                        .clip(FullRoundShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .clickable(
                            indication = LocalIndication.current,
                            interactionSource = interaction,
                            onClick = onAction
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}
