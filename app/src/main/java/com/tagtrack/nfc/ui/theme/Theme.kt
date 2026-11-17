package com.tagtrack.nfc.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val BadgeCornerRadius = 4.dp
val CardCornerRadius = 6.dp
val TileCornerRadius = 6.dp

private val LightColorScheme = lightColorScheme(
    primary = BlueBronze,
    onPrimary = Surface,
    primaryContainer = GoldSoft,
    onPrimaryContainer = GoldText,
    secondary = Gold,
    onSecondary = Navy,
    secondaryContainer = Tile,
    onSecondaryContainer = Ink,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Tile,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Red,
    onError = Surface
)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Navy,
    primaryContainer = GoldText,
    onPrimaryContainer = GoldSoft,
    secondary = BlueBronze,
    onSecondary = Surface,
    secondaryContainer = Navy,
    onSecondaryContainer = Gold,
    background = Navy,
    onBackground = Surface,
    surface = Ink,
    onSurface = Surface,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Muted,
    outline = Color(0xFF404040),
    error = Red,
    onError = Surface
)

@Composable
fun TagtrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
