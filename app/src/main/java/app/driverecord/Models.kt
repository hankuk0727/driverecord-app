package app.driverecord

data class DriveMoment(
    val id: Int,
    val timeText: String,
    val driveMode: String,
    val placeName: String,
    val memo: String = "",
    val photoPath: String = "",
    val speedKmh: Int = 0,
    val sticker: Boolean = false,
    val latitude: Double = 0.0,    // 0.0, 0.0 이면 위치를 모르는 장면
    val longitude: Double = 0.0,
    val createdAt: Long = 0L       // 저장한 시각(밀리초). 0이면 예전 장면이라 날짜를 모름
)

// 장소 이름과 좌표를 한 묶음으로 다룬다 (이름과 좌표가 어긋나지 않게)
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

interface VehicleDataSource {
    fun currentDriveMode(): String
    fun currentPlace(): Place
    fun currentSpeedKmh(): Int

    // 예전 코드와 맞추기 위한 보조 함수
    fun currentPlaceName(): String = currentPlace().name

    // 시연 모드: 장소와 속도가 정해진 순서로 나온다. (진짜 차량 데이터에서는 쓰지 않는다)
    fun setDemoMode(on: Boolean) {}
    fun isDemoMode(): Boolean = false
}

// 진짜 차량 신호를 받기 전까지 쓰는 가짜 데이터
class FakeVehicleData : VehicleDataSource {
    // 시연용 장소 (좌표는 대략적인 값)
    private val places = listOf(
        Place("경북대학교", 35.8888, 128.6105),
        Place("수성못", 35.8283, 128.6165),
        Place("동성로", 35.8687, 128.5958),
        Place("이월드 83타워", 35.8530, 128.5660)
    )

    // 시연 모드에서 차례로 나오는 순서 (places의 번호). 세 번째, 다섯 번째에 회상 카드가 뜬다.
    private val demoPlaceOrder = listOf(0, 1, 0, 2, 1)
    private val demoSpeeds = listOf(48, 62, 55, 70, 41)

    private var demo = false
    private var placeStep = 0
    private var speedStep = 0

    override fun setDemoMode(on: Boolean) {
        demo = on
        placeStep = 0   // 켜거나 끌 때마다 처음 순서부터 다시 시작
        speedStep = 0
    }

    override fun isDemoMode(): Boolean = demo

    override fun currentDriveMode(): String = "컴포트"

    override fun currentPlace(): Place {
        if (!demo) return places.random()
        val place = places[demoPlaceOrder[placeStep % demoPlaceOrder.size]]
        placeStep += 1
        return place
    }

    override fun currentSpeedKmh(): Int {
        if (!demo) return (40..80).random()
        val speed = demoSpeeds[speedStep % demoSpeeds.size]
        speedStep += 1
        return speed
    }
}