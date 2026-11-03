package app.driverecord

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.driverecord.ui.theme.DriveRecordTheme

class MainActivity : ComponentActivity() {
    private val vehicle: VehicleDataSource = FakeVehicleData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DriveRecordTheme {
                AppRoot(vehicle)
            }
        }
    }

    // 핸들 버튼(시험용: 키보드 C 키)으로 캡처
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode in WheelKeys.captureKeys && event?.repeatCount == 0) {
            if (WheelKeys.onCapture?.invoke() == true) return true
        }
        return super.onKeyDown(keyCode, event)
    }
}