package com.schednd.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta monocroma, minimal, sin azul de acento
// Primary actions are white/black depending on theme

// Light mode
//
// El modo claro es el reflejo del oscuro, y eso empieza por dejarle sitio al filo. En
// oscuro nada es negro puro: el fondo es casi negro, la tarjeta un gris que se levanta de
// él y el filo blanco encima. Aquí igual, con los mismos escalones —tarjeta +17 puntos
// sobre el fondo, hero +13, barra +10— y el blanco reservado para el canto.
//
// Por eso el fondo baja a un gris de verdad y ninguna superficie es blanca. Antes el fondo
// era casi blanco: no quedaba recorrido por arriba, así que unas piezas se levantaban
// aclarándose y otras oscureciéndose, y el filo blanco sobre tarjeta blanca no se veía.
val LightBackground = Color(0xFFE2E2EB)

/** Superficie de nivel tarjeta. Comparte tono con [LightCardSurface]: lo que se apoya en
 *  una tarjeta —las cajas de la cuenta atrás— tiene que quedarse a su altura, no saltar a
 *  blanco por encima de ella. En oscuro pasa lo mismo, y por eso la cuenta atrás se lee
 *  como números sobre el hero y no como tres cajas. */
val LightSurface = Color(0xFFF3F3F9)
val LightSurfaceVariant = Color(0xFFEEEEEE)
val LightOnSurface = Color(0xFF111111)
val LightOnSurfaceVariant = Color(0xFF6E6E73)
val LightOutline = Color(0xFFD8D8D8)
val LightPrimary = Color(0xFF111111)
val LightOnPrimary = Color(0xFFFFFFFF)

/** Relleno de las tarjetas. Es el escalón más alto, pero no llega a blanco: el blanco es
 *  del filo. Equivale al gris de la tarjeta en oscuro. */
val LightCardSurface = Color(0xFFF3F3F9)

/** Superficies que se apoyan directamente sobre [LightBackground]: barra inferior y
 *  cabecera de la cuadrícula de disponibilidad, que deben leerse con el mismo peso.
 *  Es el escalón más bajo, como la barra en oscuro. */
val LightRaisedSurface = Color(0xFFECECF4)

/** Tarjeta de próxima sesión. Se queda por debajo de la tarjeta, igual que en oscuro el
 *  hero se queda por debajo de ella. */
val LightHeroSurface = Color(0xFFEFEFF6)

/** Hueco vacío de las cuadrículas de disponibilidad. Tiene que leerse como celda sin
 *  marcar, no como fondo, así que va bastante más oscuro que [LightRaisedSurface]. */
val LightEmptyCell = Color(0xFFD1D1DC)

// Dark mode
val DarkBackground = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A1A)
val DarkSurfaceVariant = Color(0xFF252525)
val DarkOnSurface = Color(0xFFF5F5F5)
val DarkOnSurfaceVariant = Color(0xFF8A8A8E)
val DarkOutline = Color(0xFF333333)
val DarkPrimary = Color(0xFFFFFFFF)
val DarkOnPrimary = Color(0xFF111111)

// Tier colors — muted, not screaming
val TierFull = Color(0xFF2DC653)
val TierViable = Color(0xFFE8A317)
val TierLimited = Color(0xFFD4712A)
val TierInsufficient = Color(0xFFD43030)

// Tag colors — para las etiquetas de notas
val TagTrama = Color(0xFF7C5CE5)
val TagLoot = Color(0xFFE8A317)
val TagNpc = Color(0xFF1A95FF)
val TagPersonaje = Color(0xFF2DC653)
val TagOtros = Color(0xFF8A8A8E)

// Avatar palette — muted, desaturated tones
val AvatarColors = listOf(
    Color(0xFF6B6B8D), // muted indigo
    Color(0xFF5A8F8B), // muted teal
    Color(0xFF8B6B8D), // muted purple
    Color(0xFF5A7A99), // muted blue
    Color(0xFF5A8B5E), // muted green
    Color(0xFFA08050), // muted amber
    Color(0xFF9E6060), // muted rose
    Color(0xFF5A8A9E), // muted cyan
)
