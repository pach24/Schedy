package com.schednd.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.schednd.ui.components.GenCard
import com.schednd.ui.components.SkeletonBlock
import com.schednd.ui.theme.SquircleShape

/**
 * Los huecos de la pantalla de inicio, cada uno con la silueta y el sitio exacto del dato
 * que va a caer ahí.
 *
 * Que coincidan no es coquetería: [com.schednd.ui.theme.SkeletonMorph] estira la caja del
 * alto del hueco al del contenido, y cuanto menos tenga que estirarse más se lee el cambio
 * como una pieza que se concreta y menos como dos cosas distintas intercambiadas. Por eso
 * los altos van copiados del interlineado real —13 para `labelSmall`, 20 para `bodyMedium`,
 * 40 y 44 para las dos líneas de la fecha— y no redondeados a ojo.
 */

/** Barra centrada en el interlineado de la línea de texto que sustituye. */
@Composable
private fun SkeletonLine(
    widthFraction: Float,
    lineHeight: Dp,
    barHeight: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(lineHeight),
        contentAlignment = Alignment.CenterStart
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(barHeight)
        )
    }
}

/**
 * El saludo de la cabecera. El título de debajo no espera a nadie y se pinta ya.
 *
 * Ancho fijo y no una fracción: al lado del título no hay nada más, así que el hueco se
 * mide contra el saludo que va a caer ahí, no contra la pantalla.
 */
@Composable
internal fun GreetingSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        SkeletonBlock(
            modifier = Modifier
                .width(124.dp)
                .height(12.dp)
        )
    }
}

/**
 * Las tripas de la tarjeta de próxima sesión. La tarjeta en sí —relleno, filo, redondeo— se
 * queda fuera del cambio y no se mueve: es el marco dentro del que se concreta todo lo demás.
 */
@Composable
internal fun HeroSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            SkeletonLine(widthFraction = 0.32f, lineHeight = 13.dp, barHeight = 10.dp)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonLine(widthFraction = 0.62f, lineHeight = 40.dp, barHeight = 30.dp)
            SkeletonLine(widthFraction = 0.44f, lineHeight = 44.dp, barHeight = 30.dp)
        }
        SkeletonLine(widthFraction = 0.5f, lineHeight = 20.dp, barHeight = 13.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(3) {
                CountdownBoxSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Caja de la cuenta atrás vacía. Conserva el relleno y el redondeo de la de verdad, así que
 * al llegar los datos lo único que cambia son los números: la caja ya estaba ahí.
 */
@Composable
private fun CountdownBoxSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(SquircleShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.height(41.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(44.dp)
                    .height(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier.height(13.dp),
            contentAlignment = Alignment.Center
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .width(30.dp)
                    .height(9.dp)
            )
        }
    }
}

/**
 * Fila de sesión vacía. Va dentro de una [GenCard] de verdad para que la tarjeta —relleno y
 * filo— no tenga que aparecer después: lo que llega es el texto, no el soporte.
 *
 * @param leadingSize hueco de la pieza de la izquierda: el dado del listado, el punto del
 *   calendario. A cero no se reserva ninguno, que es lo que quiere la fila de resumen.
 */
@Composable
internal fun SessionRowSkeleton(
    modifier: Modifier = Modifier,
    leadingSize: Dp = 26.dp,
    titleFraction: Float = 0.5f,
    subtitleFraction: Float = 0.3f
) {
    GenCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingSize > 0.dp) {
                SkeletonBlock(
                    modifier = Modifier.size(leadingSize),
                    shape = SquircleShape(leadingSize / 3f)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(
                    widthFraction = titleFraction,
                    lineHeight = 20.dp,
                    barHeight = 13.dp
                )
                SkeletonLine(
                    widthFraction = subtitleFraction,
                    lineHeight = 18.dp,
                    barHeight = 11.dp
                )
            }
        }
    }
}

/** Encabezado de sección: el rótulo a la izquierda y la cuenta a la derecha. */
@Composable
internal fun SectionHeaderSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.28f)
                    .height(11.dp)
            )
        }
        SkeletonBlock(
            modifier = Modifier
                .width(14.dp)
                .height(11.dp)
        )
    }
}

/**
 * El bloque de sesiones de la pestaña Sesiones: dos secciones con sus filas.
 *
 * Tres filas y una, y no las que vaya a haber de verdad, porque eso todavía no se sabe.
 * Es una silueta plausible, lo justo para que el listado tenga cuerpo mientras llega.
 */
@Composable
internal fun SessionsTabSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        SessionRowSkeleton(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            titleFraction = 0.52f,
            subtitleFraction = 0.34f
        )
        SessionRowSkeleton(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            titleFraction = 0.4f,
            subtitleFraction = 0.28f
        )
        SessionRowSkeleton(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            titleFraction = 0.46f,
            subtitleFraction = 0.3f
        )
        SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
        SessionRowSkeleton(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            titleFraction = 0.44f,
            subtitleFraction = 0.26f
        )
    }
}

/** El bloque de fechas confirmadas de la pestaña Calendario. */
@Composable
internal fun CalendarListSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeaderSkeleton(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        repeat(2) { index ->
            SessionRowSkeleton(
                // Allí la pieza de la izquierda es el punto verde, no el dado.
                leadingSize = 10.dp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                titleFraction = if (index == 0) 0.5f else 0.38f,
                subtitleFraction = if (index == 0) 0.3f else 0.24f
            )
        }
    }
}
