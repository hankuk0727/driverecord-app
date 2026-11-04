package app.driverecord

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface

object StickerRender {
    // 필름 카메라 날짜 도장처럼 한 줄로: 시각 · 모드 · 속도 · 장소
    fun lines(m: DriveMoment): List<String> {
        val list = mutableListOf("${m.timeText} · ${m.driveMode} ${m.speedKmh}km/h · ${m.placeName}")
        if (m.memo.isNotBlank()) list.add(m.memo)
        return list
    }

    // 사진 오른쪽 아래에 작은 글씨를 입힌 새 사진을 돌려줌 (원본은 그대로)
    fun apply(src: Bitmap, m: DriveMoment): Bitmap {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        val margin = out.width * 0.025f
        val maxTextWidth = out.width * 0.9f

        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(150, 0, 0, 0)
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        }
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 154, 60)   // 필름 날짜 도장의 주황빛
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            textAlign = Paint.Align.RIGHT
        }
        shadow.textAlign = Paint.Align.RIGHT

        val lines = lines(m)
        var size = out.width / 55f   // 작은 글씨
        // 가장 긴 줄이 사진 너비를 넘지 않도록 줄임
        while (size > 8f) {
            text.textSize = size
            if (lines.maxOf { text.measureText(it) } <= maxTextWidth) break
            size -= 1f
        }
        text.textSize = size
        shadow.textSize = size

        val lineH = size * 1.3f
        val offset = size * 0.08f
        // 아래 줄부터 위로 쌓기
        for (i in lines.indices) {
            val y = out.height - margin - lineH * (lines.size - 1 - i)
            val x = out.width - margin
            canvas.drawText(lines[i], x + offset, y + offset, shadow)
            canvas.drawText(lines[i], x, y, text)
        }
        return out
    }
}