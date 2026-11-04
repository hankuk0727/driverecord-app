package app.driverecord

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

// "다시 가기": 저장한 장면의 장소로 길 안내를 켠다.
// 지금은 폰의 지도 앱을 열어 주고, 차량에 넣을 때는 이 안만 차량 내비 호출로 바꾸면 된다.
object NaviHelper {

    // 위치가 저장된 장면인지 (0.0, 0.0 이면 위치를 모르는 장면)
    fun hasLocation(m: DriveMoment): Boolean =
        m.latitude != 0.0 || m.longitude != 0.0

    // 열기에 성공하면 true, 지도 앱이 없으면 false
    fun open(context: Context, m: DriveMoment): Boolean {
        if (!hasLocation(m)) return false
        val label = Uri.encode(m.placeName)
        val uri = Uri.parse("geo:${m.latitude},${m.longitude}?q=${m.latitude},${m.longitude}($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}