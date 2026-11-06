package app.driverecord

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.PathParser

// 사진 위에 작은 "알약" 모양 정보 라벨 3개를 입힌다.
//  왼쪽 위: 기록 점 + 시각
//  오른쪽 위: 자동차 아이콘 + 속도 | 주행 모드
//  오른쪽 아래: 위치 핀 + 장소 이름 (가장 눈에 띄게)
//  메모가 있으면 장소 알약 바로 위에 글자 알약이 하나 더 붙는다.
object StickerRender {

    // ---- 크기 조절은 여기 숫자만 바꾸면 된다 (dp/sp는 폭 360dp 사진 기준) ----
    private const val PILL_H_DP = 11f        // 알약 높이
    private const val PAD_H_DP = 5f          // 알약 좌우 여백
    private const val INSET_DP = 5f          // 사진 가장자리에서 떨어진 거리
    private const val ICON_DP = 6.5f         // 아이콘 크기
    private const val GAP_DP = 3f            // 아이콘·글자 사이
    private const val STACK_GAP_DP = 2.5f    // 알약 위아래 사이

    private const val SIZE_PLACE = 5.5f      // 장소 글자 (제일 크게)
    private const val SIZE_MAIN = 4.8f       // 시각·속도·모드·메모
    private const val SIZE_UNIT = 3.6f       // km/h (작게)

    private const val PILL_ALPHA = 205       // 알약 바탕 진하기 (0 투명 ~ 255 불투명)
    // --------------------------------------------------------------------------

    private val PILL_COLOR = Color.argb(PILL_ALPHA, 52, 66, 96)   // 반투명 슬레이트 블루

    // Material 아이콘 모양 (24x24 격자)
    private const val PIN_PATH =
        "M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"
    private const val CAR_PATH =
        "M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"

    private enum class Kind { TEXT, DOT, PIN, CAR, DIVIDER }

    // 알약 안에 들어가는 조각 하나 (아이콘, 글자, 구분선)
    private class Seg(val kind: Kind, val text: String = "", val paint: Paint? = null, val after: Float = 0f)

    // 머리말(시각·모드·속도)과 꼬리(장소). 다른 곳에서 글자만 필요할 때 쓰는 함수
    private fun timeOf(m: DriveMoment): String =
        if (m.timeText.length == 8 && m.timeText[2] == ':') m.timeText.take(5) else m.timeText

    fun lines(m: DriveMoment): List<String> {
        val list = mutableListOf("${timeOf(m)} · ${m.driveMode} ${m.speedKmh}km/h · ${m.placeName}")
        if (m.memo.isNotBlank()) list.add(m.memo)
        return list
    }

    // 너비에 맞게 "…"로 줄인 글자
    private fun ellipsize(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var t = text
        while (t.isNotEmpty() && paint.measureText(t + "…") > maxWidth) {
            t = t.dropLast(1)
        }
        return t + "…"
    }

    // displayWidthDp: 사진이 화면에 보이는 너비(dp).
    // 글자 크기는 사진 너비에 비례해서 정해서, 미리보기와 저장·공유 이미지의 모양이 같다.
    fun apply(src: Bitmap, m: DriveMoment, displayWidthDp: Float = 360f): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val unit = out.width / displayWidthDp
        val scale = if (displayWidthDp >= 600f) 1.17f else 1f
        val u = unit * scale

        val pillH = PILL_H_DP * u
        val padH = PAD_H_DP * u
        val inset = INSET_DP * u
        val iconSize = ICON_DP * u
        val gap = GAP_DP * u
        val stackGap = STACK_GAP_DP * u

        fun paint(sizeSp: Float, weight: Int, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(alpha, 255, 255, 255)
            typeface = Typeface.create(Typeface.DEFAULT, weight, false)
            textSize = sizeSp * u
        }

        val pMain = paint(SIZE_MAIN, 500)
        val pPlace = paint(SIZE_PLACE, 600)
        val pUnit = paint(SIZE_UNIT, 500, 190)
        val pMode = paint(SIZE_MAIN, 500, 230)
        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val divPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(110, 255, 255, 255) }
        val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PILL_COLOR }

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        val pinPath = PathParser.createPathFromPathData(PIN_PATH).apply { fillType = Path.FillType.EVEN_ODD }
        val carPath = PathParser.createPathFromPathData(CAR_PATH).apply { fillType = Path.FillType.EVEN_ODD }

        fun segWidth(s: Seg): Float = when (s.kind) {
            Kind.TEXT -> s.paint!!.measureText(s.text)
            Kind.DIVIDER -> 1f * u
            else -> iconSize
        }

        // 알약 하나를 그린다. alignRight가 true면 anchorX가 오른쪽 끝, false면 왼쪽 끝.
        fun drawPill(segs: List<Seg>, anchorX: Float, top: Float, alignRight: Boolean) {
            var contentW = 0f
            for ((i, s) in segs.withIndex()) {
                contentW += segWidth(s)
                if (i < segs.size - 1) contentW += s.after
            }
            val w = contentW + 2f * padH
            val left = if (alignRight) anchorX - w else anchorX
            val rect = RectF(left, top, left + w, top + pillH)
            canvas.drawRoundRect(rect, pillH / 2f, pillH / 2f, pillPaint)

            var x = left + padH
            val cy = top + pillH / 2f
            for ((i, s) in segs.withIndex()) {
                when (s.kind) {
                    Kind.TEXT -> {
                        val p = s.paint!!
                        val fm = p.fontMetrics
                        val baseline = cy - (fm.descent + fm.ascent) / 2f
                        canvas.drawText(s.text, x, baseline, p)
                    }
                    Kind.DOT -> canvas.drawCircle(x + iconSize / 2f, cy, iconSize * 0.3f, white)
                    Kind.PIN, Kind.CAR -> {
                        val path = Path()
                        val mat = Matrix()
                        val k = iconSize / 24f
                        mat.setScale(k, k)
                        mat.postTranslate(x, cy - iconSize / 2f)
                        (if (s.kind == Kind.PIN) pinPath else carPath).transform(mat, path)
                        canvas.drawPath(path, iconPaint)
                    }
                    Kind.DIVIDER -> {
                        val hh = pillH * 0.45f
                        canvas.drawRect(x, cy - hh / 2f, x + 1f * u, cy + hh / 2f, divPaint)
                    }
                }
                x += segWidth(s)
                if (i < segs.size - 1) x += s.after
            }
        }

        val right = out.width - inset
        val maxBox = out.width - 2f * inset - 2f * padH

        // 1) 왼쪽 위: 기록 점 + 시각
        drawPill(
            listOf(
                Seg(Kind.DOT, after = gap),
                Seg(Kind.TEXT, timeOf(m), pMain)
            ),
            anchorX = inset, top = inset, alignRight = false
        )

        // 2) 오른쪽 위: 자동차 + 속도(km/h는 작게) | 주행 모드
        drawPill(
            listOf(
                Seg(Kind.CAR, after = gap),
                Seg(Kind.TEXT, "${m.speedKmh}", pMain, after = 2f * u),
                Seg(Kind.TEXT, "km/h", pUnit, after = gap + 1f * u),
                Seg(Kind.DIVIDER, after = gap + 1f * u),
                Seg(Kind.TEXT, m.driveMode, pMode)
            ),
            anchorX = right, top = inset, alignRight = true
        )

        // 3) 오른쪽 아래: 위치 핀 + 장소 이름 (길면 끝을 …로 줄임)
        val placeText = ellipsize(pPlace, m.placeName, maxBox - iconSize - gap)
        val placeTop = out.height - inset - pillH
        drawPill(
            listOf(
                Seg(Kind.PIN, after = gap),
                Seg(Kind.TEXT, placeText, pPlace)
            ),
            anchorX = right, top = placeTop, alignRight = true
        )

        // 4) 메모가 있으면 장소 알약 위에 글자 알약 하나 더
        if (m.memo.isNotBlank()) {
            val memoText = ellipsize(pMain, m.memo, maxBox)
            drawPill(
                listOf(Seg(Kind.TEXT, memoText, pMain)),
                anchorX = right, top = placeTop - stackGap - pillH, alignRight = true
            )
        }
        return out
    }
}