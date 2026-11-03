package app.driverecord

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.driverecord.ui.theme.DriveRecordTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vehicle: VehicleDataSource = FakeVehicleData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveRecordTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CaptureScreen(
                        vehicle = vehicle,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun CaptureScreen(vehicle: VehicleDataSource, modifier: Modifier = Modifier) {
    val moments = remember { mutableStateListOf<DriveMoment>() }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.KOREA) }
    var isParked by remember { mutableStateOf(false) }
    var nextId by remember { mutableStateOf(1) }
    val tone = remember {
        try {
            ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            null
        }
    }
    DisposableEffect(Unit) {
        onDispose { tone?.release() }
    }

    Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (isParked) "정차 (P)" else "주행 중", fontSize = 22.sp)
            OutlinedButton(onClick = { isParked = !isParked }) {
                Text(text = if (isParked) "주행으로 바꾸기" else "정차로 바꾸기")
            }
        }
        Text(
            text = "※ 위 버튼은 시험용이에요. 나중에 차량 신호로 바뀝니다.",
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (!isParked) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        moments.add(
                            DriveMoment(
                                id = nextId,
                                timeText = timeFormat.format(Date()),
                                driveMode = vehicle.currentDriveMode(),
                                placeName = vehicle.currentPlaceName()
                            )
                        )
                        nextId += 1
                        tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    },
                    modifier = Modifier.fillMaxWidth().height(160.dp)
                ) {
                    Text(text = "캡처", fontSize = 40.sp)
                }
            }
        } else {
            Text(text = "오늘의 장면 ${moments.size}개", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(12.dp))
            if (moments.isEmpty()) {
                Text(text = "남긴 장면이 없어요. 주행으로 바꿔서 캡처해 보세요.", fontSize = 16.sp)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(moments.reversed(), key = { it.id }) { moment ->
                        MomentCard(
                            moment = moment,
                            onDelete = { moments.remove(moment) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MomentCard(moment: DriveMoment, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "장면 ${moment.id}", fontSize = 20.sp)
            Text(
                text = "시각: ${moment.timeText}",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(text = "주행 모드: ${moment.driveMode}", fontSize = 16.sp)
            Text(text = "장소: ${moment.placeName}", fontSize = 16.sp)
            OutlinedButton(onClick = onDelete, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = "삭제")
            }
        }
    }
}