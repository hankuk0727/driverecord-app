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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrError
import app.driverecord.ui.theme.DrOutline
import app.driverecord.ui.theme.DrTextPrimary
import app.driverecord.ui.theme.DrTextSecondary
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

    // 차량의 주행 신호를 받는다. 차량이 아닌 기기(휴대폰)에서는 available 이 false 로 남는다.
    val monitor = remember { DrivingMonitor(context) }
    DisposableEffect(Unit) {
        monitor.start()
        onDispose { monitor.stop() }
    }
    var manualParked by remember { mutableStateOf(false) }   // 시험용 버튼 값 (차량 신호가 없을 때만 사용)

    // 지금 정차 중인가: 차량 신호가 있으면 그것을, 없으면 시험용 버튼 값을 쓴다
    // (함수로 만든 이유: 핸들 버튼 동작이 항상 최신 값을 읽도록)
    fun parkedNow(): Boolean = if (monitor.available) !monitor.driving else manualParked
    val isParked = parkedNow()

    var frameCount by remember { mutableStateOf(camera.frameCount()) }
    var zoomed by remember { mutableStateOf<DriveMoment?>(null) }
    var recallText by remember { mutableStateOf<String?>(null) }   // 같은 장소 회상 안내
    var demoMode by remember { mutableStateOf(vehicle.isDemoMode()) }
    var demoOpen by remember { mutableStateOf(false) }             // 시연 설정 펼침 (기본 접힘)
    var askReset by remember { mutableStateOf(false) }             // 시연 초기화 확인 창

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
        if (parkedNow()) return false
        val id = nextId
        nextId += 1
        val place = vehicle.currentPlace()   // 이름과 좌표를 한 번에 받는다

        // 이 장면을 넣기 전에, 같은 장소의 예전 장면이 있는지 찾는다
        recallText = PlaceRecall.message(PlaceRecall.find(moments, place), place.name)

        moments.add(
            DriveMoment(
                id = id,
                timeText = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date()),
                driveMode = vehicle.currentDriveMode(),
                placeName = place.name,
                speedKmh = vehicle.currentSpeedKmh(),
                latitude = place.latitude,
                longitude = place.longitude,
                createdAt = System.currentTimeMillis()
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
                if (learned != null && learned >= 0.03) cut = learned   // 너무 작으면 못 찾은 것으로 보고 기본값 사용
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

    // ---- 가로/세로 화면에서 함께 쓰는 조각들 ----

    // 같은 장소 회상 카드
    val recallCard: @Composable () -> Unit = {
        recallText?.let { text ->
            DrCard {
                Text("이곳에서 남긴 장면", style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = DrTextSecondary)
            }
        }
    }

    // 시연 설정: 기본은 접혀 있고, 누르면 펼쳐진다
    val demoCard: @Composable () -> Unit = {
        DrCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable { demoOpen = !demoOpen },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("시연 설정", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (demoOpen) "접기" else "펼치기",
                    style = MaterialTheme.typography.bodySmall,
                    color = DrTextSecondary
                )
            }
            if (demoOpen) {
                DrButton(
                    "시연 카메라 사진 등록 (현재 ${frameCount}장)",
                    onClick = {
                        multiPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (frameCount == 0) {
                    Text(
                        "차량 카메라 대신 쓸 사진을 먼저 등록해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                }
                DrSwitchRow(
                    label = "시연 모드 (장소·속도가 정해진 순서로)",
                    checked = demoMode,
                    onCheckedChange = { on ->
                        demoMode = on
                        vehicle.setDemoMode(on)
                        recallText = null
                    }
                )
                DrDangerButton(
                    "시연 초기화 (장면 모두 지우기)",
                    onClick = { askReset = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "시험용: 키보드 C 키로도 캡처돼요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DrTextSecondary
                )
            }
        }
    }

    // 주행 ↔ 정차 전환 버튼 (시험용). 차량 신호를 받고 있을 때는 보이지 않는다.
    val modeButton: @Composable () -> Unit = {
        if (!monitor.available) {
            DrLargeButton(
                if (isParked) "주행으로 바꾸기" else "정차로 바꾸기",
                onClick = { manualParked = !manualParked },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    val modeNote: @Composable () -> Unit = {
        Text(
            if (monitor.available) "차량 신호에 따라 자동으로 바뀌어요."
            else "※ 위 버튼은 시험용이에요. 차량에서는 신호로 바뀝니다.",
            style = MaterialTheme.typography.bodySmall,
            color = DrTextSecondary,
            textAlign = TextAlign.Center
        )
    }

    // 정차 화면의 장면 카드 목록 (세로·가로가 같이 씀). columns 로 한 줄에 몇 장 놓을지 정한다.
    val momentList: @Composable (Modifier, GridCells) -> Unit = { listModifier, columns ->
        val newestFirst = moments.reversed()
        LazyVerticalGrid(
            columns = columns,
            modifier = listModifier,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // 가로로 넓은 화면(차량 디스플레이 등)이면 2열로
        val wide = maxWidth > maxHeight && maxWidth >= 480.dp

        if (wide && !isParked) {
            // ---- 가로 주행 화면: 왼쪽 큰 캡처 버튼 / 오른쪽 정보와 전환 ----
            Row(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DrCaptureButton("캡처", onClick = { capture() }, modifier = Modifier.fillMaxWidth().weight(1f))
                    Text(
                        "핸들 버튼으로도 저장",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("주행 중", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(12.dp))
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        recallCard()
                        demoCard()
                    }
                    Spacer(Modifier.height(12.dp))
                    modeButton()
                    Spacer(Modifier.height(8.dp))
                    modeNote()
                }
            }
        } else if (wide && isParked) {
            // ---- 가로 정차 화면: 위에 제목 한 줄 / 가운데 장면 카드(폭에 맞춰 여러 열) / 아래 전환 ----
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("정차 중", style = MaterialTheme.typography.headlineSmall)
                    Text("오늘의 장면 ${moments.size}개", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(12.dp))
                momentList(Modifier.weight(1f).fillMaxWidth(), GridCells.Adaptive(560.dp))
                Spacer(Modifier.height(12.dp))
                Box(Modifier.widthIn(max = 560.dp)) { modeButton() }
                Spacer(Modifier.height(8.dp))
                modeNote()
            }
        } else {
            // ---- 세로 화면: 가로 최대 840dp(차량 세로 창 폭)로 가운데 정렬 ----
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (isParked) "정차 중" else "주행 중",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))

                    if (!isParked) {
                        // 내용이 길어져도 아래 버튼이 가려지지 않게 이 부분만 스크롤
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DrCaptureButton("캡처", onClick = { capture() }, modifier = Modifier.fillMaxWidth())
                            Text(
                                "핸들 버튼으로도 저장",
                                style = MaterialTheme.typography.bodySmall,
                                color = DrTextSecondary
                            )
                            recallCard()
                            demoCard()
                        }
                    } else {
                        Text("오늘의 장면 ${moments.size}개", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        momentList(Modifier.weight(1f), GridCells.Fixed(1))
                    }

                    Spacer(Modifier.height(12.dp))
                    modeButton()
                    Spacer(Modifier.height(8.dp))
                    modeNote()
                }
            }
        }
    }

    // 시연 초기화: 실수로 누르지 않게 한 번 더 확인
    if (askReset) {
        AlertDialog(
            onDismissRequest = { askReset = false },
            title = { Text("모든 장면을 지울까요?") },
            text = { Text("저장된 장면 ${moments.size}개와 사진이 모두 지워져요. 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(onClick = {
                    moments.forEach { if (it.photoPath.isNotEmpty()) File(it.photoPath).delete() }
                    moments.clear()
                    store.save(moments)
                    recallText = null
                    vehicle.setDemoMode(demoMode)   // 시연 순서도 처음부터 다시
                    askReset = false
                }) { Text("지우기", color = DrError) }
            },
            dismissButton = {
                TextButton(onClick = { askReset = false }) { Text("취소") }
            }
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

    DrCard {
        Text("장면 %02d".format(number), style = MaterialTheme.typography.titleMedium)

        shown?.let {
            Image(
                it, null,
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPhotoClick),
                contentScale = ContentScale.FillWidth
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("시각: ${moment.timeText}", style = MaterialTheme.typography.bodyMedium, color = DrTextSecondary)
            Text(
                "주행 모드: ${moment.driveMode} · 속도: ${moment.speedKmh}km/h",
                style = MaterialTheme.typography.bodyMedium,
                color = DrTextSecondary
            )
            Text("장소: ${moment.placeName}", style = MaterialTheme.typography.bodyMedium, color = DrTextSecondary)
        }

        DrSwitchRow("차량 정보 스티커", moment.sticker, onStickerChange)

        OutlinedTextField(
            value = moment.memo,
            onValueChange = onMemoChange,
            placeholder = { Text("한 줄 메모") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DrTextPrimary,
                unfocusedTextColor = DrTextPrimary,
                focusedBorderColor = DrAmber,
                unfocusedBorderColor = DrOutline,
                cursorColor = DrAmber,
                focusedPlaceholderColor = DrTextSecondary,
                unfocusedPlaceholderColor = DrTextSecondary
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            DrDangerButton("삭제", onClick = onDelete, modifier = Modifier.weight(1f))
            DrButton(
                "다시 가기",
                onClick = {
                    if (!NaviHelper.open(context, moment)) {
                        Toast.makeText(context, "길 안내를 열 수 없어요", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = NaviHelper.hasLocation(moment)
            )
        }

        ShareButton(moment, Modifier.fillMaxWidth())
    }
}