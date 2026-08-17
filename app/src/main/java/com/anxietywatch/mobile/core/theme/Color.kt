package com.anxietywatch.mobile.core.theme

import androidx.compose.ui.graphics.Color

// Tokens exactos del sistema de diseño "Serene Oversight" (stitch_anxietywatch_monitoring_system.zip
// → serene_oversight/DESIGN.md). Mapean 1:1 a los roles de Material 3 ColorScheme, así que no hay
// que reinterpretar nada: esto ES la paleta que ya usan las 16 pantallas del prototipo.

val md_theme_light_primary = Color(0xFF6A5C46)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFD0BEA3)
val md_theme_light_onPrimaryContainer = Color(0xFF594D37)

val md_theme_light_secondary = Color(0xFF525F75)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFD5E3FD)
val md_theme_light_onSecondaryContainer = Color(0xFF57657B)

val md_theme_light_tertiary = Color(0xFF5E604D) // sage green — estados "normal"/positivos
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFC1C2AB)
val md_theme_light_onTertiaryContainer = Color(0xFF4E503E)

val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onErrorContainer = Color(0xFF93000A)

val md_theme_light_background = Color(0xFFFCF9F8)
val md_theme_light_onBackground = Color(0xFF1B1B1C)
val md_theme_light_surface = Color(0xFFFCF9F8)
val md_theme_light_onSurface = Color(0xFF1B1B1C)
val md_theme_light_surfaceVariant = Color(0xFFE5E2E1)
val md_theme_light_onSurfaceVariant = Color(0xFF4C463D)

val md_theme_light_outline = Color(0xFF7D766C)
val md_theme_light_outlineVariant = Color(0xFFCFC5B9)

val md_theme_light_surfaceContainerLowest = Color(0xFFFFFFFF)
val md_theme_light_surfaceContainerLow = Color(0xFFF6F3F2)
val md_theme_light_surfaceContainer = Color(0xFFF0EDED)
val md_theme_light_surfaceContainerHigh = Color(0xFFEAE7E7)
val md_theme_light_surfaceContainerHighest = Color(0xFFE5E2E1)

val md_theme_light_inverseSurface = Color(0xFF303030)
val md_theme_light_inverseOnSurface = Color(0xFFF3F0EF)
val md_theme_light_inversePrimary = Color(0xFFD6C4A9)

// Modo oscuro: la app del paciente lo requiere de forma explícita en el PDF/backlog
// ("modo oscuro profundo para reducir la fatiga visual"). Derivado del mismo sistema tonal,
// invirtiendo la relación superficie/contenido y manteniendo el mismo hue de acento.
val md_theme_dark_primary = Color(0xFFD6C4A9)
val md_theme_dark_onPrimary = Color(0xFF3B2F1B)
val md_theme_dark_primaryContainer = Color(0xFF594D37)
val md_theme_dark_onPrimaryContainer = Color(0xFFF3E0C4)

val md_theme_dark_secondary = Color(0xFFB9C7E0)
val md_theme_dark_onSecondary = Color(0xFF243447)
val md_theme_dark_secondaryContainer = Color(0xFF3A475C)
val md_theme_dark_onSecondaryContainer = Color(0xFFD5E3FD)

val md_theme_dark_tertiary = Color(0xFFC7C8B1)
val md_theme_dark_onTertiary = Color(0xFF2F3221)
val md_theme_dark_tertiaryContainer = Color(0xFF464837)
val md_theme_dark_onTertiaryContainer = Color(0xFFE3E4CC)

val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)

val md_theme_dark_background = Color(0xFF121212)
val md_theme_dark_onBackground = Color(0xFFE5E2E1)
val md_theme_dark_surface = Color(0xFF121212)
val md_theme_dark_onSurface = Color(0xFFE5E2E1)
val md_theme_dark_surfaceVariant = Color(0xFF4C463D)
val md_theme_dark_onSurfaceVariant = Color(0xFFCFC5B9)

val md_theme_dark_outline = Color(0xFF988F82)
val md_theme_dark_outlineVariant = Color(0xFF4C463D)

val md_theme_dark_surfaceContainerLowest = Color(0xFF0D0D0D)
val md_theme_dark_surfaceContainerLow = Color(0xFF1B1B1C)
val md_theme_dark_surfaceContainer = Color(0xFF1F1F1F)
val md_theme_dark_surfaceContainerHigh = Color(0xFF2A2A2A)
val md_theme_dark_surfaceContainerHighest = Color(0xFF353535)

val md_theme_dark_inverseSurface = Color(0xFFE5E2E1)
val md_theme_dark_inverseOnSurface = Color(0xFF303030)
val md_theme_dark_inversePrimary = Color(0xFF6A5C46)
