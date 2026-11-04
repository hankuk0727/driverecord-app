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
    val longitude: Double = 0.0
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

    override fun currentDriveMode(): String = "컴포트"
    override fun currentPlace(): Place = places.random()
    override fun currentSpeedKmh(): Int = (40..80).random()
}