package com.schednd.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta monocroma, minimal, sin azul de acento
// Primary actions are white/black depending on theme

// Light mode
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEEEEEE)
val LightOnSurface = Color(0xFF111111)
val LightOnSurfaceVariant = Color(0xFF6E6E73)
val LightOutline = Color(0xFFD8D8D8)
val LightPrimary = Color(0xFF111111)
val LightOnPrimary = Color(0xFFFFFFFF)

/** Gris de las superficies que se apoyan directamente sobre [LightBackground]: el blanco
 *  de [LightSurface] y el [LightSurfaceVariant] del tema quedan a un par de puntos del
 *  fondo y no se recortan. Lo comparten la barra inferior y la cabecera de la cuadrícula
 *  de disponibilidad, que deben leerse con el mismo peso. */
val LightRaisedSurface = Color(0xFFE0E0E9)

/** Tarjeta de próxima sesión: mismo problema que [LightRaisedSurface]. */
val LightHeroSurface = Color(0xFFE4E4EA)

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
