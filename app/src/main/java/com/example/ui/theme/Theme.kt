package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TerracottaPrimaryDark,
    onPrimary = Color(0xFF5D1900),
    primaryContainer = TerracottaContainerDark,
    onPrimaryContainer = Color(0xFFFFDBD0),
    secondary = GoldenSecondaryDark,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = GoldenContainerDark,
    onSecondaryContainer = Color(0xFFFFDEAC),
    background = SurfaceDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceCardDark,
    onBackground = TextLight,
    onSurface = TextLight,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = OnTerracottaContainer,
    secondary = GoldenSecondary,
    onSecondary = GoldenOnSecondary,
    secondaryContainer = GoldenContainer,
    onSecondaryContainer = OnGoldenContainer,
    tertiary = HerbTertiary,
    onTertiary = HerbOnTertiary,
    tertiaryContainer = HerbContainer,
    onTertiaryContainer = OnHerbContainer,
    background = SurfaceWarmLight,
    surface = SurfaceWarmLight,
    surfaceVariant = SurfaceCardLight,
    onBackground = TextDark,
    onSurface = TextDark,
    outline = OutlineWarm
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our handcrafted culinary palette for consistency
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
