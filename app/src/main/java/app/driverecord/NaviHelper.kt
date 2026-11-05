package app.driverecord

import ai.pleos.playground.navi.constants.RouteOption
import ai.pleos.playground.navi.data.NavigationScreenRequest
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper as PleosNaviHelper
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

// "다시 가기": 저장한 장면의 장소로 길 안내를 켠다.
// 차량(Pleos)이면 차량 내비(NaviHelper SDK)에 경로를 요청하고, 아니면 폰의 지도 앱을 연다.
// (이 파일의 NaviHelper 는 우리 앱의 이름이고, Pleos SDK 의 같은 이름 클래스는 PleosNaviHelper 로 부른다.)
object NaviHelper {

    private const val TAG = "NaviHelper"

    @Volatile private var pleos: PleosNaviHelper? = null
    @Volatile private var connected = false
    @Volatile private var pending: ((PleosNaviHelper) -> Unit)? = null

    // 위치가 저장된 장면인지 (0.0, 0.0 이면 위치를 모르는 장면)
    fun hasLocation(m: DriveMoment): Boolean =
        m.latitude != 0.0 || m.longitude != 0.0

    // 열기에 성공(또는 요청 접수)하면 true, 열 수 없으면 false
    fun open(context: Context, m: DriveMoment): Boolean {
        if (!hasLocation(m)) return false
        val appContext = context.applicationContext
        if (appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            if (openInPleos(appContext, m)) return true
        }
        return openInMapApp(context, m)
    }

    // ---- 차량: Pleos 내비에 경로 요청 ----

    private fun openInPleos(context: Context, m: DriveMoment): Boolean {
        val request: (PleosNaviHelper) -> Unit = { helper -> startGuidance(helper, m) }
        val ready = pleos
        if (ready != null && connected) {
            request(ready)
            return true
        }
        // 아직 연결 전이면 연결된 뒤에 요청한다
        pending = request
        if (ready == null) connect(context)
        return pleos != null
    }

    private fun connect(context: Context) {
        val helper = try {
            PleosNaviHelper(context)
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos 내비 연결 준비 실패", t)
            return
        }
        pleos = helper
        try {
            helper.initialize(
                onConnected = {
                    if (pleos === helper) {
                        connected = true
                        pending?.invoke(helper)
                        pending = null
                    }
                },
                onFailed = { throwable ->
                    Log.w(TAG, "Pleos 내비 연결 실패", throwable)
                    if (pleos === helper) {
                        connected = false
                        pleos = null
                        pending = null
                    }
                }
            )
        } catch (t: Throwable) {
            Log.w(TAG, "Pleos 내비 initialize 예외", t)
            pleos = null
            pending = null
        }
    }

    private fun startGuidance(helper: PleosNaviHelper, m: DriveMoment) {
        try {
            helper.requestRoute(
                RouteInfo(
                    latitude = m.latitude,
                    longitude = m.longitude,
                    displayLatitude = null,
                    displayLongitude = null,
                    poiName = m.placeName,
                    poiId = "",          // 좌표만으로 요청되는지는 에뮬레이터에서 확인 필요
                    poiSubId = "0",
                    address = "",
                    routeOption = RouteOption.RECOMMENDED
                )
            )
        } catch (t: Throwable) {
            Log.w(TAG, "경로 요청 실패", t)
            return
        }
        try {
            helper.requestNavigationScreen(NavigationScreenRequest.Foreground)
        } catch (t: Throwable) {
            Log.w(TAG, "내비 화면 요청 실패", t)
        }
    }

    // ---- 휴대폰: 지도 앱 열기 ----

    private fun openInMapApp(context: Context, m: DriveMoment): Boolean {
        val label = Uri.encode(m.placeName)
        val uri = Uri.parse("geo:${m.latitude},${m.longitude}?q=${m.latitude},${m.longitude}($label)")
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}