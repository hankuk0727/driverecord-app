package app.driverecord

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import kotlin.math.max
import kotlin.math.sqrt

// 같은 카메라로 찍은 사진 여러 장에서, 매번 변하지 않는 아래쪽 줄을 찾아 보닛 위치를 학습
class BonnetLearner(context: Context) {
    private val dir = File(context.filesDir, "learn").apply { mkdirs() }

    data class Result(
        val frames: Int,       // 지금까지 모인 사진 수
        val cut: Double?       // 학습된 보닛 비율. 아직 배울 수 없으면 null
    )

    // 캡처한 사진(왜곡을 편 상태)의 작은 흑백 복사본을 저장
    @Synchronized
    fun add(undistorted: Bitmap) {
        val small = Bitmap.createScaledBitmap(undistorted, W, H, true)
        val px = IntArray(W * H)
        small.getPixels(px, 0, W, 0, 0, W, H)
        val bytes = ByteArray(W * H) { i ->
            val c = px[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt().toByte()
        }
        File(dir, "f_${System.currentTimeMillis()}_${(0..999).random()}.bin").writeBytes(bytes)

        val files = dir.listFiles()?.sortedBy { it.name } ?: return
        if (files.size > MAX_FRAMES) {
            files.take(files.size - MAX_FRAMES).forEach { it.delete() }
        }
    }

    @Synchronized
    fun learn(): Result {
        val files = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
        val n = files.size
        if (n < MIN_FRAMES) return Result(n, null)

        val data = files.map { it.readBytes() }
        val rowSd = DoubleArray(H)
        val colSd = DoubleArray(W)
        for (y in 0 until H) {
            for (x in 0 until W) {
                val i = y * W + x
                var sum = 0.0
                var sum2 = 0.0
                for (d in data) {
                    val v = (d[i].toInt() and 0xFF).toDouble()
                    sum += v
                    sum2 += v * v
                }
                val mean = sum / n
                colSd[x] = sqrt(max(0.0, sum2 / n - mean * mean))
            }
            rowSd[y] = median(colSd)
        }

        // 도로 부분(위쪽)이 사진마다 얼마나 변하는지. 너무 안 변하면(정지 상태 등) 배울 수 없음
        val upper = median(rowSd.copyOfRange((H * 0.15).toInt(), (H * 0.60).toInt()))
        if (upper < MIN_UPPER_CHANGE) return Result(n, null)

        // 맨 아래에서 위로 올라가며, 거의 변하지 않는 줄이 이어지는 만큼이 보닛
        val threshold = upper * 0.4
        var y = H
        while (y > (H * 0.55).toInt() && rowSd[y - 1] < threshold) y--
        return Result(n, (H - y).toDouble() / H)
    }

    @Synchronized
    fun clear() {
        dir.listFiles()?.forEach { it.delete() }
    }

    private fun median(a: DoubleArray): Double {
        val s = a.sortedArray()
        return s[s.size / 2]
    }

    companion object {
        private const val W = 240
        private const val H = 135
        const val MIN_FRAMES = 5
        private const val MAX_FRAMES = 10
        private const val MIN_UPPER_CHANGE = 12.0
    }
}