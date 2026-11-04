package app.driverecord

import android.location.Location
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// "같은 장소 회상": 지금 있는 곳 근처에서 예전에 저장한 장면을 찾는다.
object PlaceRecall {

    private const val RADIUS_METERS = 200f   // 이 거리 안이면 같은 장소로 본다

    // 두 지점 사이 거리(미터)
    private fun distance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val result = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, result)
        return result[0]
    }

    // 위치가 저장된 장면 중, 이 장소 근처에서 저장한 장면들 (최근 것이 앞)
    fun find(all: List<DriveMoment>, place: Place): List<DriveMoment> =
        all.filter { NaviHelper.hasLocation(it) }
            .filter { distance(it.latitude, it.longitude, place.latitude, place.longitude) <= RADIUS_METERS }
            .sortedByDescending { it.id }

    // 알려 줄 문장. 예전 장면이 없으면 null
    fun message(past: List<DriveMoment>, placeName: String): String? {
        if (past.isEmpty()) return null
        val last = past.first()
        val whenText = if (last.createdAt > 0L) {
            SimpleDateFormat("M월 d일 HH:mm", Locale.KOREA).format(Date(last.createdAt))
        } else {
            last.timeText
        }
        return "$placeName 에서 전에 저장한 장면이 ${past.size}개 있어요. 가장 최근: $whenText"
    }
}