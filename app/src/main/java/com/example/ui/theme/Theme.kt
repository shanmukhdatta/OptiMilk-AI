package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val OptiMilkLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = PureWhite,
    primaryContainer = MintLight,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldDark,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceMintTint,
    onSecondaryContainer = TextPrimaryInk,
    tertiary = EmeraldPrimary,
    onTertiary = PureWhite,
    tertiaryContainer = MintLight,
    onTertiaryContainer = EmeraldDark,
    background = PureWhite,
    onBackground = TextPrimaryInk,
    surface = PureWhite,
    onSurface = TextPrimaryInk,
    surfaceVariant = SurfaceMintTint,
    onSurfaceVariant = TextSecondarySlate,
    surfaceContainerLowest = PureWhite,
    surfaceContainerLow = SurfaceMintTint,
    surfaceContainer = SurfaceMintTint,
    surfaceContainerHigh = MintLight,
    surfaceContainerHighest = MintLight,
    outline = BorderSage,
    outlineVariant = BorderSage,
    error = ErrorCrimson,
    onError = PureWhite,
    errorContainer = ErrorSoftBg,
    onErrorContainer = ErrorCrimson,
)

val OptiMilkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun OptiMilkTheme(
    content: @Composable () -> Unit,
) {
    // Strictly enforces the White and Green Light Theme as specified.
    MaterialTheme(
        colorScheme = OptiMilkLightColorScheme,
        typography = OptiMilkTypography,
        shapes = OptiMilkShapes,
        content = content,
    )
}
