package app.driverecord

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

// 카메라마다 고정인 값: 렌즈 왜곡(k, 음수일수록 강한 광각)과 보닛이 차지하는 아래쪽 비율(0.0~0.5)
data class CameraProfile(val k: Double, val cutFraction: Double)

data class FixResult(
    val bitmap: Bitmap,
    val k: Double,
    val bonnetFound: Boolean,
    val cutRows: Int
)

object ImageFix {
    private const val MAX_SIDE = 1600

    fun apply(src: Bitmap, profile: CameraProfile): FixResult {
        val bmp = shrink(src)
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)

        val fixed = if (abs(profile.k) < 0.001) px else undistortRgb(px, w, h, profile.k)

        val cut = (h * profile.cutFraction).toInt().coerceIn(0, h / 2)
        val outH = h - cut
        val out = Bitmap.createBitmap(w, outH, Bitmap.Config.ARGB_8888)
        out.setPixels(fixed, 0, w, 0, 0, w, outH)
        return FixResult(out, profile.k, cut > 0, cut)
    }

    // 예전 화면(FixScreen)이 아직 쓰는 호출 방식. 다음 단계에서 화면을 바꾸면 쓰이지 않아요.
    fun autoFix(src: Bitmap, forceK: Double? = null, cutBonnet: Boolean = true): FixResult =
        apply(src, CameraProfile(forceK ?: -0.03, if (cutBonnet) 0.15 else 0.0))

    private fun shrink(b: Bitmap): Bitmap {
        val m = max(b.width, b.height)
        if (m <= MAX_SIDE) return b
        val s = MAX_SIDE.toDouble() / m
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }

    // 렌즈 왜곡 보정 (배럴 왜곡: k < 0)
    private fun undistortRgb(px: IntArray, w: Int, h: Int, k: Double): IntArray {
        val out = IntArray(w * h)
        val cx = w / 2.0
        val cy = h / 2.0
        val nrm = hypot(cx, cy)
        for (y in 0 until h) for (x in 0 until w) {
            val xu = (x - cx) / nrm
            val yu = (y - cy) / nrm
            val f = 1 + k * (xu * xu + yu * yu)
            val sx = xu * f * nrm + cx
            val sy = yu * f * nrm + cy
            if (sx < 0 || sy < 0 || sx > w - 1 || sy > h - 1) {
                out[y * w + x] = 0xFF000000.toInt()
                continue
            }
            val x0 = sx.toInt(); val y0 = sy.toInt()
            val x1 = min(x0 + 1, w - 1); val y1 = min(y0 + 1, h - 1)
            val fx = (sx - x0).toFloat(); val fy = (sy - y0).toFloat()
            val c00 = px[y0 * w + x0]; val c10 = px[y0 * w + x1]
            val c01 = px[y1 * w + x0]; val c11 = px[y1 * w + x1]
            var res = 0xFF000000.toInt()
            for (shift in intArrayOf(16, 8, 0)) {
                val v =
                    ((c00 shr shift) and 0xFF) * (1 - fx) * (1 - fy) +
                            ((c10 shr shift) and 0xFF) * fx * (1 - fy) +
                            ((c01 shr shift) and 0xFF) * (1 - fx) * fy +
                            ((c11 shr shift) and 0xFF) * fx * fy
                res = res or (v.toInt().coerceIn(0, 255) shl shift)
            }
            out[y * w + x] = res
        }
        return out
    }
}