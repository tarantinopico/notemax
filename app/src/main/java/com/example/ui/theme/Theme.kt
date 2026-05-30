package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.background
import com.example.data.InterfaceDensity
import com.example.data.ThemeMode

data class VisualEffects(val glassmorphism: Boolean, val uiTransparency: Boolean)
val LocalVisualEffects = staticCompositionLocalOf { VisualEffects(false, false) }

fun Color.toHex(): String {
    val r = (this.red * 255).toInt()
    val g = (this.green * 255).toInt()
    val b = (this.blue * 255).toInt()
    return String.format("%02X%02X%02X", r, g, b)
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    alpha: Float = 0.65f,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val effects = LocalVisualEffects.current
    androidx.compose.foundation.layout.Box(modifier = modifier, contentAlignment = contentAlignment) {
        if (effects.glassmorphism) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .blur(20.dp, androidx.compose.ui.draw.BlurredEdgeTreatment.Unbounded)
                    .background(color.copy(alpha = alpha))
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(color.copy(alpha = alpha * 0.7f))
            )
        } else {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(shape)
                    .background(color.copy(alpha = if (effects.uiTransparency) alpha else 1f))
            )
        }
        content()
    }
}

private val LightColors = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant
)

private val DeepDarkColors = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    background = Color(0xFF000000), // OLED Pure Black
    onBackground = md_theme_dark_onBackground,
    surface = Color(0xFF000000), // OLED Pure Black
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = md_theme_dark_onSurfaceVariant
)

val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    useDynamicColor: Boolean = true,
    interfaceDensity: InterfaceDensity = InterfaceDensity.DEFAULT,
    visualEffects: VisualEffects = VisualEffects(false, false),
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DeepDarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            var window: android.view.Window? = null
            while (context is android.content.ContextWrapper) {
                if (context is Activity) {
                    window = context.window
                    break
                }
                context = context.baseContext
            }
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    val densityMultiplier = when (interfaceDensity) {
        InterfaceDensity.COZY -> 1.15f
        InterfaceDensity.DEFAULT -> 1.0f
        InterfaceDensity.COMPACT -> 0.85f
    }
    
    val defaultDensity = LocalDensity.current
    val customDensity = Density(
        density = defaultDensity.density * densityMultiplier,
        fontScale = defaultDensity.fontScale * densityMultiplier
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes
    ) {
        CompositionLocalProvider(
            LocalDensity provides customDensity,
            LocalVisualEffects provides visualEffects,
            content = content
        )
    }
}
