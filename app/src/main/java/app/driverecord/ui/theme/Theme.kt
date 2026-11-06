package app.driverecord.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// 시스템(차량)의 낮/밤 설정을 따라갑니다. 폰 배경화면 색(동적 색상)은 쓰지 않습니다.
// 색 이름(DrAmber 등)은 DrMode.day 값에 따라 주간/야간 색을 돌려주므로,
// 낮/밤이 바뀔 때마다 색 구성을 새로 만듭니다.
private fun buildColors(day: Boolean): ColorScheme {
    val base = if (day) lightColorScheme() else darkColorScheme()
    return base.copy(
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
}

@Composable
fun DriveRecordTheme(content: @Composable () -> Unit) {
    // 시스템이 어두운 모드면 야간, 밝은 모드면 주간. 시스템 설정이 바뀔 때마다 다시 맞춥니다.
    val systemDark = isSystemInDarkTheme()
    remember(systemDark) { DrMode.day = !systemDark }

    val day = DrMode.day
    val colors = remember(day) { buildColors(day) }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}