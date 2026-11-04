package app.driverecord

import android.content.Context

// 카메라 설정값을 한 번 저장해 두고, 이후 모든 캡처에 자동 적용
class CameraSettings(context: Context) {
    private val prefs = context.getSharedPreferences("camera_profile", Context.MODE_PRIVATE)

    // 사람이 슬라이더로 맞춘 값
    fun load(): CameraProfile = CameraProfile(
        k = prefs.getFloat("k", -0.03f).toDouble(),
        cutFraction = prefs.getFloat("cut", 0.15f).toDouble()
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