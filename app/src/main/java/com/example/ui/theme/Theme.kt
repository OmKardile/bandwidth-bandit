package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 1. Cyber Turquoise / Slate (Default Neon Blue/Cyan)
private val CyberTurquoiseDark = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color(0xFF00373D),
    primaryContainer = Color(0xFF004E57),
    onPrimaryContainer = Color(0xFFE0F7FA),
    
    secondary = Color(0xFF2979FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF0044C9),
    onSecondaryContainer = Color(0xFFECF0FF),
    
    tertiary = Color(0xFFB15EFF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5317B1),
    onTertiaryContainer = Color(0xFFFAEDFF),
    
    background = Color(0xFF0C101B),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF161E2E),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    error = Color(0xFFFF6D00),
    onError = Color.White
)

private val CyberTurquoiseLight = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF155E75),
    
    tertiary = Color(0xFF8E24AA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFD84315),
    onError = Color.White
)

// 2. Sunset Retro (Vivid Pink/Orange Sunset Neon)
private val SunsetRetroDark = darkColorScheme(
    primary = Color(0xFFFF5252),
    onPrimary = Color(0xFF4A000A),
    primaryContainer = Color(0xFF8B001D),
    onPrimaryContainer = Color(0xFFFFDAD9),
    
    secondary = Color(0xFFFF7A00),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF703200),
    onSecondaryContainer = Color(0xFFFFDBC6),
    
    tertiary = Color(0xFFFF0D87),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF8A0041),
    onTertiaryContainer = Color(0xFFFFDDE6),
    
    background = Color(0xFF12080D),
    onBackground = Color(0xFFFCEFF1),
    surface = Color(0xFF1E1019),
    onSurface = Color(0xFFFCEFF1),
    surfaceVariant = Color(0xFF2E1725),
    onSurfaceVariant = Color(0xFFD3BCC7),
    error = Color(0xFFFF3333),
    onError = Color.White
)

private val SunsetRetroLight = lightColorScheme(
    primary = Color(0xFFD81B60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E2),
    onPrimaryContainer = Color(0xFF3F001C),
    
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBC6),
    onSecondaryContainer = Color(0xFF330D00),
    
    tertiary = Color(0xFF9C27B0),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = Color(0xFF4A0054),
    
    background = Color(0xFFFFF5F5),
    onBackground = Color(0xFF26181D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF26181D),
    surfaceVariant = Color(0xFFF9EAEF),
    onSurfaceVariant = Color(0xFF847278),
    error = Color(0xFFC62828),
    onError = Color.White
)

// 3. Forest Emerald (Cyber Hacker Green)
private val ForestEmeraldDark = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF003913),
    primaryContainer = Color(0xFF00531F),
    onPrimaryContainer = Color(0xFF90FFAD),
    
    secondary = Color(0xFF00B0FF),
    onSecondary = Color(0xFF003550),
    secondaryContainer = Color(0xFF004C71),
    onSecondaryContainer = Color(0xFFC6E7FF),
    
    tertiary = Color(0xFF76FF03),
    onTertiary = Color(0xFF1F4E00),
    tertiaryContainer = Color(0xFF2F7500),
    onTertiaryContainer = Color(0xFFC8FF94),
    
    background = Color(0xFF080D0A),
    onBackground = Color(0xFFE1ECE4),
    surface = Color(0xFF121C16),
    onSurface = Color(0xFFE1ECE4),
    surfaceVariant = Color(0xFF1B2C23),
    onSurfaceVariant = Color(0xFF9EB4A4),
    error = Color(0xFFFFAB00),
    onError = Color.Black
)

private val ForestEmeraldLight = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF002105),
    
    secondary = Color(0xFF006064),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCEF5F7),
    onSecondaryContainer = Color(0xFF001F21),
    
    tertiary = Color(0xFF33691E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCEDC8),
    onTertiaryContainer = Color(0xFF0D2000),
    
    background = Color(0xFFF4F9F6),
    onBackground = Color(0xFF0E1A11),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0E1A11),
    surfaceVariant = Color(0xFFE0ECE2),
    onSurfaceVariant = Color(0xFF55645A),
    error = Color(0xFFB71C1C),
    onError = Color.White
)

// 4. Royal Amethyst (Deep Magic Purple)
private val RoyalAmethystDark = darkColorScheme(
    primary = Color(0xFFC54BFF),
    onPrimary = Color(0xFF4B0072),
    primaryContainer = Color(0xFF6B009F),
    onPrimaryContainer = Color(0xFFFAEEFF),
    
    secondary = Color(0xFF00E5FF),
    onSecondary = Color(0xFF00373D),
    secondaryContainer = Color(0xFF004E57),
    onSecondaryContainer = Color(0xFFE0F7FA),
    
    tertiary = Color(0xFF7000FF),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF3F00A6),
    onTertiaryContainer = Color(0xFFEDDFFF),
    
    background = Color(0xFF07040E),
    onBackground = Color(0xFFECE4F8),
    surface = Color(0xFF110B22),
    onSurface = Color(0xFFECE4F8),
    surfaceVariant = Color(0xFF1C1335),
    onSurfaceVariant = Color(0xFFB1A4CE),
    error = Color(0xFFFF3D00),
    onError = Color.White
)

private val RoyalAmethystLight = lightColorScheme(
    primary = Color(0xFF6A0DAD),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3E5F5),
    onPrimaryContainer = Color(0xFF3F006B),
    
    secondary = Color(0xFF0288D1),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE1F5FE),
    onSecondaryContainer = Color(0xFF00293F),
    
    tertiary = Color(0xFFAB47BC),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8E6FA),
    onTertiaryContainer = Color(0xFF4B0059),
    
    background = Color(0xFFF7F4FD),
    onBackground = Color(0xFF1C0D2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C0D2B),
    surfaceVariant = Color(0xFFEAE2F3),
    onSurfaceVariant = Color(0xFF6D5D7C),
    error = Color(0xFFD32F2F),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    paletteName: String = "Cyber Turquoise",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (paletteName) {
        "Sunset Retro" -> if (darkTheme) SunsetRetroDark else SunsetRetroLight
        "Forest Emerald" -> if (darkTheme) ForestEmeraldDark else ForestEmeraldLight
        "Royal Amethyst" -> if (darkTheme) RoyalAmethystDark else RoyalAmethystLight
        else -> if (darkTheme) CyberTurquoiseDark else CyberTurquoiseLight // "Cyber Turquoise"
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
