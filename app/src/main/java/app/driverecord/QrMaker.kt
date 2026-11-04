package app.driverecord

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

// 휴대폰으로 열 사진 보기 페이지 주소.
// (나중에 GitHub Pages 페이지를 만들면 이 주소만 맞게 바꾸면 된다.)
object ShareLink {
    const val VIEWER = "https://hankuk0727.github.io/driverecord-view/"

    fun url(id: String): String = "$VIEWER?id=$id"
}

object QrMaker {

    // 글자(주소)를 QR 코드 그림으로 만든다. 검정 칸 + 흰 바탕.
    fun make(text: String, size: Int = 720): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 2,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}