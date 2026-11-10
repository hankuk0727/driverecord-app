package app.driverecord

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import app.driverecord.ui.theme.DriveRecordTheme

class MainActivity : ComponentActivity() {
    // 차량에서는 진짜 값, 폰에서는 가짜 값 (Context는 onCreate 이후에 쓸 수 있어서 lazy)
    private val pleosVehicle by lazy { PleosVehicleData(this) }
    private val vehicle: VehicleDataSource by lazy { pleosVehicle }

    // 주행/정차 신호 (권한을 "정차 중"에만 묻기 위해 사용)
    private val drivingMonitor by lazy { DrivingMonitor(this) }

    private var speedAsked = false

    override fun onStart() {
        super.onStart()
        pleosVehicle.start()
        drivingMonitor.start()
        // 권한 묻기는 아래 setContent 안에서, 정차 중일 때만 한다.
    }

    override fun onStop() {
        super.onStop()
        pleosVehicle.pause()
        drivingMonitor.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        pleosVehicle.release()
    }

    // 차량 속도 권한(CAR_SPEED)은 실행 중에 허락받는 종류라서, 차량에서 한 번만 묻는다.
    // 주행 중에 허락 창을 띄우면 차량이 "주행 중에는 사용할 수 없습니다"로 앱을 막을 수 있어서
    // 반드시 정차 중일 때만 부른다.
    private fun askSpeedPermission() {
        if (!pleosVehicle.isOnCar() || speedAsked) return
        val permission = "android.car.permission.CAR_SPEED"
        if (checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED) return
        speedAsked = true
        requestPermissions(arrayOf(permission), 1001)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 &&
            grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pleosVehicle.retrySpeed()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 차량 신호가 들어오고 + 정차 중일 때만 권한을 묻는다.
            // 주행 중에 앱을 처음 켜면 기다렸다가, P로 바뀌는 순간 묻는다.
            val available = drivingMonitor.available
            val driving = drivingMonitor.driving
            LaunchedEffect(available, driving) {
                if (available && !driving) askSpeedPermission()
            }
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