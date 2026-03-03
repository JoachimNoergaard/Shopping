package dk.joachim.shopping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Grey99,
    primaryContainer = Green90,
    onPrimaryContainer = Green10,
    secondary = Teal40,
    onSecondary = Grey99,
    secondaryContainer = Teal90,
    onSecondaryContainer = Grey10,
    error = Red40,
    errorContainer = Red90,
    onError = Grey99,
    onErrorContainer = Red40,
    background = BackgroundLight,
    onBackground = Grey10,
    surface = Grey99,
    onSurface = Grey10,
    surfaceVariant = Grey95,
    onSurfaceVariant = Grey20,
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = Green20,
    primaryContainer = Green30,
    onPrimaryContainer = Green90,
    secondary = Teal80,
    onSecondary = Grey10,
    secondaryContainer = Teal40,
    onSecondaryContainer = Teal90,
    error = Red80,
    errorContainer = Red40,
    onError = Grey10,
    onErrorContainer = Red90,
    background = BackgroundDark,
    onBackground = Grey90,
    surface = SurfaceDark,
    onSurface = Grey90,
    surfaceVariant = Grey20,
    onSurfaceVariant = Grey90,
)

@Composable
fun ShoppingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
