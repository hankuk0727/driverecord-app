package app.driverecord

import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper as PleosNaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

// 차량(Pleos)에서는 진짜 값을, 폰이나 값을 못 받을 때는 가짜 값(FakeVehicleData)을 쓰는 데이터 공급자.
//  - 주행 모드: Vehicle SDK DrivingMode (일반/에코/스포츠)
//  - 현재 위치: NaviHelper getCurrentLocationInfo (5초마다 갱신)
//  - 속도: 아직 가짜 값 (다음 단계에서 연결)
// 시연 모드가 켜져 있으면 항상 가짜 값의 정해진 순서를 쓴다.
class PleosVehicleData(context: Context) : VehicleDataSource {

    private val app = context.applicationContext
    private val fake = FakeVehicleData()
    private val onCar = app.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var mode: String? = null
    @Volatile private var place: Place? = null
    @Volatile private var naviReady = false

    private var vehicle: Vehicle? = null
    private var navi: PleosNaviHelper? = null
    private var running = false
    private var demo = false

    private val modeListener = object : DrivingModeStateListener {
        override fun onDrivingModeStateUpdated(state: DrivingModeType?) {
            mode = modeLabel(state)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "driving mode listener failed", e)
        }
    }

    private val naviListener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            val lat = currentLocationInfo.latitude
            val lng = currentLocationInfo.longitude
            if (lat == 0.0 && lng == 0.0) return
            place = Place(placeName(currentLocationInfo.address), lat, lng)
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (!running) return
            requestLocation()
            handler.postDelayed(this, POLL_MS)
        }
    }

    // 화면이 보이는 동안 시작한다. 폰에서는 아무것도 하지 않는다.
    fun start() {
        if (!onCar || running) return
        running = true
        connectVehicle()
        connectNavi()
        handler.post(poll)
    }

    // 화면이 사라지면 위치 갱신만 멈춘다.
    fun pause() {
        running = false
        handler.removeCallbacks(poll)
    }

    // 앱이 끝날 때 연결을 모두 정리한다.
    fun release() {
        pause()
        val v = vehicle
        vehicle = null
        runCatching { v?.getDrivingMode()?.unregisterDrivingModeState(modeListener) }
        runCatching { v?.release() }
        val n = navi
        navi = null
        naviReady = false
        runCatching { n?.removeListener(naviListener) }
        runCatching { n?.release() }
    }

    private fun connectVehicle() {
        if (vehicle != null) return
        try {
            val v = Vehicle(app)
            v.initialize()
            vehicle = v
            val dm = v.getDrivingMode()
            dm.registerDrivingModeState(modeListener)
            dm.getCurrentDrivingModeState(
                onComplete = { state -> mode = modeLabel(state) },
                onFailed = { e -> Log.w(TAG, "driving mode read failed", e) }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Vehicle SDK connect failed", t)
        }
    }

    private fun connectNavi() {
        if (navi != null) return
        try {
            val helper = PleosNaviHelper(app)
            navi = helper
            helper.initialize(
                onConnected = {
                    if (navi === helper) {
                        helper.addListener(naviListener)
                        naviReady = true
                        requestLocation()
                    }
                },
                onFailed = { t -> Log.w(TAG, "NaviHelper connect failed", t) }
            )
        } catch (t: Throwable) {
            navi = null
            Log.w(TAG, "NaviHelper create failed", t)
        }
    }

    private fun requestLocation() {
        if (!naviReady) return
        try {
            navi?.getCurrentLocationInfo()
        } catch (t: Throwable) {
            Log.w(TAG, "location request failed", t)
        }
    }

    private fun modeLabel(state: DrivingModeType?): String? = when (state) {
        DrivingModeType.NORMAL -> "일반"
        DrivingModeType.ECO -> "에코"
        DrivingModeType.SPORT -> "스포츠"
        else -> null
    }

    // "대구광역시 북구 대학로 80" 처럼 긴 주소는 앞의 세 마디까지만 쓴다.
    private fun placeName(address: String): String {
        val words = address.trim().split(" ").filter { it.isNotEmpty() }
        return if (words.isEmpty()) "현재 위치" else words.take(3).joinToString(" ")
    }

    private fun useReal(): Boolean = onCar && !demo

    override fun currentDriveMode(): String =
        if (useReal()) mode ?: fake.currentDriveMode() else fake.currentDriveMode()

    override fun currentPlace(): Place {
        if (!useReal()) return fake.currentPlace()
        requestLocation()
        return place ?: fake.currentPlace()
    }

    // 속도는 다음 단계에서 차량 값으로 바꾼다.
    override fun currentSpeedKmh(): Int = fake.currentSpeedKmh()

    override fun setDemoMode(on: Boolean) {
        demo = on
        fake.setDemoMode(on)
    }

    override fun isDemoMode(): Boolean = demo

    private companion object {
        const val TAG = "PleosVehicleData"
        const val POLL_MS = 5000L
    }
}