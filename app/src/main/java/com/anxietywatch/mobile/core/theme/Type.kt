package com.anxietywatch.mobile.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// El sistema de diseño usa dos familias deliberadamente:
// - Manrope para títulos: look geométrico y profesional.
// - Atkinson Hyperlegible Next para cuerpo/etiquetas: máxima legibilidad para usuarios que
//   pueden estar en un momento de estrés alto o fatiga visual (razón explícita en el DESIGN.md).
//
// Para usarlas de verdad hay que añadir los archivos .ttf a app/src/main/res/font/ y
// declarar FontFamily(Font(R.font.manrope_bold, FontWeight.Bold), ...). Mientras tanto,
// FontFamily.Default asegura que la app compile y se vea correcta con la tipografía del
// sistema — cambia esta única línea por familia cuando tengas los .ttf.
private val ManropeFallback = FontFamily.Default
private val AtkinsonFallback = FontFamily.Default

val AnxietyWatchTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = ManropeFallback,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.02).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = ManropeFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = ManropeFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = AtkinsonFallback,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 28.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = AtkinsonFallback,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = AtkinsonFallback,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.01.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = AtkinsonFallback,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
