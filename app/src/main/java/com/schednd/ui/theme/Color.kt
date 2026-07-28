package com.schednd.ui.theme

import androidx.compose.ui.graphics.Color

// Trade Republic inspired — monochrome, minimal, no accent blue
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

/** El blanco puro de [LightSurface] se pierde sobre el fondo claro: la barra inferior
 *  necesita un tono algo más oscuro para que se lea su silueta y el hueco de la bolita. */
val LightBottomBar = Color(0xFFE3E3E9)

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
