package app.driverecord.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Pleos Connect 공식 디자인 파일(design assets, Hyundai 테마)에서 읽은 값입니다.
// [공식] 표시가 없는 값은 반투명 색을 바탕 위에 겹쳐 계산했거나, 글자 대비(4.5:1)를 맞추려고 조정한 값입니다.

/** 주간/야간 전환. 기본은 야간. 차량의 주야간 신호를 연결하기 전까지는 앱 안 스위치로만 바뀝니다. */
object DrMode {
    var day by mutableStateOf(false)
}

// ---------- 야간 (Hyundai-Dark) ----------
private val DrBackgroundNight = Color(0xFF1D1E21)            // [공식] background/primary
private val DrSurfaceNight = Color(0xFF28292C)               // 바탕 위 흰색 5% (button/basic/enabled)
private val DrSurfaceRaisedNight = Color(0xFF313236)         // [공식] surface/high
private val DrSurfaceHighNight = Color(0xFF44464E)           // [공식] surface/accent
private val DrTextPrimaryNight = Color(0xFFFFFFFF)           // [공식] text primary
private val DrTextSecondaryNight = Color(0xFFDBDBDB)         // 흰색 84% 계산
private val DrAmberNight = Color(0xFFE0E1E6)                 // [공식] button/filled/enabled
private val DrOnAmberNight = Color(0xFF131417)               // [공식] basic/00
private val DrAmberContainerNight = Color(0xFF44464E)        // [공식] surface/accent
private val DrOnAmberContainerNight = Color(0xFFFFFFFF)
private val DrOnSecondaryNight = Color(0xFF131417)
private val DrSecondaryContainerNight = Color(0xFF313236)
private val DrOnSecondaryContainerNight = Color(0xFFFFFFFF)
private val DrErrorNight = Color(0xFFFF7A70)                 // regulation/red(FF4339)를 글자 대비용으로 밝힘
private val DrOnErrorNight = Color(0xFF131417)
private val DrErrorContainerNight = Color(0xFF4A2526)
private val DrOnErrorContainerNight = Color(0xFFFFD9D5)
private val DrOutlineNight = Color(0xFF7A7B7E)
private val DrOutlineVariantNight = Color(0xFF3F4045)
private val DrDisabledContentNight = Color(0xFF9A9B9E)
private val DrSwitchOnNight = Color(0xFF00E074)              // [공식] switch/on

// ---------- 주간 (Hyundai-Light) ----------
private val DrBackgroundDay = Color(0xFFF7F8FA)              // [공식] background/primary
private val DrSurfaceDay = Color(0xFFFFFFFF)                 // [공식] surface/basic
private val DrSurfaceRaisedDay = Color(0xFFEDEEF2)           // [공식] surface/high
private val DrSurfaceHighDay = Color(0xFFE0E1E6)             // [공식] surface/accent
private val DrTextPrimaryDay = Color(0xFF131417)             // [공식] text primary
private val DrTextSecondaryDay = Color(0xFF38383B)           // 검정 84% 계산
private val DrAmberDay = Color(0xFF44464E)                   // [공식] button/filled/enabled
private val DrOnAmberDay = Color(0xFFFFFFFF)
private val DrAmberContainerDay = Color(0xFFE0E1E6)
private val DrOnAmberContainerDay = Color(0xFF131417)
private val DrOnSecondaryDay = Color(0xFFFFFFFF)
private val DrSecondaryContainerDay = Color(0xFFEDEEF2)
private val DrOnSecondaryContainerDay = Color(0xFF131417)
private val DrErrorDay = Color(0xFFC4231B)                   // regulation/red(F62E24)를 글자 대비용으로 어둡게
private val DrOnErrorDay = Color(0xFFFFFFFF)
private val DrErrorContainerDay = Color(0xFFFEE6E5)
private val DrOnErrorContainerDay = Color(0xFF7A1510)
private val DrOutlineDay = Color(0xFF8E8F93)
private val DrOutlineVariantDay = Color(0xFFD5D6DB)
private val DrDisabledContentDay = Color(0xFF6E6F73)
private val DrSwitchOnDay = Color(0xFF02C265)                // [공식] switch/on

// ---------- 화면에서 쓰는 이름 (주야간 자동 선택) ----------
val DrBackground: Color get() = if (DrMode.day) DrBackgroundDay else DrBackgroundNight
val DrSurface: Color get() = if (DrMode.day) DrSurfaceDay else DrSurfaceNight
val DrSurfaceRaised: Color get() = if (DrMode.day) DrSurfaceRaisedDay else DrSurfaceRaisedNight
val DrSurfaceHigh: Color get() = if (DrMode.day) DrSurfaceHighDay else DrSurfaceHighNight
val DrTextPrimary: Color get() = if (DrMode.day) DrTextPrimaryDay else DrTextPrimaryNight
val DrTextSecondary: Color get() = if (DrMode.day) DrTextSecondaryDay else DrTextSecondaryNight
val DrAmber: Color get() = if (DrMode.day) DrAmberDay else DrAmberNight
val DrOnAmber: Color get() = if (DrMode.day) DrOnAmberDay else DrOnAmberNight
val DrAmberContainer: Color get() = if (DrMode.day) DrAmberContainerDay else DrAmberContainerNight
val DrOnAmberContainer: Color get() = if (DrMode.day) DrOnAmberContainerDay else DrOnAmberContainerNight
val DrOnSecondary: Color get() = if (DrMode.day) DrOnSecondaryDay else DrOnSecondaryNight
val DrSecondaryContainer: Color get() = if (DrMode.day) DrSecondaryContainerDay else DrSecondaryContainerNight
val DrOnSecondaryContainer: Color get() = if (DrMode.day) DrOnSecondaryContainerDay else DrOnSecondaryContainerNight
val DrError: Color get() = if (DrMode.day) DrErrorDay else DrErrorNight
val DrOnError: Color get() = if (DrMode.day) DrOnErrorDay else DrOnErrorNight
val DrErrorContainer: Color get() = if (DrMode.day) DrErrorContainerDay else DrErrorContainerNight
val DrOnErrorContainer: Color get() = if (DrMode.day) DrOnErrorContainerDay else DrOnErrorContainerNight
val DrOutline: Color get() = if (DrMode.day) DrOutlineDay else DrOutlineNight
val DrOutlineVariant: Color get() = if (DrMode.day) DrOutlineVariantDay else DrOutlineVariantNight
val DrDisabledContent: Color get() = if (DrMode.day) DrDisabledContentDay else DrDisabledContentNight
val DrSwitchOn: Color get() = if (DrMode.day) DrSwitchOnDay else DrSwitchOnNight

// 스티커(사진에 붙는 글자)는 주야간 구분 없이 그대로 둡니다.
val DrStickerInk = Color(0xFFE8D9BF)
val DrStickerBackground = Color(0xFF24211D)