package app.driverecord

import ai.pleos.playground.stt.SpeechToText
import ai.pleos.playground.stt.constant.ErrorType
import ai.pleos.playground.stt.constant.Mode
import ai.pleos.playground.stt.listener.OnInitializeListener
import ai.pleos.playground.stt.listener.ResultListener
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

// "캡처"라고 말하면 장면을 저장한다 (Pleos SpeechToText SDK).
// - 사용자가 켠 경우에만, 차량에서, 주행 화면일 때만 차량 마이크로 듣는다. (기본은 꺼짐)
// - 차량 안의 음성 엔진(ON_DEVICE)만 써서 말소리가 서버로 가지 않고, 인식한 글자도 저장하지 않는다.
// - 한 문장 인식이 끝나면 다시 듣고, 오류가 계속되면(5번) 조용히 멈춘다. 화면 버튼은 그대로 동작한다.
@SuppressLint("MissingPermission")   // 권한은 AndroidManifest.xml 에 선언되어 있음
object VoiceCapture {

    private const val TAG = "VoiceCapture"
    private const val PREFS = "voice_prefs"
    private const val KEY_ON = "voice_capture"

    // 이 말이 들리면 캡처. ("저장"은 넣지 않음: 음성 안내 "장면을 저장했어요"를 다시 듣고 또 찍는 일을 막기 위해)
    private val WORDS = listOf("캡처", "캡쳐", "찍어")
    private const val GAP_MS = 3_000L      // 같은 말로 두 번 찍히지 않게 3초 간격
    private const val MAX_ERRORS = 5

    private val main = Handler(Looper.getMainLooper())
    private var stt: SpeechToText? = null
    private var onCommand: (() -> Unit)? = null
    private var listening = false
    private var lastFire = 0L
    private var errorsInRow = 0

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ON, false)

    fun setEnabled(context: Context, on: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ON, on).apply()
    }

    private val requestAgain = Runnable {
        if (listening) {
            try {
                stt?.request()
            } catch (e: Throwable) {
                Log.w(TAG, "다시 듣기 실패", e)
            }
        }
    }

    private fun listenAgain(delayMs: Long) {
        main.removeCallbacks(requestAgain)
        main.postDelayed(requestAgain, delayMs)
    }

    private fun handleText(text: String) {
        if (!listening) return
        val t = text.replace(" ", "")
        if (WORDS.none { t.contains(it) }) return
        val now = System.currentTimeMillis()
        if (now - lastFire < GAP_MS) return
        lastFire = now
        Log.i(TAG, "음성 캡처")
        onCommand?.invoke()
    }

    private val listener = object : ResultListener {
        override fun onReady() = Unit

        override fun onStartedRecognition() {
            main.post { errorsInRow = 0 }
        }

        // 중간 결과(completed=false)에도 반응해서 빨리 찍는다. 3초 간격으로 두 번 찍히는 건 막는다.
        override fun onUpdated(stt: String, completed: Boolean) {
            main.post { handleText(stt) }
        }

        override fun onUpdatedEpdData(on: Long, off: Long) = Unit

        override fun onUpdatedRms(rms: Double) = Unit

        // 한 문장이 끝나면 바로 다시 듣는다
        override fun onEndedRecognition() {
            main.post { listenAgain(300) }
        }

        override fun onError(errorType: ErrorType) {
            Log.w(TAG, "음성 인식 오류: $errorType")
            main.post {
                errorsInRow++
                if (errorsInRow < MAX_ERRORS) listenAgain(2_000)
                else Log.w(TAG, "오류가 계속되어 음성 캡처를 멈춤")
            }
        }
    }

    // 주행 화면에 들어왔을 때 (CaptureTab)
    fun start(context: Context, command: () -> Unit) {
        onCommand = command
        if (listening) return
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) return
        listening = true
        errorsInRow = 0
        try {
            val s = SpeechToText(context.applicationContext, Mode.ON_DEVICE)
            stt = s
            s.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    main.post {
                        if (!listening || stt !== s) return@post
                        try {
                            s.addListener(listener)
                            s.setLocale(Locale.KOREA)
                            s.request()
                            Log.i(TAG, "음성 캡처 듣기 시작")
                        } catch (e: Throwable) {
                            Log.w(TAG, "음성 캡처 시작 실패", e)
                        }
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    Log.w(TAG, "음성 인식 준비 실패: $errorType")
                }
            })
        } catch (e: Throwable) {
            Log.w(TAG, "음성 인식을 쓸 수 없음", e)
            listening = false
            stt = null
        }
    }

    // 정차하거나, 끄거나, 화면을 떠날 때
    fun stop() {
        listening = false
        onCommand = null
        main.removeCallbacks(requestAgain)
        val s = stt ?: return
        stt = null
        runCatching { s.stop() }
        runCatching { s.removeListener(listener) }
        runCatching { s.release() }
    }
}