package app.driverecord

data class DriveMoment(
    val id: Int,
    val timeText: String,
    val driveMode: String,
    val placeName: String
)

interface VehicleDataSource {
    fun currentDriveMode(): String
    fun currentPlaceName(): String
}

class FakeVehicleData : VehicleDataSource {
    override fun currentDriveMode(): String = "컴포트"
    override fun currentPlaceName(): String = "시범 장소"
}