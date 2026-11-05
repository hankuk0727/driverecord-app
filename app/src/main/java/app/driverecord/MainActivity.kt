package app.driverecord

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.driverecord.ui.theme.DriveRecordTheme

class MainActivity : ComponentActivity() {
    // 차량에서는 진짜 값, 폰에서는 가짜 값 (Context는 onCreate 이후에 쓸 수 있어서 lazy)
    private val pleosVehicle by lazy { PleosVehicleData(this) }
    private val vehicle: VehicleDataSource by lazy { pleosVehicle }

    override fun onStart() {
        super.onStart()
        pleosVehicle.start()
    }

    override fun onStop() {
        super.onStop()
        pleosVehicle.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        pleosVehicle.release()
    }

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