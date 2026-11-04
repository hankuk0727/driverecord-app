package app.driverecord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 차량 화면 느낌의 얇은 선 아이콘. 이미지 파일 없이 코드로 그린다.
// 24칸짜리 격자에 그린 뒤 원하는 크기로 늘린다.

enum class DrIconKind { Camera, Tune, Car, Phone, Pin }

@Composable
fun DrIcon(
    kind: DrIconKind,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp
) {
    Canvas(modifier.size(iconSize)) {
        val s = this.size.width / 24f
        val w = 1.8f * s
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)

        fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
            drawLine(tint, Offset(x1 * s, y1 * s), Offset(x2 * s, y2 * s), strokeWidth = w, cap = StrokeCap.Round)
        }
        fun circle(cx: Float, cy: Float, r: Float) {
            drawCircle(tint, r * s, Offset(cx * s, cy * s), style = stroke)
        }
        fun roundRect(x: Float, y: Float, wd: Float, ht: Float, r: Float) {
            drawRoundRect(
                tint,
                topLeft = Offset(x * s, y * s),
                size = Size(wd * s, ht * s),
                cornerRadius = CornerRadius(r * s),
                style = stroke
            )
        }

        when (kind) {
            DrIconKind.Camera -> {
                roundRect(3f, 7f, 18f, 12f, 3f)
                val p = Path().apply {
                    moveTo(8f * s, 7f * s)
                    lineTo(9.5f * s, 4.5f * s)
                    lineTo(14.5f * s, 4.5f * s)
                    lineTo(16f * s, 7f * s)
                }
                drawPath(p, tint, style = stroke)
                circle(12f, 13f, 3.5f)
            }
            DrIconKind.Tune -> {
                line(3f, 6f, 5.8f, 6f);   line(10.2f, 6f, 21f, 6f);   circle(8f, 6f, 2.2f)
                line(3f, 12f, 13.8f, 12f); line(18.2f, 12f, 21f, 12f); circle(16f, 12f, 2.2f)
                line(3f, 18f, 7.8f, 18f);  line(12.2f, 18f, 21f, 18f); circle(10f, 18f, 2.2f)
            }
            DrIconKind.Car -> {
                val p = Path().apply {
                    moveTo(3f * s, 16f * s)
                    lineTo(3f * s, 12.5f * s)
                    lineTo(5.5f * s, 8f * s)
                    lineTo(18.5f * s, 8f * s)
                    lineTo(21f * s, 12.5f * s)
                    lineTo(21f * s, 16f * s)
                }
                drawPath(p, tint, style = stroke)
                line(3f, 12.5f, 21f, 12.5f)
                line(3f, 16f, 5.5f, 16f)
                line(9.5f, 16f, 14.5f, 16f)
                line(18.5f, 16f, 21f, 16f)
                circle(7.5f, 16f, 2f)
                circle(16.5f, 16f, 2f)
            }
            DrIconKind.Phone -> {
                roundRect(7f, 3f, 10f, 18f, 2.5f)
                line(10.5f, 18f, 13.5f, 18f)
            }
            DrIconKind.Pin -> {
                val p = Path().apply {
                    moveTo(12f * s, 21f * s)
                    cubicTo(12f * s, 21f * s, 5f * s, 14.8f * s, 5f * s, 10f * s)
                    cubicTo(5f * s, 6.1f * s, 8.1f * s, 3f * s, 12f * s, 3f * s)
                    cubicTo(15.9f * s, 3f * s, 19f * s, 6.1f * s, 19f * s, 10f * s)
                    cubicTo(19f * s, 14.8f * s, 12f * s, 21f * s, 12f * s, 21f * s)
                }
                drawPath(p, tint, style = stroke)
                circle(12f, 10f, 2.5f)
            }
        }
    }
}