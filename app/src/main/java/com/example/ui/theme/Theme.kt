package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class RookieSemanticColors(
  val lightSquare: Color,
  val darkSquare: Color,
  val lastMoveTint: Color,
  val selectedTint: Color,
  val checkGlow: Color,
  val hintOriginTint: Color,
  val hintTargetTint: Color,
  val hintBannerBorder: Color,
  val successAccent: Color,
  val dangerAccent: Color,
  val subtleBorder: Color,
)

val LocalRookieColors = staticCompositionLocalOf {
  RookieSemanticColors(
    lightSquare = BoardLightSquare,
    darkSquare = BoardDarkSquare,
    lastMoveTint = LastMoveHighlight,
    selectedTint = SelectedSquareHighlight,
    checkGlow = CheckGlowColor,
    hintOriginTint = HintOriginHighlight,
    hintTargetTint = HintTargetHighlight,
    hintBannerBorder = HintAccentBlue,
    successAccent = TournamentGreen,
    dangerAccent = DangerRed,
    subtleBorder = CharcoalBorder,
  )
}

private val RookieDarkColorScheme = darkColorScheme(
  primary = TournamentGreen,
  onPrimary = TournamentGreenDarkOn,
  primaryContainer = Color(0xFF2B3A1B),
  onPrimaryContainer = Color(0xFFD8F0B4),
  secondary = Color(0xFFB8C99E),
  onSecondary = Color(0xFF1F2B11),
  secondaryContainer = Color(0xFF34322D),
  onSecondaryContainer = WarmIvoryText,
  tertiary = HintAccentBlue,
  onTertiary = Color.White,
  background = CharcoalBg,
  onBackground = WarmIvoryText,
  surface = CharcoalPanel,
  onSurface = WarmIvoryText,
  surfaceVariant = CharcoalSurfaceHigh,
  onSurfaceVariant = WarmMutedText,
  surfaceContainerLowest = Color(0xFF12110F),
  surfaceContainerLow = Color(0xFF1D1B18),
  surfaceContainer = CharcoalPanel,
  surfaceContainerHigh = CharcoalSurfaceHigh,
  surfaceContainerHighest = Color(0xFF3A3631),
  outline = CharcoalBorder,
  outlineVariant = Color(0xFF2F2C28),
  error = DangerRed,
  onError = Color.White,
)

private val RookieLightColorScheme = lightColorScheme(
  primary = TournamentGreenLight,
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD9ECB9),
  onPrimaryContainer = Color(0xFF152606),
  secondary = Color(0xFF536342),
  onSecondary = Color.White,
  secondaryContainer = WarmStoneHigh,
  onSecondaryContainer = StoneInkText,
  tertiary = Color(0xFF2768C4),
  onTertiary = Color.White,
  background = WarmStoneBg,
  onBackground = StoneInkText,
  surface = WarmWhitePanel,
  onSurface = StoneInkText,
  surfaceVariant = WarmStoneHigh,
  onSurfaceVariant = StoneMutedText,
  surfaceContainerLowest = Color.White,
  surfaceContainerLow = Color(0xFFF7F5F0),
  surfaceContainer = WarmWhitePanel,
  surfaceContainerHigh = WarmStoneHigh,
  surfaceContainerHighest = Color(0xFFDDD8CC),
  outline = WarmStoneBorder,
  outlineVariant = Color(0xFFE6E2D8),
  error = DangerRed,
  onError = Color.White,
)

private val RookieShapes = Shapes(
  small = RoundedCornerShape(8.dp),
  medium = RoundedCornerShape(12.dp),
  large = RoundedCornerShape(16.dp),
  extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun RookieTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) RookieDarkColorScheme else RookieLightColorScheme
  val semantic = RookieSemanticColors(
    lightSquare = BoardLightSquare,
    darkSquare = BoardDarkSquare,
    lastMoveTint = LastMoveHighlight,
    selectedTint = SelectedSquareHighlight,
    checkGlow = CheckGlowColor,
    hintOriginTint = HintOriginHighlight,
    hintTargetTint = HintTargetHighlight,
    hintBannerBorder = HintAccentBlue,
    successAccent = if (darkTheme) TournamentGreen else TournamentGreenLight,
    dangerAccent = DangerRed,
    subtleBorder = if (darkTheme) CharcoalBorder else WarmStoneBorder,
  )

  CompositionLocalProvider(LocalRookieColors provides semantic) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = RookieTypography,
      shapes = RookieShapes,
      content = content,
    )
  }
}
