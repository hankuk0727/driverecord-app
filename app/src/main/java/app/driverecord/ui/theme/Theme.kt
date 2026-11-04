package app.driverecord.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// 항상 어두운 차량용 테마. 폰 설정이나 배경화면 색(동적 색상)은 쓰지 않는다.
private val DriveRecordColors = darkColorScheme(
    primary = DrAmber,
    onPrimary = DrOnAmber,
    primaryContainer = DrAmberContainer,
    onPrimaryContainer = DrOnAmberContainer,

    secondary = DrTextSecondary,
    onSecondary = DrOnSecondary,
    secondaryContainer = DrSecondaryContainer,
    onSecondaryContainer = DrOnSecondaryContainer,

    // tertiary 계열은 secondary 계열과 같게
    tertiary = DrTextSecondary,
    onTertiary = DrOnSecondary,
    tertiaryContainer = DrSecondaryContainer,
    onTertiaryContainer = DrOnSecondaryContainer,

    background = DrBackground,
    onBackground = DrTextPrimary,

    surface = DrSurface,
    onSurface = DrTextPrimary,
    surfaceVariant = DrSurfaceRaised,
    onSurfaceVariant = DrTextSecondary,
    surfaceContainerLowest = DrBackground,
    surfaceContainerLow = DrSurface,
    surfaceContainer = DrSurface,
    surfaceContainerHigh = DrSurfaceRaised,
    surfaceContainerHighest = DrSurfaceHigh,

    error = DrError,
    onError = DrOnError,
    errorContainer = DrErrorContainer,
    onErrorContainer = DrOnErrorContainer,

    outline = DrOutline,
    outlineVariant = DrOutlineVariant
)

@Composable
fun DriveRecordTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DriveRecordColors,
        typography = Typography,
        content = content
    )
}