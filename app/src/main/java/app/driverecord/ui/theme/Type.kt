package app.driverecord.ui.theme

import android.content.pm.PackageManager
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.driverecord.R

// Pleos Connect 공식 글꼴(Asta Sans, OFL 라이선스)과 공식 글자 크기 표를 따릅니다.
// 공식 표의 숫자는 sp/dp이고, 17인치 차량 화면(160dpi)에서는 그대로 쓰는 게 맞습니다.
// (가이드: 글자 높이 최소 18px 이상, 가장 작은 공식 글자는 Label Small 20)
// 휴대폰은 화면 밀도가 높아 같은 숫자가 너무 크게 보이므로 폰에서만 줄여 씁니다.
private const val CarTextScale = 1.0f
private const val PhoneTextScale = 0.65f

val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_bold, FontWeight.Bold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold)
)

private fun style(weight: FontWeight, size: Float, line: Float, scale: Float) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = (size * scale).sp,
    lineHeight = (line * scale).sp,
    letterSpacing = 0.sp
)

// 공식 스타일 이름 → Material 이름 대응 (크기/줄높이는 공식 표 값)
private fun buildTypography(s: Float) = Typography(
    displayLarge = style(FontWeight.Bold, 60f, 80f, s),        // Headline/Large/Strong
    displayMedium = style(FontWeight.ExtraBold, 56f, 72f, s),  // Headline/Medium/Extra_Strong
    displaySmall = style(FontWeight.Bold, 48f, 64f, s),        // Headline/Small/Strong
    headlineLarge = style(FontWeight.Bold, 40f, 52f, s),       // Title/Large/Strong
    headlineMedium = style(FontWeight.Bold, 36f, 44f, s),      // Title/Medium/Strong
    headlineSmall = style(FontWeight.Bold, 32f, 40f, s),       // Title/Small/Strong
    titleLarge = style(FontWeight.Bold, 30f, 38f, s),          // Body/Large/Strong
    titleMedium = style(FontWeight.Bold, 28f, 36f, s),         // Body/Medium/Strong
    titleSmall = style(FontWeight.Bold, 26f, 34f, s),          // Body/Small/Strong
    bodyLarge = style(FontWeight.Normal, 30f, 38f, s),         // Body/Large/Normal
    bodyMedium = style(FontWeight.Normal, 26f, 34f, s),        // Body/Small/Normal
    bodySmall = style(FontWeight.Normal, 24f, 28f, s),         // Label/Medium
    labelLarge = style(FontWeight.Bold, 26f, 34f, s),          // 버튼 글자: Body/Small/Strong
    labelMedium = style(FontWeight.Normal, 24f, 28f, s),       // Label/Medium
    labelSmall = style(FontWeight.Normal, 20f, 24f, s)         // Label/Small
)

// 차량(Automotive)이면 공식 크기 그대로, 휴대폰이면 줄인 크기를 돌려줍니다.
val Typography: Typography
    @Composable
    get() {
        val context = LocalContext.current
        val onCar = context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        return remember(onCar) {
            buildTypography(if (onCar) CarTextScale else PhoneTextScale)
        }
    }