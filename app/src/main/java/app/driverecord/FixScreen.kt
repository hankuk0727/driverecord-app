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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FixScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember { CameraSettings(context) }
    val learner = remember { BonnetLearner(context) }
    val saved = remember { settings.load() }

    var original by remember { mutableStateOf<Bitmap?>(null) }
    var result by remember { mutableStateOf<FixResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var strength by remember { mutableStateOf((-saved.k).toFloat()) }   // k = -strength
    var cut by remember { mutableStateOf(saved.cutFraction.toFloat()) }

    var autoLearn by remember { mutableStateOf(settings.autoLearn()) }
    var learnStatus by remember { mutableStateOf(BonnetLearner.Result(0, null)) }
    var refreshKey by remember { mutableStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            original = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }

    // 화면이 열리거나 새로고침할 때 학습 상태를 읽어 옴
    LaunchedEffect(refreshKey) {
        learnStatus = withContext(Dispatchers.IO) { learner.learn() }
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
        Text("차량 카메라는 렌즈 왜곡과 보닛 위치가 고정이에요. 한 번 맞춰 저장하면 같은 카메라의 사진에 자동 적용돼요.")

        // ---- 보닛 자동 학습 ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoLearn, onCheckedChange = {
                        autoLearn = it
                        settings.setAutoLearn(it)
                    })
                    Spacer(Modifier.width(8.dp))
                    Text("보닛 위치 자동 학습")
                }
                Text(
                    "캡처한 사진이 ${BonnetLearner.MIN_FRAMES}장 이상 쌓이면, 매번 변하지 않는 아래쪽을 보닛으로 스스로 찾아요.",
                    style = MaterialTheme.typography.bodySmall
                )
                val learned = learnStatus.cut
                Text(
                    when {
                        learnStatus.frames < BonnetLearner.MIN_FRAMES ->
                            "모인 사진 ${learnStatus.frames}장 · 아직 학습 전 (슬라이더 값 사용 중)"
                        learned == null ->
                            "모인 사진 ${learnStatus.frames}장 · 장면 변화가 적어 학습 못 함 (슬라이더 값 사용 중)"
                        else ->
                            "모인 사진 ${learnStatus.frames}장 · 학습된 보닛 ${(learned * 100).toInt()}% 적용 중"
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { refreshKey++ }) { Text("새로고침") }
                    OutlinedButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            learner.clear()
                            settings.setLearnedCut(null)
                            withContext(Dispatchers.Main) {
                                refreshKey++
                                Toast.makeText(context, "학습을 초기화했어요", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) { Text("학습 초기화") }
                }
            }
        }

        // ---- 수동 설정 ----
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