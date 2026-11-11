package app.driverecord

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.Locale

// 캡처하면 차량 스피커로 "장면을 저장했어요"라고 말해 준다 (Pleos TextToSpeech SDK).
// 운전자가 화면을 보지 않아도 저장됐는지 알 수 있게 하려는 것.
// 차량 안의 음성 엔진(ON_DEVICE)만 써서 인터넷과 앱 등록이 필요 없다.
// 차량이 아니거나(폰) 준비에 실패하면 아무 말도 하지 않고, 기존 삑 소리만 난다.
@SuppressLint("MissingPermission")   // 권한은 AndroidManifest.xml 에 선언되어 있음
object VoiceGuide {

    private const val TAG = "VoiceGuide"

    private var tts: TextToSpeech? = null

    @Volatile
    private var ready = false

    private val listener = object : EventListener {
        override fun onReady() = Unit
        override fun onStart() = Unit
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = Unit
        override fun onStop() = Unit
        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "음성 안내 오류: $errorType")
        }
    }

    // 앱 화면이 보일 때 한 번 준비한다 (MainActivity.onStart)
    fun start(context: Context) {
        if (tts != null) return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        try {
            val t = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
            tts = t
            t.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    try {
                        t.addEventListener(listener)
                        t.setLocale(Locale.KOREA)
                        ready = true
                        Log.i(TAG, "음성 안내 준비 완료")
                    } catch (e: Throwable) {
                        Log.w(TAG, "음성 안내 설정 실패", e)
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "음성 안내 준비 실패: $errorType")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "음성 안내를 쓸 수 없음", e)
            tts = null
            ready = false
        }
    }

    // 짧게 말하기. 준비가 안 됐으면 조용히 넘어간다.
    fun say(text: String) {
        val t = tts ?: return
        if (!ready) return
        try {
            t.speak(text)
        } catch (e: Throwable) {
            Log.w(TAG, "음성 안내 실패", e)
        }
    }

    // 앱 화면이 사라질 때 정리한다 (MainActivity.onStop)
    fun stop() {
        val t = tts ?: return
        tts = null
        ready = false
        runCatching { t.removeEventListener(listener) }
        runCatching { t.release() }
    }
}