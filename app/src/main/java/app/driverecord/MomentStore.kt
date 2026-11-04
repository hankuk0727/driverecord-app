package app.driverecord

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class MomentStore(context: Context) {
    private val prefs = context.getSharedPreferences("drive_record", Context.MODE_PRIVATE)

    fun load(): List<DriveMoment> {
        val raw = prefs.getString("moments", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                DriveMoment(
                    id = o.getInt("id"),
                    timeText = o.optString("timeText"),
                    driveMode = o.optString("driveMode"),
                    placeName = o.optString("placeName"),
                    memo = o.optString("memo", ""),
                    photoPath = o.optString("photoPath", ""),
                    speedKmh = o.optInt("speedKmh", 0),
                    sticker = o.optBoolean("sticker", false),
                    latitude = o.optDouble("latitude", 0.0),
                    longitude = o.optDouble("longitude", 0.0),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(list: List<DriveMoment>) {
        val arr = JSONArray()
        list.forEach { m ->
            arr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("timeText", m.timeText)
                    .put("driveMode", m.driveMode)
                    .put("placeName", m.placeName)
                    .put("memo", m.memo)
                    .put("photoPath", m.photoPath)
                    .put("speedKmh", m.speedKmh)
                    .put("sticker", m.sticker)
                    .put("latitude", m.latitude)
                    .put("longitude", m.longitude)
                    .put("createdAt", m.createdAt)
            )
        }
        prefs.edit().putString("moments", arr.toString()).apply()
    }
}