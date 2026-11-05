package app.driverecord

import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper as PleosNaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.constant.control.DrivingModeType
import ai.pleos.playground.vehicle.listener.DrivingModeStateListener
import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log

// 차량(Pleos)에서는 진짜 값을, 폰이나 값을 못 받을 때는 가짜 값(FakeVehicleData)을 쓰는 데이터 공급자.
//  - 주행 모드: Vehicle SDK DrivingMode (일반/에코/스포츠)
//  - 현재 위치: NaviHelper getCurrentLocationInfo (5초마다 갱신)
//  - 속도: 안드로이드 표준 CarPropertyManager PERF_VEHICLE_SPEED (m/s를 km/h로 환산)
// 시연 모드가 켜져 있으면 항상 가짜 값의 정해진 순서를 쓴다.
class PleosVehicleData(context: Context) : VehicleDataSource {

    private val app = context.applicationContext
    private val fake = FakeVehicleData()
    private val onCar = app.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile private var mode: String? = null
    @Volatile private var place: Place? = null
    @Volatile private var speedKmh: Int? = null
    @Volatile private var naviReady = false

    private var vehicle: Vehicle? = null
    private var navi: PleosNaviHelper? = null
    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null
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

    private val speedCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            val ms = value.value as? Float ?: return
            speedKmh = Math.round(ms * 3.6f)
        }

        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w(TAG, "speed error property=$propertyId zone=$zone")
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
        connectSpeed()
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
        runCatching { propertyManager?.unregisterCallback(speedCallback) }
        propertyManager = null
        runCatching { car?.disconnect() }
        car = null
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

    // 속도: 못 받으면(권한 없음 등) speedKmh가 null로 남아 가짜 값이 쓰인다.
    // 계기판과 같은 값(PERF_VEHICLE_SPEED)을 먼저, 안 되면 표시용 속도(PERF_VEHICLE_SPEED_DISPLAY)를 시도한다.
    private fun connectSpeed() {
        if (propertyManager != null) return
        try {
            val granted = app.checkSelfPermission("android.car.permission.CAR_SPEED") ==
                    PackageManager.PERMISSION_GRANTED
            Log.d(TAG, "CAR_SPEED granted=$granted")
            val c = Car.createCar(app)
            car = c
            val manager = c?.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
            if (manager == null) {
                Log.w(TAG, "CarPropertyManager not available")
                return
            }
            propertyManager = manager
            Log.d(TAG, "car property count=${manager.propertyList.size}")
            val ids = listOf(
                VehiclePropertyIds.PERF_VEHICLE_SPEED,
                VehiclePropertyIds.PERF_VEHICLE_SPEED_DISPLAY
            )
            for (id in ids) {
                val available = manager.isPropertyAvailable(id, 0)
                val registered = manager.registerCallback(
                    speedCallback,
                    id,
                    CarPropertyManager.SENSOR_RATE_NORMAL
                )
                Log.d(TAG, "speed prop=$id available=$available registered=$registered")
                if (registered) break
            }
        } catch (t: Throwable) {
            Log.w(TAG, "speed connect failed", t)
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

    override fun currentSpeedKmh(): Int =
        if (useReal()) speedKmh ?: fake.currentSpeedKmh() else fake.currentSpeedKmh()

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