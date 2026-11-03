package app.driverecord

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun FixScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings = remember { CameraSettings(context) }
    val saved = remember { settings.load() }

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<FixResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var strength by remember { mutableStateOf((-saved.k).toFloat()) }   // k = -strength
    var cut by remember { mutableStateOf(saved.cutFraction.toFloat()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }

    // 값이 바뀔 때마다 다시 계산 (너무 잦은 계산을 막기 위해 잠깐 기다림)
    LaunchedEffect(original, strength, cut) {
        val src = original ?: return@LaunchedEffect
        delay(120)
        busy = true
        result = withContext(Dispatchers.Default) {
            ImageFix.apply(src, CameraProfile(-strength.toDouble(), cut.toDouble()))
        }
        busy = false
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("카메라 설정 · 사진 보정", style = MaterialTheme.typography.headlineSmall)
        Text("차량 카메라는 렌즈 왜곡과 보닛 위치가 고정이에요. 한 번 맞춰 저장하면, 이후 같은 카메라의 사진에는 저장된 값이 자동으로 적용돼요.")

        Button(onClick = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }) { Text("갤러리에서 사진 고르기") }

        if (original == null) {
            Text("사진을 고르면 저장된 카메라 설정이 바로 적용돼요.", style = MaterialTheme.typography.bodySmall)
        }

        original?.let {
            Text("원본")
            Image(it.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        }

        if (busy) CircularProgressIndicator()
        result?.let { r ->
            Text("보정 결과")
            Image(r.bitmap.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
        }

        Text("왜곡 펴기: ${"%.2f".format(strength)}")
        Slider(value = strength, onValueChange = { strength = it }, valueRange = 0f..0.3f)

        Text("아래 보닛 잘라내기: ${(cut * 100).toInt()}%")
        Slider(value = cut, onValueChange = { cut = it }, valueRange = 0f..0.4f)

        Button(onClick = {
            settings.save(CameraProfile(-strength.toDouble(), cut.toDouble()))
            Toast.makeText(context, "카메라 설정으로 저장했어요", Toast.LENGTH_SHORT).show()
        }) { Text("이 값을 카메라 설정으로 저장") }
    }
}