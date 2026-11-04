package app.driverecord.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * 낮/밤 전환 스위치. 기본은 밤(false).
 * [차량 낮/밤 신호 연동 미확인] 확인되면 이 값만 바꿔 주면 돼요.
 *
 * 색 값은 모두 [임시·앱 전용 제안 / Pleos 공식값 아님] 입니다.
 * DrAmber 라는 이름은 코드 호환용이고 실제 값은 블루그레이예요.
 */
object DrMode {
    var day by mutableStateOf(false)
}

// ===== 화면에서 쓰는 이름 (낮/밤에 따라 값이 바뀜) =====
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

// 사진 서명은 낮/밤 동일
val DrStickerInk = Color(0xFFE8D9BF)
val DrStickerBackground = Color(0xFF24211D)

// ===== 밤 값 [임시] =====
val DrBackgroundNight = Color(0xFF101418)
val DrSurfaceNight = Color(0xFF191F25)
val DrSurfaceRaisedNight = Color(0xFF242C34)
val DrSurfaceHighNight = Color(0xFF2A333C)

val DrTextPrimaryNight = Color(0xFFF3F6F8)
val DrTextSecondaryNight = Color(0xFFB8C3CE)

val DrAmberNight = Color(0xFFAEC4D9)
val DrOnAmberNight = Color(0xFF101820)
val DrAmberContainerNight = Color(0xFF263440)
val DrOnAmberContainerNight = Color(0xFFCFE6F7)

val DrOnSecondaryNight = Color(0xFF18232D)
val DrSecondaryContainerNight = Color(0xFF2A3540)
val DrOnSecondaryContainerNight = Color(0xFFDFE8F0)

val DrErrorNight = Color(0xFFFF9A91)
val DrOnErrorNight = Color(0xFF3A1010)
val DrErrorContainerNight = Color(0xFF492222)
val DrOnErrorContainerNight = Color(0xFFFFDAD6)

val DrOutlineNight = Color(0xFF7F90A0)
val DrOutlineVariantNight = Color(0xFF3B4855)
val DrDisabledContentNight = Color(0xFF98A6B4)

// ===== 낮 값 [임시, 야간 팔레트에 맞춰 직접 만든 값] =====
val DrBackgroundDay = Color(0xFFF2F5F7)
val DrSurfaceDay = Color(0xFFFFFFFF)
val DrSurfaceRaisedDay = Color(0xFFE4EAF0)
val DrSurfaceHighDay = Color(0xFFD6DFE7)

val DrTextPrimaryDay = Color(0xFF15202B)
val DrTextSecondaryDay = Color(0xFF455565)

val DrAmberDay = Color(0xFF35546F)
val DrOnAmberDay = Color(0xFFFFFFFF)
val DrAmberContainerDay = Color(0xFFDDE7F0)
val DrOnAmberContainerDay = Color(0xFF1F3547)

val DrOnSecondaryDay = Color(0xFFFFFFFF)
val DrSecondaryContainerDay = Color(0xFFDCE6EF)
val DrOnSecondaryContainerDay = Color(0xFF243B50)

val DrErrorDay = Color(0xFFB42318)
val DrOnErrorDay = Color(0xFFFFFFFF)
val DrErrorContainerDay = Color(0xFFFCE5E2)
val DrOnErrorContainerDay = Color(0xFF7A1C16)

val DrOutlineDay = Color(0xFF627384)
val DrOutlineVariantDay = Color(0xFFBCC8D3)
val DrDisabledContentDay = Color(0xFF506170)