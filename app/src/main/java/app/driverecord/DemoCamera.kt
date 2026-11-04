package app.driverecord

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

// 시연용 카메라.
// - 갤러리에서 직접 등록한 사진이 있으면 그것을 차례로 꺼내 줌
// - 없으면 앱에 들어 있는 샘플 사진(assets/demo 폴더)을 차례로 꺼내 줌
//   → 심사자가 APK만 설치해도 바로 캡처를 해 볼 수 있음
// 샘플 사진은 모두 같은 카메라로 찍은 것이어야 보닛 자동 학습이 맞게 작동한다.
class DemoCamera(private val context: Context) {
    private val dir = File(context.filesDir, "demo").apply { mkdirs() }
    private var index = 0

    private fun registeredFiles(): List<File> =
        dir.listFiles()?.sortedBy { it.name } ?: emptyList()

    // 앱에 들어 있는 샘플 사진 이름들 (assets/demo)
    private fun bundledNames(): List<String> =
        (context.assets.list("demo") ?: emptyArray())
            .filter {
                val n = it.lowercase()
                n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
            }
            .sorted()

    // 쓸 수 있는 사진 수 (직접 등록한 사진이 없으면 샘플 사진 수)
    fun frameCount(): Int {
        val n = registeredFiles().size
        return if (n > 0) n else bundledNames().size
    }

    // 직접 등록했는지 (false면 샘플 사진을 쓰는 중)
    fun hasRegistered(): Boolean = registeredFiles().isNotEmpty()

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
        val files = registeredFiles()
        val bmp = if (files.isNotEmpty()) {
            BitmapFactory.decodeFile(files[index % files.size].path)
        } else {
            val names = bundledNames()
            if (names.isEmpty()) null
            else context.assets.open("demo/" + names[index % names.size]).use {
                BitmapFactory.decodeStream(it)
            }
        }
        index += 1
        return bmp
    }
}