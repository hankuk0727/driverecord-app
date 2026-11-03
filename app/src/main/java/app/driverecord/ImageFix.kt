package app.driverecord

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class FixResult(
    val bitmap: Bitmap,
    val k: Double,
    val bonnetFound: Boolean,
    val cutRows: Int
)

object ImageFix {
    private const val MAX_SIDE = 1600

    fun autoFix(src: Bitmap, forceK: Double? = null, cutBonnet: Boolean = true): FixResult {
        val bmp = shrink(src)
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val gray = FloatArray(w * h) { luma(px[it]) }

        val k = forceK ?: estimateK(gray, w, h)
        val fixed = undistortRgb(px, w, h, k)

        var outH = h
        var found = false
        if (cutBonnet) {
            val g2 = FloatArray(w * h) { luma(fixed[it]) }
            val top = findBonnetTop(g2, w, h)
            if (top > 0) {
                outH = max(top - 4, h / 2)
                found = true
            }
        }
        val out = Bitmap.createBitmap(w, outH, Bitmap.Config.ARGB_8888)
        out.setPixels(fixed, 0, w, 0, 0, w, outH)
        return FixResult(out, k, found, h - outH)
    }

    private fun shrink(b: Bitmap): Bitmap {
        val m = max(b.width, b.height)
        if (m <= MAX_SIDE) return b
        val s = MAX_SIDE.toDouble() / m
        return Bitmap.createScaledBitmap(b, (b.width * s).toInt(), (b.height * s).toInt(), true)
    }

    private fun luma(c: Int): Float {
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = c and 0xFF
        return (0.299f * r + 0.587f * g + 0.114f * b)
    }

    // ---- 렌즈 왜곡 보정 (배럴 왜곡: k < 0) ----
    private fun undistortGray(g: FloatArray, w: Int, h: Int, k: Double): FloatArray {
        val out = FloatArray(w * h)
        val cx = w / 2.0
        val cy = h / 2.0
        val nrm = hypot(cx, cy)
        for (y in 0 until h) for (x in 0 until w) {
            val xu = (x - cx) / nrm
            val yu = (y - cy) / nrm
            val f = 1 + k * (xu * xu + yu * yu)
            val sx = xu * f * nrm + cx
            val sy = yu * f * nrm + cy
            if (sx < 0 || sy < 0 || sx > w - 1 || sy > h - 1) continue
            val x0 = sx.toInt(); val y0 = sy.toInt()
            val x1 = min(x0 + 1, w - 1); val y1 = min(y0 + 1, h - 1)
            val fx = (sx - x0).toFloat(); val fy = (sy - y0).toFloat()
            out[y * w + x] =
                g[y0 * w + x0] * (1 - fx) * (1 - fy) + g[y0 * w + x1] * fx * (1 - fy) +
                        g[y1 * w + x0] * (1 - fx) * fy + g[y1 * w + x1] * fx * fy
        }
        return out
    }

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

    // ---- 왜곡 정도 자동 추정: 보정했을 때 직선이 가장 곧게 되는 k ----
    private fun estimateK(gray: FloatArray, w: Int, h: Int): Double {
        val sw = w / 2
        val sh = h / 2
        val small = FloatArray(sw * sh)
        for (y in 0 until sh) for (x in 0 until sw) small[y * sw + x] = gray[(2 * y) * w + 2 * x]
        var bestK = -0.01
        var bestScore = -1.0
        for (i in 0..39) {
            val k = -0.40 + 0.01 * i
            val s = straightness(small, sw, sh, k)
            if (s > bestScore) { bestScore = s; bestK = k }
        }
        return bestK
    }

    private fun straightness(g0: FloatArray, w: Int, h: Int, k: Double): Double {
        val g = undistortGray(g0, w, h, k)
        val b = FloatArray(w * h)
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            b[y * w + x] = (g[y * w + x] + g[(y - 1) * w + x] + g[(y + 1) * w + x] +
                    g[y * w + x - 1] + g[y * w + x + 1]) / 5f
        }
        val limit = (h * 0.75).toInt()
        val cs = DoubleArray(w)
        val rs = DoubleArray(h)
        for (y in 1 until limit) for (x in 1 until w - 1) {
            cs[x] += abs(b[y * w + x + 1] - b[y * w + x]).toDouble()
            rs[y] += abs(b[(y + 1) * w + x] - b[y * w + x]).toDouble()
        }
        return concentration(cs) + concentration(rs)
    }

    private fun concentration(a: DoubleArray): Double {
        var s = 0.0
        var s2 = 0.0
        for (v in a) { s += v; s2 += v * v }
        return s2 / (s * s + 1e-9)
    }

    // ---- 보닛 윗선 찾기 (찾으면 행 번호, 못 찾으면 -1) ----
    private fun findBonnetTop(g: FloatArray, w: Int, h: Int): Int {
        val strips = 40
        val sw = w / strips
        val y0 = (h * 0.55).toInt()
        val tops = IntArray(strips)
        val edges = DoubleArray(strips)
        val sds = DoubleArray(strips)
        val col = DoubleArray(h)
        val s1 = DoubleArray(h + 1)
        val s2 = DoubleArray(h + 1)
        for (i in 0 until strips) {
            for (y in 0 until h) {
                var s = 0.0
                for (x in i * sw until (i + 1) * sw) s += g[y * w + x]
                col[y] = s / sw
            }
            for (y in h - 1 downTo 0) {
                s1[y] = s1[y + 1] + col[y]
                s2[y] = s2[y + 1] + col[y] * col[y]
            }
            var best = -1.0
            var by = y0
            var be = 0.0
            var bs = 0.0
            for (y in y0 until h - 8) {
                val n = (h - (y + 2)).toDouble()
                val mean = s1[y + 2] / n
                val sd = sqrt(max(0.0, s2[y + 2] / n - mean * mean))
                var e = 0.0
                for (d in -1..1) e = max(e, abs(col[y + d + 1] - col[y + d]))
                val sc = e / (1 + sd)
                if (sc > best) { best = sc; by = y; be = e; bs = sd }
            }
            tops[i] = by; edges[i] = be; sds[i] = bs
        }
        val medEdge = edges.sorted()[strips / 2]
        val medSd = sds.sorted()[strips / 2]
        if (medEdge < 10.0 || medSd > 35.0) return -1
        return tops.sorted()[strips / 10]
    }
}