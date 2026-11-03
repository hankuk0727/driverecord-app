package app.driverecord

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

// 시연용 카메라: 갤러리에서 고른 사진들을 앱 안에 복사해 두고, 캡처할 때마다 차례로 꺼내 줌
class DemoCamera(private val context: Context) {
    private val dir = File(context.filesDir, "demo").apply { mkdirs() }
    private var index = 0

    fun frameCount(): Int = dir.listFiles()?.size ?: 0

    fun register(uris: List<Uri>) {
        dir.listFiles()?.forEach { it.delete() }
        uris.forEachIndexed { i, uri ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                File(dir, "frame_$i.jpg").outputStream().use { out -> input.copyTo(out) }
            }
        }
        index = 0
    }

    fun nextFrame(): Bitmap? {
        val files = dir.listFiles()?.sortedBy { it.name } ?: return null
        if (files.isEmpty()) return null
        val f = files[index % files.size]
        index += 1
        return BitmapFactory.decodeFile(f.path)
    }
}