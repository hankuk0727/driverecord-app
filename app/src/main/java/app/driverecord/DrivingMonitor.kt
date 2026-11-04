package app.driverecord

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// 차량(Pleos)의 주행 규제 신호를 받아서 "지금 주행 중인가"를 알려 준다.
// 차량이 아닌 기기(휴대폰 등)에서는 available = false 로 남고, 그때는 화면의 시험용 버튼을 쓴다.
class DrivingMonitor(context: Context) {

    private val appContext = context.applicationContext

    // 차량 신호를 실제로 받고 있으면 true
    var available by mutableStateOf(false)
        private set

    // 주행 규제가 켜져 있으면 true (기어 D 등). available 이 true 일 때만 의미가 있다.
    var driving by mutableStateOf(false)
        private set

    private var car: Car? = null
    private var manager: CarUxRestrictionsManager? = null

    fun start() {
        if (car != null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            !appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
        ) {
            return
        }
        try {
            val newCar = Car.createCar(appContext) ?: return
            @Suppress("DEPRECATION")
            val newManager = newCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
            if (newManager == null) {
                newCar.disconnect()
                return
            }
            car = newCar
            manager = newManager
            newManager.registerListener { restrictions -> apply(restrictions) }
            newManager.currentCarUxRestrictions?.let { apply(it) }
            available = true
        } catch (t: Throwable) {
            // android.car 가 없는 기기에서는 여기로 온다. 조용히 시험용 버튼 방식으로 돌아간다.
            Log.w(TAG, "차량 주행 신호를 받을 수 없음", t)
            available = false
        }
    }

    fun stop() {
        runCatching { manager?.unregisterListener() }
        runCatching { car?.disconnect() }
        manager = null
        car = null
        available = false
    }

    private fun apply(restrictions: CarUxRestrictions) {
        driving = restrictions.isRequiresDistractionOptimization
    }

    private companion object {
        const val TAG = "DrivingMonitor"
    }
}