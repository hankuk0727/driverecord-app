package app.driverecord

import android.content.Context

// 카메라 설정값을 한 번 저장해 두고, 이후 모든 캡처에 자동 적용
class CameraSettings(context: Context) {
    private val prefs = context.getSharedPreferences("camera_profile", Context.MODE_PRIVATE)

    companion object {
        // 처음 설치했을 때의 기본값 (샘플 사진에 맞춰 둔 값).
        // 사용자가 '이 값을 카메라 설정으로 저장'을 누르면 그 값이 우선한다.
        const val DEFAULT_K = -0.15f
        const val DEFAULT_CUT = 0.23f
    }

    // 사람이 슬라이더로 맞춘 값
    fun load(): CameraProfile = CameraProfile(
        k = prefs.getFloat("k", DEFAULT_K).toDouble(),
        cutFraction = prefs.getFloat("cut", DEFAULT_CUT).toDouble()
    )

    fun save(p: CameraProfile) {
        prefs.edit()
            .putFloat("k", p.k.toFloat())
            .putFloat("cut", p.cutFraction.toFloat())
            .apply()
    }

    // 보닛 자동 학습을 쓸지 (기본: 켬)
    fun autoLearn(): Boolean = prefs.getBoolean("auto_learn", true)

    fun setAutoLearn(on: Boolean) {
        prefs.edit().putBoolean("auto_learn", on).apply()
    }

    // 마지막으로 학습된 보닛 비율 (아직 없으면 null)
    fun learnedCut(): Double? {
        val v = prefs.getFloat("learned_cut", -1f)
        return if (v < 0f) null else v.toDouble()
    }

    fun setLearnedCut(cut: Double?) {
        prefs.edit().putFloat("learned_cut", cut?.toFloat() ?: -1f).apply()
    }
}