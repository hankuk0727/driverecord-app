package app.driverecord

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface

object StickerRender {
    // 스티커에 들어갈 글자들
    fun lines(m: DriveMoment): List<String> {
        val list = mutableListOf(
            m.timeText,
            "${m.driveMode} 모드 · ${m.speedKmh}km/h",
            m.placeName
        )
        if (m.memo.isNotBlank()) list.add(m.memo)
        return list
    }

    // 사진 왼쪽 아래에 스티커를 그린 새 사진을 돌려줌 (원본은 그대로)
    fun apply(src: Bitmap, m: DriveMoment): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val unit = out.width / 40f

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = unit * 1.6f
            typeface = Typeface.DEFAULT_BOLD
        }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(170, 0, 0, 0)
        }

        val lines = lines(m)
        val pad = unit
        val lineH = textPaint.textSize * 1.35f
        val boxW = lines.maxOf { textPaint.measureText(it) } + pad * 2
        val boxH = lineH * lines.size + pad * 2
        val left = unit
        val top = out.height - boxH - unit

        canvas.drawRoundRect(RectF(left, top, left + boxW, top + boxH), unit, unit, bgPaint)
        lines.forEachIndexed { i, s ->
            canvas.drawText(s, left + pad, top + pad + textPaint.textSize * 0.85f + lineH * i, textPaint)
        }
        return out
    }
}