package de.thkoeln.codescope.theme


import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    
    secondary = Success,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5), // Light Green for feedback
    onSecondaryContainer = Color(0xFF047857), // Dark Green for feedback text
    
    tertiary = Warning,
    onTertiary = Color.White,
    tertiaryContainer = WarningLight,
    onTertiaryContainer = WarningDark,
    
    error = Error,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = TextSecondary,
    
    outline = Border,
    outlineVariant = Color(0xFFE5E7EB) // Original track color
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = PrimaryLight,
    
    secondary = SuccessLight,
    onSecondary = Color.White,
    secondaryContainer = SuccessDark,
    onSecondaryContainer = Color.White, // White text for feedback in dark mode

    tertiary = WarningLight,
    onTertiary = Color.White,
    tertiaryContainer = WarningDark,
    onTertiaryContainer = WarningLight,
    
    error = ErrorLight,
    onError = Color.White,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    
    outline = BorderDark,
    outlineVariant = Border
)

@Composable
fun CodeScopeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
