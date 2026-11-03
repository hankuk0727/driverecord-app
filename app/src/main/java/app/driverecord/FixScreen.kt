package app.driverecord

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun FixScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var original by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<FixResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var cutBonnet by remember { mutableStateOf(true) }
    var manual by remember { mutableStateOf(false) }
    var manualStrength by remember { mutableStateOf(0.1f) } // k = -값

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            original = bmp
        }
    }

    LaunchedEffect(original, cutBonnet, manual, manualStrength) {
        val src = original ?: return@LaunchedEffect
        busy = true
        result = withContext(Dispatchers.Default) {
            ImageFix.autoFix(src, if (manual) -manualStrength.toDouble() else null, cutBonnet)
        }
        busy = false
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("블박 사진 자동 보정", style = MaterialTheme.typography.headlineSmall)
        Text("광각 왜곡을 펴고, 화면 아래 보닛을 잘라냅니다. 원본은 그대로 남아요.")
        Button(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("사진 고르기") }

        original?.let {
            Text("원본")
            Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        }

        if (busy) CircularProgressIndicator()
        result?.let { r ->
            Text("보정 결과")
            Image(r.bitmap.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            Text("왜곡 값 k = ${"%.2f".format(r.k)} / 보닛 ${if (r.bonnetFound) "찾아서 ${r.cutRows}줄 잘라냄" else "못 찾음(자르지 않음)"}")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = cutBonnet, onCheckedChange = { cutBonnet = it })
            Spacer(Modifier.width(8.dp))
            Text("보닛 자르기")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = manual, onCheckedChange = { manual = it })
            Spacer(Modifier.width(8.dp))
            Text(if (manual) "왜곡 강도 직접 조절" else "왜곡 강도 자동")
        }
        if (manual) Slider(value = manualStrength, onValueChange = { manualStrength = it }, valueRange = 0f..0.4f)

        Button(
            enabled = result != null,
            onClick = {
                val bmp = result?.bitmap ?: return@Button
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val file = File(dir, "fixed_${System.currentTimeMillis()}.jpg")
                FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                Toast.makeText(context, "저장됨: ${file.name}", Toast.LENGTH_LONG).show()
            }
        ) { Text("보정본 저장") }
    }
}