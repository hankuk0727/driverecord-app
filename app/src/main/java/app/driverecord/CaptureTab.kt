package app.driverecord

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaptureTab(vehicle: VehicleDataSource, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val store = remember { MomentStore(context) }
    val moments = remember { mutableStateListOf<DriveMoment>().apply { addAll(store.load()) } }
    var nextId by remember { mutableStateOf((moments.maxOfOrNull { it.id } ?: 0) + 1) }
    var isParked by remember { mutableStateOf(false) }

    val tone = remember {
        try { ToneGenerator(AudioManager.STREAM_MUSIC, 80) } catch (e: Exception) { null }
    }
    DisposableEffect(Unit) { onDispose { tone?.release() } }

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
                onClick = {
                    val m = DriveMoment(
                        id = nextId,
                        timeText = SimpleDateFormat("HH:mm:ss", Locale.KOREA).format(Date()),
                        driveMode = vehicle.currentDriveMode(),
                        placeName = vehicle.currentPlaceName()
                    )
                    moments.add(m)
                    store.save(moments)
                    nextId += 1
                    tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                },
                modifier = Modifier.fillMaxWidth().height(160.dp)
            ) { Text("캡처", fontSize = 40.sp) }
        } else {
            Text("오늘의 장면 ${moments.size}개", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(moments.reversed(), key = { it.id }) { m ->
                    MomentItem(
                        moment = m,
                        onDelete = {
                            moments.removeAll { it.id == m.id }
                            store.save(moments)
                        },
                        onMemoChange = { text ->
                            val idx = moments.indexOfFirst { it.id == m.id }
                            if (idx >= 0) {
                                moments[idx] = moments[idx].copy(memo = text)
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
}

@Composable
private fun MomentItem(
    moment: DriveMoment,
    onDelete: () -> Unit,
    onMemoChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("장면 ${moment.id}", style = MaterialTheme.typography.titleMedium)
            Text("시각: ${moment.timeText}")
            Text("주행 모드: ${moment.driveMode}")
            Text("장소: ${moment.placeName}")
            OutlinedTextField(
                value = moment.memo,
                onValueChange = onMemoChange,
                label = { Text("한 줄 메모") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = onDelete) { Text("삭제") }
        }
    }
}