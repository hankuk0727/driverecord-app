package app.driverecord

data class DriveMoment(
    val id: Int,
    val timeText: String,
    val driveMode: String,
    val placeName: String,
    val memo: String = "",
    val photoPath: String = "",
    val speedKmh: Int = 0,
    val sticker: Boolean = false
)

interface VehicleDataSource {
    fun currentDriveMode(): String
    fun currentPlaceName(): String
    fun currentSpeedKmh(): Int
}

// 진짜 차량 신호를 받기 전까지 쓰는 가짜 데이터
class FakeVehicleData : VehicleDataSource {
    override fun currentDriveMode(): String = "컴포트"
    override fun currentPlaceName(): String = "시범 장소"
    override fun currentSpeedKmh(): Int = (40..80).random()
}