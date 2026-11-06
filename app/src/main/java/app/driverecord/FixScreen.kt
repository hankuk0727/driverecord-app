package app.driverecord

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrSurfaceHigh
import app.driverecord.ui.theme.DrTextPrimary
import app.driverecord.ui.theme.DrTextSecondary
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
    val car = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) }

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

    // 슬라이더: 채워진 부분은 공식 filled 버튼 색, 나머지 길은 공식 surface/accent 회색
    val sliderColors = SliderDefaults.colors(
        thumbColor = DrAmber,
        activeTrackColor = DrAmber,
        inactiveTrackColor = DrSurfaceHigh
    )
    val sliderHeight = if (car) 64.dp else 48.dp
    val photoShape = RoundedCornerShape(12.dp)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 원본/보정을 나란히 보여 줄 만큼 넓은지 (차량 세로 창 840dp는 위아래로 쌓음)
        val wide = maxWidth >= 900.dp

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = if (wide) 1000.dp else 840.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "사진 보정",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DrTextPrimary
                )
                Text(
                    "차량 카메라는 렌즈 왜곡과 보닛 위치가 고정이에요. 한 번 맞춰 저장하면 같은 카메라의 사진에 자동 적용돼요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DrTextSecondary
                )

                // ---- 보닛 자동 학습 ----
                DrCard {
                    DrSwitchRow(
                        label = "보닛 위치 자동 학습",
                        checked = autoLearn,
                        onCheckedChange = {
                            autoLearn = it
                            settings.setAutoLearn(it)
                        }
                    )
                    Text(
                        "캡처한 사진이 ${BonnetLearner.MIN_FRAMES}장 이상 쌓이면, 매번 변하지 않는 아래쪽을 보닛으로 스스로 찾아요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DrTextSecondary
                    )
                    // 캡처할 때는 3% 미만이면 못 찾은 것으로 보고 슬라이더 값을 쓴다. 안내 문구도 같은 기준으로 맞춘다.
                    val learned = learnStatus.cut
                    Text(
                        when {
                            learnStatus.frames < BonnetLearner.MIN_FRAMES ->
                                "모인 사진 ${learnStatus.frames}장 · 아직 학습 전이에요 (슬라이더 값 사용 중)"
                            learned == null || learned < 0.03 ->
                                "모인 사진 ${learnStatus.frames}장 · 보닛을 찾지 못했어요 (슬라이더 값 사용 중)"
                            else ->
                                "모인 사진 ${learnStatus.frames}장 · 보닛을 찾았어요 (사진 아래 ${(learned * 100).toInt()}% 잘라내는 중)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = DrTextPrimary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DrButton(
                            text = "새로고침",
                            onClick = { refreshKey++ },
                            modifier = Modifier.weight(1f)
                        )
                        DrButton(
                            text = "학습 초기화",
                            onClick = {
                                scope.launch(Dispatchers.IO) {
                                    learner.clear()
                                    settings.setLearnedCut(null)
                                    withContext(Dispatchers.Main) {
                                        refreshKey++
                                        Toast.makeText(context, "학습을 초기화했어요", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ---- 사진 고르기 ----
                DrButton(
                    text = "갤러리에서 사진 고르기",
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (original == null) {
                    Text(
                        "사진을 고르면 저장된 카메라 설정이 바로 적용돼요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DrTextSecondary
                    )
                }

                // ---- 미리보기: 넓으면 나란히, 좁으면 위아래 ----
                val src = original
                if (src != null) {
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("원본", style = MaterialTheme.typography.titleMedium, color = DrTextPrimary)
                                Image(
                                    src.asImageBitmap(), null,
                                    Modifier.fillMaxWidth().clip(photoShape),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("보정", style = MaterialTheme.typography.titleMedium, color = DrTextPrimary)
                                val r = result
                                if (r != null) {
                                    Image(
                                        r.bitmap.asImageBitmap(), null,
                                        Modifier.fillMaxWidth().clip(photoShape),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                if (busy) CircularProgressIndicator(color = DrAmber)
                            }
                        }
                    } else {
                        Text("원본", style = MaterialTheme.typography.titleMedium, color = DrTextPrimary)
                        Image(
                            src.asImageBitmap(), null,
                            Modifier.fillMaxWidth().clip(photoShape),
                            contentScale = ContentScale.FillWidth
                        )
                        if (busy) CircularProgressIndicator(color = DrAmber)
                        result?.let { r ->
                            Text("보정", style = MaterialTheme.typography.titleMedium, color = DrTextPrimary)
                            Image(
                                r.bitmap.asImageBitmap(), null,
                                Modifier.fillMaxWidth().clip(photoShape),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }

                // ---- 보정 값 ----
                DrCard {
                    Text(
                        "왜곡 펴기  ${"%.2f".format(strength)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = DrTextPrimary
                    )
                    Slider(
                        value = strength,
                        onValueChange = { strength = it },
                        valueRange = 0f..0.3f,
                        colors = sliderColors,
                        modifier = Modifier.heightIn(min = sliderHeight)
                    )
                    Text(
                        "아래 보닛 잘라내기  ${(cut * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                        color = DrTextPrimary
                    )
                    Slider(
                        value = cut,
                        onValueChange = { cut = it },
                        valueRange = 0f..0.4f,
                        colors = sliderColors,
                        modifier = Modifier.heightIn(min = sliderHeight)
                    )
                }

                // ---- 저장: 이 화면에서 밝게 채우는 버튼은 이것 하나 ----
                DrPrimaryButton(
                    text = "이 값을 카메라 설정으로 저장",
                    onClick = {
                        settings.save(CameraProfile(-strength.toDouble(), cut.toDouble()))
                        Toast.makeText(context, "카메라 설정으로 저장했어요", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}