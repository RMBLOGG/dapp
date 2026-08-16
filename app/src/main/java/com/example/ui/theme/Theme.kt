package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DappColorScheme = darkColorScheme(
    primary = SignalBlue,
    onPrimary = FogWhite,
    primaryContainer = ElevatedColor,
    onPrimaryContainer = FogWhite,
    secondary = PulseTeal,
    onSecondary = GraphiteVoid,
    secondaryContainer = PanelColor,
    onSecondaryContainer = PulseTeal,
    tertiary = PulseTealVariant,
    onTertiary = GraphiteVoid,
    background = GraphiteVoid,
    onBackground = FogWhite,
    surface = PanelColor,
    onSurface = FogWhite,
    surfaceVariant = ElevatedColor,
    onSurfaceVariant = SlateText,
    outline = BorderDark,
    outlineVariant = SlateMuted,
    error = ErrorRed,
    onError = FogWhite
)

@Composable
fun DappTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GraphiteVoid.toArgb()
            window.navigationBarColor = GraphiteVoid.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = DappColorScheme,
        typography = DappTypography,
        content = content
    )
}
