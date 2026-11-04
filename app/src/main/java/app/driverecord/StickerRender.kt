package app.driverecord

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

// 사진 오른쪽 아래에 작은 "사진 서명" 같은 글씨를 입힌다. (아주 연한 반투명 바탕 + 얇은 그림자)
object StickerRender {

    // ---- 크기 조절은 여기 숫자만 바꾸면 된다 (dp/sp는 폭 360dp 사진 기준) ----
    private const val TEXT_SP = 9f            // 글자 크기 (사진 폭이 600dp 이상이면 ×1.17)
    private const val LINE_DP = 12f           // 줄 높이
    private const val PAD_H_DP = 3f           // 바탕 좌우 여백
    private const val PAD_V_DP = 1.5f         // 바탕 위아래 여백
    private const val INSET_DP = 8f           // 사진 가장자리에서 떨어진 거리
    private const val CORNER_DP = 3f          // 바탕 모서리
    private const val BACK_ALPHA = 110        // 바탕 진하기 (0 투명 ~ 255 불투명)
    private const val SHADOW_DP = 1.5f        // 그림자 퍼짐
    private const val SHADOW_ALPHA = 200      // 그림자 진하기
    // --------------------------------------------------------------------------

    private val INK = Color.rgb(232, 217, 191)                    // 글자: 크림색 #E8D9BF
    private val BACK = Color.argb(BACK_ALPHA, 20, 18, 16)         // 바탕: 어두운 갈색(반투명)

    // 머리말(시각·모드·속도)과 꼬리(장소). 길면 꼬리만 줄인다.
    private fun parts(m: DriveMoment): List<Pair<String, String>> {
        val time = if (m.timeText.length == 8 && m.timeText[2] == ':') m.timeText.take(5) else m.timeText
        val list = mutableListOf("$time · ${m.driveMode} ${m.speedKmh}km/h · " to m.placeName)
        if (m.memo.isNotBlank()) list.add("" to m.memo)
        return list
    }

    // 다른 곳에서 글자만 필요할 때 쓰는 함수
    fun lines(m: DriveMoment): List<String> = parts(m).map { it.first + it.second }

    // 너비에 맞게 꼬리만 "…"로 줄인 한 줄을 만든다
    private fun fit(paint: Paint, head: String, tail: String, maxWidth: Float): String {
        if (paint.measureText(head + tail) <= maxWidth) return head + tail
        var t = tail
        while (t.isNotEmpty() && paint.measureText(head + t + "…") > maxWidth) {
            t = t.dropLast(1)
        }
        return head + t + "…"
    }

    // displayWidthDp: 사진이 화면에 보이는 너비(dp).
    // 글자 크기는 사진 너비에 비례해서 정해서, 미리보기와 저장·공유 이미지의 모양이 같다.
    fun apply(src: Bitmap, m: DriveMoment, displayWidthDp: Float = 360f): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val unit = out.width / displayWidthDp               // 1dp가 몇 픽셀인지
        val scale = if (displayWidthDp >= 600f) 1.17f else 1f
        val lineHeight = LINE_DP * scale * unit
        val padH = PAD_H_DP * scale * unit
        val padV = PAD_V_DP * scale * unit
        val inset = INSET_DP * scale * unit
        val corner = CORNER_DP * scale * unit
        val gap = 1.5f * unit
        val shadowR = SHADOW_DP * scale * unit

        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = INK
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textSize = TEXT_SP * scale * unit
            setShadowLayer(shadowR, shadowR * 0.3f, shadowR * 0.3f, Color.argb(SHADOW_ALPHA, 0, 0, 0))
        }
        val back = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BACK }

        // 바탕 최대 너비: 사진 너비의 90%, 그리고 좌우 인셋을 침범하지 않게
        val maxBox = minOf(out.width * 0.9f, out.width - 2f * inset)
        val maxText = maxBox - 2f * padH

        val rows = parts(m).map { fit(ink, it.first, it.second, maxText) }
        val boxH = lineHeight + 2f * padV
        val fm = ink.fontMetrics

        // 아래 줄부터 위로 쌓기
        for (i in rows.indices) {
            val fromBottom = rows.size - 1 - i
            val text = rows[i]
            val boxW = ink.measureText(text) + 2f * padH
            val right = out.width - inset
            val bottom = out.height - inset - fromBottom * (boxH + gap)
            val rect = RectF(right - boxW, bottom - boxH, right, bottom)
            canvas.drawRoundRect(rect, corner, corner, back)

            val baseline = rect.top + padV + (lineHeight - (fm.descent - fm.ascent)) / 2f - fm.ascent
            canvas.drawText(text, rect.left + padH, baseline, ink)
        }
        return out
    }
}