package com.example.walletwise.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = Primary500,
    onPrimary = Neutral0,

    primaryContainer = Primary100,
    onPrimaryContainer = Primary900,

    secondary = Accent500,
    onSecondary = Neutral900,

    secondaryContainer = Accent100,
    onSecondaryContainer = Accent700,

    error = ExpenseRed,
    onError = Neutral0,

    background = BackgroundLight,
    surface = SurfaceLight,

    onSurface = Neutral900,
    onSurfaceVariant = Neutral700,
)

private val DarkColors = darkColorScheme(
    primary = Primary500,
    onPrimary = Neutral0,

    primaryContainer = Primary700,
    onPrimaryContainer = Primary50,

    secondary = Accent500,
    onSecondary = Neutral900,

    secondaryContainer = Accent700,
    onSecondaryContainer = Accent100,

    error = ExpenseRed,
    onError = Neutral0,

    background = BackgroundDark,
    surface = SurfaceDark,

    onSurface = Neutral0,
    onSurfaceVariant = Neutral300,
)

@Composable
fun ExpenceAndBudgetTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {

            val context = LocalContext.current

            if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.statusBarColor = colorScheme.primary.toArgb()

            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}