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
    ink = Color(0xFF11171D),
    surface1 = Color(0xFF1A222B),
    surface2 = Color(0xFF202B35),
    surface3 = Color(0xFF2A3641),
    line = Color(0xFF3A4753),
    amber = Color(0xFF7EDBC5),
    mint = Color(0xFF7EDBC5),
    sky = Color(0xFF9DBCD7),
    rose = Color(0xFFF09B91),
    violet = Color(0xFFB7BACB),
    textPrimary = Color(0xFFEDF2F7),
    textSecondary = Color(0xFFBAC6D1),
    textMuted = Color(0xFF91A2B2),
    heroStart = Color(0xFF1D3437),
    onAccent = Color(0xFF102A25)
)

val LightPalette = DashaPalette(
    ink = Color(0xFFF4F6F8),
    surface1 = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF5F7F9),
    surface3 = Color(0xFFEBF0F4),
    line = Color(0xFFDCE3E9),
    amber = Color(0xFF126D60),
    mint = Color(0xFF126D60),
    sky = Color(0xFF45687D),
    rose = Color(0xFFB34238),
    violet = Color(0xFF666780),
    textPrimary = Color(0xFF192632),
    textSecondary = Color(0xFF506273),
    textMuted = Color(0xFF627487),
    heroStart = Color(0xFFE6F3EF),
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

/** 设备页顶部概览的强调底色。 */
val HeroStart: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.heroStart

/** 主色按钮上的文字/图标颜色。 */
val OnAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalDashaPalette.current.onAccent

private val DarkScheme = darkColorScheme(
    primary = DarkPalette.amber,
    onPrimary = DarkPalette.onAccent,
    primaryContainer = Color(0xFF234D46),
    onPrimaryContainer = Color(0xFFC3EDD8),
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
    outlineVariant = Color(0xFF1D3437),
    error = DarkPalette.rose,
    onError = Color(0xFF1B0505),
    errorContainer = Color(0xFF3A1414),
    onErrorContainer = Color(0xFFFFC9C9),
    scrim = Color(0xCC000000)
)

private val LightScheme = lightColorScheme(
    primary = LightPalette.amber,
    onPrimary = LightPalette.onAccent,
    primaryContainer = Color(0xFFD8EDDF),
    onPrimaryContainer = Color(0xFF193B28),
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
    outlineVariant = Color(0xFFE5EAF0),
    error = LightPalette.rose,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color(0x99000000)
)

private val DashaTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.8).sp, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp, fontFeatureSettings = "tnum"),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.5).sp
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
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.sp)
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
