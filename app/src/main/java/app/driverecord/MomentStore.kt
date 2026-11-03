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
                    memo = o.optString("memo", "")
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
            )
        }
        prefs.edit().putString("moments", arr.toString()).apply()
    }
}