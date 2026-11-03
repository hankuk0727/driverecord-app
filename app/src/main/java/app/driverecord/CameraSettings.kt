package app.driverecord

import android.content.Context

// 카메라 설정값을 한 번 저장해 두고, 이후 모든 캡처에 자동 적용
class CameraSettings(context: Context) {
    private val prefs = context.getSharedPreferences("camera_profile", Context.MODE_PRIVATE)

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
}

data class SamplePhoto(val label: String, val resId: Int, val profile: CameraProfile)

// 시연용 사진 3장과, 사진마다 눈으로 맞춰 본 추천값
object SamplePhotos {
    val all = listOf(
        SamplePhoto("대구 시내", R.drawable.dashcam1, CameraProfile(-0.03, 0.17)),
        SamplePhoto("한강", R.drawable.dashcam2, CameraProfile(-0.03, 0.15)),
        SamplePhoto("광각", R.drawable.dashcam3, CameraProfile(-0.15, 0.24))
    )
}
