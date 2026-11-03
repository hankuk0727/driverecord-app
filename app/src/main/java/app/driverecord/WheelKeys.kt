package app.driverecord

import android.view.KeyEvent

object WheelKeys {
    // 시험용: 컴퓨터 키보드의 C 키. 나머지는 차량 핸들 버튼에서 흔히 오는 키 후보.
    val captureKeys = setOf(
        KeyEvent.KEYCODE_C,
        KeyEvent.KEYCODE_VOICE_ASSIST,
        KeyEvent.KEYCODE_MEDIA_NEXT,
        KeyEvent.KEYCODE_HEADSETHOOK
    )

    // 캡처 화면이 등록해 두는 동작. 캡처했으면 true, 지금은 무시했으면 false.
    var onCapture: (() -> Boolean)? = null
}