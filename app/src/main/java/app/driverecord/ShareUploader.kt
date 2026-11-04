package app.driverecord

import android.graphics.Bitmap
import android.util.Base64
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Date

// 사진을 줄여서 Firebase(Firestore)에 올리고, 사진 번호(id)를 돌려준다.
object ShareUploader {

    private const val MAX_BASE64 = 850_000   // Firestore 문서 한도(약 1MB)보다 작게
    private const val KEEP_HOURS = 24L       // 올린 사진을 보여주는 시간
    private const val CHARS = "abcdefghijklmnopqrstuvwxyz0123456789"

    // 실패하면 예외가 나므로, 부르는 쪽에서 try/catch 해야 한다.
    suspend fun upload(photo: Bitmap): String {
        val image = withContext(Dispatchers.Default) { toBase64(photo) }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }

        val id = newId()
        val now = System.currentTimeMillis()
        val data = hashMapOf(
            "image" to image,
            "createdAt" to Timestamp(Date(now)),
            "expiresAt" to Timestamp(Date(now + KEEP_HOURS * 3600_000L))
        )
        FirebaseFirestore.getInstance()
            .collection("shares")
            .document(id)
            .set(data)
            .await()
        return id
    }

    private fun newId(): String {
        val random = SecureRandom()
        return buildString {
            repeat(20) { append(CHARS[random.nextInt(CHARS.length)]) }
        }
    }

    // 크기가 한도 안에 들어올 때까지 화질과 크기를 조금씩 낮춘다.
    private fun toBase64(src: Bitmap): String {
        var side = 1280
        while (true) {
            val bmp = scaled(src, side)
            var quality = 85
            while (quality >= 45) {
                val out = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
                val text = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (text.length <= MAX_BASE64) return text
                quality -= 10
            }
            if (side <= 480) error("사진이 너무 커서 줄일 수 없어요")
            side = side * 3 / 4
        }
    }

    private fun scaled(src: Bitmap, side: Int): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= side) return src
        val ratio = side.toFloat() / longest
        return Bitmap.createScaledBitmap(
            src,
            (src.width * ratio).toInt().coerceAtLeast(1),
            (src.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}