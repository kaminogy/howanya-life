package com.kamino.howanyalife.mobile.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Howanya's blue, as in the character artwork. */
val HowanyaBlue = Color(0xFF0A9BF5)
/** The yellow used on the official Howanya page and the launcher icon. */
val HowanyaYellow = Color(0xFFFFF100)

val FullnessColor = Color(0xFFFFA62B)
val EnergyColor = Color(0xFF4CC38A)
val MoodColor = Color(0xFFF2789A)

private val Colors = lightColorScheme(
    primary = HowanyaBlue,
    onPrimary = Color.White,
    secondary = HowanyaYellow,
    onSecondary = Color(0xFF3B3200),
    background = Color(0xFFFFFBEA),
    surface = Color.White,
)

@Composable
fun HowanyaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
