package app.driverecord.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import app.driverecord.R

// Pleos Connect 공식 글꼴(Asta Sans, OFL 라이선스)과 공식 글자 크기 표를 따릅니다.
// 공식 표는 화면 픽셀(px) 기준이라, 폰에서 너무 크게 보이지 않도록 한 곳(PleosTextScale)에서 줄여 씁니다.
// 차량 에뮬레이터에서 실행해 보고 이 숫자만 바꾸면 모든 글자 크기가 같이 바뀝니다.
private const val PleosTextScale = 0.65f

private fun px(value: Float): TextUnit = (value * PleosTextScale).sp

val AstaSans = FontFamily(
    Font(R.font.asta_sans_regular, FontWeight.Normal),
    Font(R.font.asta_sans_bold, FontWeight.Bold),
    Font(R.font.asta_sans_extrabold, FontWeight.ExtraBold)
)

private fun style(weight: FontWeight, size: Float, line: Float) = TextStyle(
    fontFamily = AstaSans,
    fontWeight = weight,
    fontSize = px(size),
    lineHeight = px(line),
    letterSpacing = 0.sp
)

// 공식 스타일 이름 → Material 이름 대응 (크기/줄높이는 공식 표 값)
val Typography = Typography(
    displayLarge = style(FontWeight.Bold, 60f, 80f),        // Headline/Large/Strong
    displayMedium = style(FontWeight.ExtraBold, 56f, 72f),  // Headline/Medium/Extra_Strong
    displaySmall = style(FontWeight.Bold, 48f, 64f),        // Headline/Small/Strong
    headlineLarge = style(FontWeight.Bold, 40f, 52f),       // Title/Large/Strong
    headlineMedium = style(FontWeight.Bold, 36f, 44f),      // Title/Medium/Strong
    headlineSmall = style(FontWeight.Bold, 32f, 40f),       // Title/Small/Strong
    titleLarge = style(FontWeight.Bold, 30f, 38f),          // Body/Large/Strong
    titleMedium = style(FontWeight.Bold, 28f, 36f),         // Body/Medium/Strong
    titleSmall = style(FontWeight.Bold, 26f, 34f),          // Body/Small/Strong
    bodyLarge = style(FontWeight.Normal, 30f, 38f),         // Body/Large/Normal
    bodyMedium = style(FontWeight.Normal, 26f, 34f),        // Body/Small/Normal
    bodySmall = style(FontWeight.Normal, 24f, 28f),         // Label/Medium
    labelLarge = style(FontWeight.Bold, 26f, 34f),          // 버튼 글자: Body/Small/Strong
    labelMedium = style(FontWeight.Normal, 24f, 28f),       // Label/Medium
    labelSmall = style(FontWeight.Normal, 20f, 24f)         // Label/Small
)