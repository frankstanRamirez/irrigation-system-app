package com.irrigationsystem.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Fondo = Color(0xFF0D1712)
val Superficie = Color(0xFF14231B)
val Linea = Color(0xFF21352A)
val Texto = Color(0xFFE8F2EC)
val Texto2 = Color(0xFF8AA596)
val Verde = Color(0xFF3DDC84)
val Azul = Color(0xFF38BDF8)
val Ambar = Color(0xFFF5B041)
val Rojo = Color(0xFFEF5350)

@Composable
fun IrrigationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Verde,
            onPrimary = Color(0xFF06210F),
            secondary = Azul,
            background = Fondo,
            onBackground = Texto,
            surface = Superficie,
            onSurface = Texto,
            surfaceVariant = Linea,
            onSurfaceVariant = Texto2,
            error = Rojo,
        ),
        content = content,
    )
}
