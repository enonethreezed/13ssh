package io.enonethreezed.sshclient.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColors = darkColorScheme(
    primary = Copper,
    secondary = Sea,
    tertiary = Moss,
    background = InkBlack,
    surface = SlatePanel,
    onPrimary = Mist,
    onSecondary = Mist,
    onTertiary = Mist,
    onBackground = Mist,
    onSurface = Mist,
)

@Composable
fun ThirteenSshTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        content = content,
    )
}
