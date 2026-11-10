package com.dashabi.control.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 应用调色板。暗色与亮色各一套，组件统一从 [LocalDashaPalette] 读取，
 * 这样切换主题时所有自定义颜色一起变化。
 */
@Immutable
data class DashaPalette(
    val ink: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val line: Color,
    val amber: Color,
    val mint: Color,
    val sky: Color,
    val rose: Color,
    val violet: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val heroStart: Color,
    val onAccent: Color
)

val DarkPalette = DashaPalette(
    ink = Color(0xFF0A0D11),
    surface1 = Color(0xFF11161C),
    surface2 = Color(0xFF171F27),
    surface3 = Color(0xFF202A35),
    line = Color(0xFF25303D),
    amber = Color(0xFFF2A93B),
    mint = Color(0xFF3FD9A0),
    sky = Color(0xFF5B9DFF),
    rose = Color(0xFFFF6B6B),
    violet = Color(0xFFB48CFF),
    textPrimary = Color(0xFFE8EEF6),
    textSecondary = Color(0xFF98A6B8),
    textMuted = Color(0xFF6A7A8E),
    heroStart = Color(0xFF1C2632),
    onAccent = Color(0xFF171005)
)

val LightPalette = DashaPalette(
    ink = Color(0xFFF3F5F9),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF7F9FC),
    surface3 = Color(0xFFEEF2F7),
    line = Color(0xFFDCE3EC),
    amber = Color(0xFFB26A00),
    mint = Color(0xFF0B8F66),
    sky = Color(0xFF1F6FEB),
    rose = Color(0xFFD63C3C),
    violet = Color(0xFF7A4DD6),
    textPrimary = Color(0xFF10161E),
    textSecondary = Color(0xFF4C5868),
    textMuted = Color(0xFF7C8898),
    heroStart = Color(0xFFF4F8FD),
    onAccent = Color(0xFFFFFFFF)
)

val LocalDashaPalette = staticCompositionLocalOf { DarkPalette }

val Ink: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.ink

val Surface1: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.surface1

val Surface2: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.surface2

val Surface3: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.surface3

val Line: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.line

val Amber: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.amber

val Mint: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.mint

val Sky: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.sky

val Rose: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.rose

val Violet: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.violet

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.textMuted

/** 设备页顶部渐变卡的起始色。 */
val HeroStart: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.heroStart

/** 琥珀色按钮上的文字/图标颜色。 */
val OnAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.onAccent

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.amber,
    onPrimary = DarkPalette.onAccent,
    primaryContainer = Color(0xFF3B2A10),
    onPrimaryContainer = Color(0xFFFFD9A0),
    secondary = DarkPalette.mint,
    onSecondary = Color(0xFF04150F),
    secondaryContainer = Color(0xFF103024),
    onSecondaryContainer = Color(0xFFB6F5DC),
    tertiary = DarkPalette.sky,
    onTertiary = Color(0xFF04101F),
    tertiaryContainer = Color(0xFF12263F),
    onTertiaryContainer = Color(0xFFC9E0FF),
    background = DarkPalette.ink,
    onBackground = DarkPalette.textPrimary,
    surface = DarkPalette.surface1,
    onSurface = DarkPalette.textPrimary,
    surfaceVariant = DarkPalette.surface2,
    onSurfaceVariant = DarkPalette.textSecondary,
    surfaceTint = DarkPalette.amber,
    outline = DarkPalette.line,
    outlineVariant = Color(0xFF1C2632),
    error = DarkPalette.rose,
    onError = Color(0xFF1B0505),
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFFC9C9),
    scrim = Color(0xCC000000)
)

private val LightScheme = lightColorScheme(
    primary = LightPalette.amber,
    onPrimary = LightPalette.onAccent,
    primaryContainer = Color(0xFFFFE2B8),
    onPrimaryContainer = Color(0xFF3A2200),
    secondary = LightPalette.mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F5E4),
    onSecondaryContainer = Color(0xFF002117),
    tertiary = LightPalette.sky,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD8E6FF),
    onTertiaryContainer = Color(0xFF001B3D),
    background = LightPalette.ink,
    onBackground = LightPalette.textPrimary,
    surface = LightPalette.surface1,
    onSurface = LightPalette.textPrimary,
    surfaceVariant = LightPalette.surface2,
    onSurfaceVariant = LightPalette.textSecondary,
    surfaceTint = LightPalette.amber,
    outline = LightPalette.line,
    outlineVariant = Color(0xFFE7EDF4),
    error = LightPalette.rose,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0x99000000)
)

private val DashaTypography = Typography(
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.3.sp)
)

private val DashaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun DashaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val scheme = if (darkTheme) DarkScheme else LightScheme
    CompositionLocalProvider(LocalDashaPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DashaTypography,
            shapes = DashaShapes,
            content = content
        )
    }
}
