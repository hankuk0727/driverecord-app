package app.driverecord

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                moments.add(
                    DriveMoment(
                        id = moments.size + 1,
                        timeText = timeFormat.format(Date()),
                        driveMode = vehicle.currentDriveMode(),
                        placeName = vehicle.currentPlaceName()
                    )
                )
            },
            modifier = Modifier.fillMaxWidth().height(160.dp)
        ) {
            Text(text = "캡처", fontSize = 40.sp)
        }
        Text(
            text = "남긴 장면: ${moments.size}개",
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 24.dp)
        )
        val last = moments.lastOrNull()
        if (last != null) {
            Text(
                text = "마지막 기록: ${last.timeText} / ${last.driveMode} / ${last.placeName}",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}