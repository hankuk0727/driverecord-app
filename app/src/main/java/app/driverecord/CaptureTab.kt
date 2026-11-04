package app.driverecord

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureTab(vehicle: VehicleDataSource, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember { MomentStore(context) }
    val camera = remember { DemoCamera(context) }
    val cameraSettings = remember { CameraSettings(context) }
    val learner = remember { BonnetLearner(context) }
    val moments = remember { mutableStateListOf<DriveMoment>().apply { addAll(store.load()) } }
    var nextId by remember { mutableStateOf((moments.maxOfOrNull { it.id } ?: 0) + 1) }
    var isParked by remember { mutableStateOf(false) }
    var frameCount by remember { mutableStateOf(camera.frameCount()) }
    var zoomed by remember { mutableStateOf<DriveMoment?>(null) }

    val multiPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                camera.register(uris)
                withContext(Dispatchers.Main) { frameCount = camera.frameCount() }
            }
        }
    }

    val tone = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (e: Exception) { null }
    }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

    // 화면 버튼과 핸들 버튼이 함께 쓰는 캡처 동작. 정차 중이면 무시(false).
    fun capture(): Boolean {
        if (isParked) return false
        val id = nextId
        nextId += 1
        val place = vehicle.currentPlace()   // 이름과 좌표를 한 번에 받는다
        moments.add(
            DriveMoment(
                id = id,
                timeText = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date()),
                driveMode = vehicle.currentDriveMode(),
                placeName = place.name,
                speedKmh = vehicle.currentSpeedKmh(),
                latitude = place.latitude,
                longitude = place.longitude
            )
        )
        store.save(moments)
        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)

        // 사진 가져오기 + 보정 + 학습 + 저장은 뒤에서 처리 (캡처 반응은 바로 돌려줌)
        scope.launch(Dispatchers.Default) {
            val frame = camera.nextFrame() ?: return@launch
            val profile = cameraSettings.load()
            val undist = ImageFix.undistorted(frame, profile.k)

            // 보닛 비율: 기본은 사람이 맞춘 값, 자동 학습이 켜져 있고 학습이 되면 학습된 값
            var cut = profile.cutFraction
            if (cameraSettings.autoLearn()) {
                learner.add(undist)
                val learned = learner.learn().cut
                cameraSettings.setLearnedCut(learned)
                if (learned != null) cut = learned
            }

            val fixed = ImageFix.cropBottom(undist, profile.k, cut)
            val dir = File(context.filesDir, "moments").apply { mkdirs() }
            val file = File(dir, "moment_$id.jpg")
            FileOutputStream(file).use { fixed.bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            withContext(Dispatchers.Main) {
                val idx = moments.indexOfFirst { it.id == id }
                if (idx >= 0) {
                    moments[idx] = moments[idx].copy(photoPath = file.path)
                    store.save(moments)
                }
            }
        }
        return true
    }

    DisposableEffect(Unit) {
        WheelKeys.onCapture = { capture() }
        onDispose { WheelKeys.onCapture = null }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            if (isParked) "정차 중" else "주행 중",
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(12.dp))

        if (!isParked) {
            Button(
                onClick = { capture() },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) { Text("캡처", fontSize = 40.sp) }
            Spacer(Modifier.height(8.dp))
            Text(
                "핸들 버튼(시험용: 키보드 C 키)으로도 캡처돼요.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = {
                multiPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("시연 카메라 사진 등록 (현재 ${frameCount}장)") }
            if (frameCount == 0) {
                Text(
                    "차량 카메라 대신 쓸 사진을 먼저 등록해 주세요.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Text("오늘의 장면 ${moments.size}개", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val newestFirst = moments.reversed()
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(newestFirst, key = { _, m -> m.id }) { index, m ->
                    MomentItem(
                        number = newestFirst.size - index,   // 화면에 보이는 번호: 항상 1, 2, 3… 순서대로
                        moment = m,
                        onPhotoClick = { zoomed = m },
                        onDelete = {
                            if (m.photoPath.isNotEmpty()) File(m.photoPath).delete()
                            moments.removeAll { it.id == m.id }
                            store.save(moments)
                        },
                        onMemoChange = { text ->
                            val idx = moments.indexOfFirst { it.id == m.id }
                            if (idx >= 0) {
                                moments[idx] = moments[idx].copy(memo = text)
                                store.save(moments)
                            }
                        },
                        onStickerChange = { on ->
                            val idx = moments.indexOfFirst { it.id == m.id }
                            if (idx >= 0) {
                                moments[idx] = moments[idx].copy(sticker = on)
                                store.save(moments)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { isParked = !isParked }) {
            Text(if (isParked) "주행으로 바꾸기" else "정차로 바꾸기")
        }
        Text(
            "※ 위 버튼은 시험용이에요. 나중에 차량 신호로 바뀝니다.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    // 사진을 누르면 전체 화면으로 크게 보기 (스티커가 켜져 있으면 입혀진 채로)
    zoomed?.let { z ->
        val big = remember(z) {
            if (z.photoPath.isEmpty()) null
            else BitmapFactory.decodeFile(z.photoPath)?.let { b ->
                (if (z.sticker) StickerRender.apply(b, z) else b).asImageBitmap()
            }
        }
        Dialog(
            onDismissRequest = { zoomed = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { zoomed = null },
                contentAlignment = Alignment.Center
            ) {
                if (big != null) {
                    Image(big, null, Modifier.fillMaxWidth(), contentScale = ContentScale.Fit)
                }
                Text(
                    "화면을 누르면 닫혀요",
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MomentItem(
    number: Int,
    moment: DriveMoment,
    onPhotoClick: () -> Unit,
    onDelete: () -> Unit,
    onMemoChange: (String) -> Unit,
    onStickerChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    // 사진은 한 번만 읽고, 스티커는 켜져 있을 때만 그 위에 입혀서 보여줌
    val base = remember(moment.photoPath) {
        if (moment.photoPath.isEmpty()) null
        else {
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeFile(moment.photoPath, opts)
        }
    }
    val shown = remember(base, moment.sticker, moment.memo, moment.speedKmh, moment.timeText) {
        if (base == null) null
        else if (moment.sticker) StickerRender.apply(base, moment).asImageBitmap()
        else base.asImageBitmap()
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("장면 $number", style = MaterialTheme.typography.titleMedium)
            shown?.let {
                Image(
                    it, null,
                    Modifier.fillMaxWidth().clickable(onClick = onPhotoClick),
                    contentScale = ContentScale.FillWidth
                )
            }
            Text("시각: ${moment.timeText}")
            Text("주행 모드: ${moment.driveMode} · 속도: ${moment.speedKmh}km/h")
            Text("장소: ${moment.placeName}")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = moment.sticker, onCheckedChange = onStickerChange)
                Spacer(Modifier.width(8.dp))
                Text("차량 정보 스티커")
            }
            OutlinedTextField(
                value = moment.memo,
                onValueChange = onMemoChange,
                label = { Text("한 줄 메모") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDelete) { Text("삭제") }
                OutlinedButton(
                    enabled = NaviHelper.hasLocation(moment),
                    onClick = {
                        if (!NaviHelper.open(context, moment)) {
                            Toast.makeText(context, "길 안내를 열 수 없어요", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("다시 가기") }
            }
            ShareButton(moment)
        }
    }
}