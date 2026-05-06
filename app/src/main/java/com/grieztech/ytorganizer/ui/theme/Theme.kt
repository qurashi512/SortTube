package com.grieztech.ytorganizer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ═══════════════════════════════════════
//  GriezTech - App Theme
// ═══════════════════════════════════════

private val DarkColorScheme = darkColorScheme(
    primary          = DarkPrimary,
    onPrimary        = DarkOnPrimary,
    secondary        = DarkSecondary,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurfaceVar,
    onBackground     = DarkOnBackground,
    onSurface        = DarkOnSurface,
    error            = ErrorColor,
)

private val LightColorScheme = lightColorScheme(
    primary          = LightPrimary,
    onPrimary        = LightOnPrimary,
    secondary        = LightSecondary,
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightSurfaceVar,
    onBackground     = LightOnBackground,
    onSurface        = LightOnSurface,
    error            = ErrorColor,
)

// ── Custom Glass Colors passed down the tree ──
data class GlassColors(
    val panel : Color,
    val border: Color,
    val gradientStart: Color,
    val gradientMid  : Color,
    val gradientEnd  : Color,
)

val LocalGlassColors = compositionLocalOf {
    GlassColors(
        panel        = DarkGlassPanel,
        border       = DarkGlassBorder,
        gradientStart = GradientStart,
        gradientMid  = GradientMid,
        gradientEnd  = GradientEnd,
    )
}

@Composable
fun GriezTechTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val glassColors = if (darkTheme) {
        GlassColors(
            panel        = DarkGlassPanel,
            border       = DarkGlassBorder,
            gradientStart = GradientStart,
            gradientMid  = GradientMid,
            gradientEnd  = GradientEnd,
        )
    } else {
        GlassColors(
            panel        = LightGlassPanel,
            border       = LightGlassBorder,
            gradientStart = LightGradientStart,
            gradientMid  = LightGradientMid,
            gradientEnd  = LightGradientEnd,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor  = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = GriezTechTypography,
            content     = content
        )
    }
}
