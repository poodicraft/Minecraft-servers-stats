package io.github.poodicraft.serverscope.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.poodicraft.serverscope.R

/** Block colors used for accents, pixel art and status indicators. */
object Mc {
    val Grass = Color(0xFF6CC349)
    val GrassLight = Color(0xFF7CC744)
    val GrassDark = Color(0xFF3F7F24)
    val Dirt = Color(0xFF7A5034)
    val DirtLight = Color(0xFF9A6A45)
    val DirtDark = Color(0xFF5A3A25)
    val Stone = Color(0xFF7D7D7D)
    val StoneDark = Color(0xFF4F4D4B)
    val Emerald = Color(0xFF41E07A)
    val Redstone = Color(0xFFFF5A4E)
    val Gold = Color(0xFFFFC233)
    val Diamond = Color(0xFF5CE1E6)
    val Lapis = Color(0xFF6F8CFF)
    val Amethyst = Color(0xFFB98CFF)
    val Obsidian = Color(0xFF0E0C10)
}

private val ServerScopeColors = darkColorScheme(
    primary = Mc.Grass,
    onPrimary = Color(0xFF0E1F06),
    primaryContainer = Color(0xFF2B4A1D),
    onPrimaryContainer = Color(0xFFC7F2B0),
    secondary = Color(0xFFC79A6B),
    onSecondary = Color(0xFF2A1A0E),
    secondaryContainer = Color(0xFF4A3222),
    onSecondaryContainer = Color(0xFFF2D9C2),
    tertiary = Mc.Diamond,
    onTertiary = Color(0xFF00363A),
    tertiaryContainer = Color(0xFF0F4A4E),
    onTertiaryContainer = Color(0xFFB8F6F8),
    background = Color(0xFF151413),
    onBackground = Color(0xFFE9E4DE),
    surface = Color(0xFF151413),
    onSurface = Color(0xFFE9E4DE),
    surfaceVariant = Color(0xFF3A3734),
    onSurfaceVariant = Color(0xFFC9C2BA),
    surfaceTint = Mc.Grass,
    surfaceContainerLowest = Color(0xFF0F0E0D),
    surfaceContainerLow = Color(0xFF1B1A18),
    surfaceContainer = Color(0xFF211F1D),
    surfaceContainerHigh = Color(0xFF2A2825),
    surfaceContainerHighest = Color(0xFF34312E),
    outline = Color(0xFF6B655F),
    outlineVariant = Color(0xFF45413D),
    error = Mc.Redstone,
    onError = Color(0xFF3B0000),
    errorContainer = Color(0xFF5C1A17),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE9E4DE),
    inverseOnSurface = Color(0xFF2A2825),
    inversePrimary = Mc.GrassDark,
    scrim = Color.Black,
)

/** "Press Start 2P" for headings; the system font keeps body text readable. */
val PixelFont = FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))

private fun pixel(size: Int, lineHeight: Int) =
    TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Normal, fontSize = size.sp, lineHeight = lineHeight.sp)

private val ServerScopeTypography = Typography().let { base ->
    base.copy(
        displaySmall = pixel(24, 34),
        headlineLarge = pixel(22, 32),
        headlineMedium = pixel(18, 28),
        headlineSmall = pixel(16, 24),
        titleLarge = pixel(14, 22),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Small pixel-font label style (section titles, badges). */
val PixelLabel = pixel(10, 16)

private val ServerScopeShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

@Composable
fun ServerScopeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ServerScopeColors,
        typography = ServerScopeTypography,
        shapes = ServerScopeShapes,
        content = content,
    )
}
