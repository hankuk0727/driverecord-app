package app.driverecord

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrAmberContainer
import app.driverecord.ui.theme.DrBackground
import app.driverecord.ui.theme.DrSurface
import app.driverecord.ui.theme.DrTextSecondary

// 차량용 큰 메뉴 버튼 (Pleos 큰 버튼 기준으로 높이 크게)
@Composable
private fun CarRailButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String
) {
    val tint = if (selected) DrAmber else DrTextSecondary
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(128.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(if (selected) DrAmberContainer else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompositionLocalProvider(LocalContentColor provides tint) { icon() }
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                color = tint,
                fontSize = 24.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun AppRoot(vehicle: VehicleDataSource) {
    var tab by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val onCar = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }
    val iconDp: Dp = if (onCar) 48.dp else 28.dp
    val labelSize: TextUnit = if (onCar) 24.sp else TextUnit.Unspecified

    // 선택은 포인트색 아이콘·글자 + 어두운 표시, 비선택은 보조 글자색
    val barColors = NavigationBarItemDefaults.colors(
        selectedIconColor = DrAmber,
        selectedTextColor = DrAmber,
        indicatorColor = DrAmberContainer,
        unselectedIconColor = DrTextSecondary,
        unselectedTextColor = DrTextSecondary
    )
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = DrAmber,
        selectedTextColor = DrAmber,
        indicatorColor = DrAmberContainer,
        unselectedIconColor = DrTextSecondary,
        unselectedTextColor = DrTextSecondary
    )

    // 탭 아이콘: 선택/비선택에 따라 정해진 색(LocalContentColor)을 그대로 씀
    val captureIcon: @Composable () -> Unit = {
        DrIcon(DrIconKind.Camera, LocalContentColor.current, iconSize = iconDp)
    }
    val fixIcon: @Composable () -> Unit = {
        DrIcon(DrIconKind.Tune, LocalContentColor.current, iconSize = iconDp)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(DrBackground)) {
        // 가로로 넓은 화면(차량 디스플레이 등)이면 탭을 왼쪽 세로 막대로
        val sideRail = maxWidth > maxHeight && maxWidth >= 560.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DrBackground,
            bottomBar = {
                if (!sideRail) {
                    NavigationBar(
                        containerColor = DrSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.heightIn(min = if (onCar) 120.dp else 80.dp)
                    ) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = captureIcon,
                            label = {
                                Text("캡처", style = MaterialTheme.typography.labelMedium, fontSize = labelSize)
                            },
                            colors = barColors
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = fixIcon,
                            label = {
                                Text("사진 보정", style = MaterialTheme.typography.labelMedium, fontSize = labelSize)
                            },
                            colors = barColors
                        )
                    }
                }
            }
        ) { inner ->
            Row(Modifier.fillMaxSize().padding(inner)) {
                if (sideRail) {
                    if (onCar) {
                        // 차량: 넓고 큰 버튼 두 개
                        Column(
                            Modifier.width(172.dp).fillMaxHeight().background(DrSurface),
                            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CarRailButton(tab == 0, { tab = 0 }, captureIcon, "캡처")
                            CarRailButton(tab == 1, { tab = 1 }, fixIcon, "사진 보정")
                        }
                    } else {
                        NavigationRail(
                            containerColor = DrSurface,
                            windowInsets = WindowInsets()
                        ) {
                            Spacer(Modifier.weight(1f))
                            NavigationRailItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = captureIcon,
                                label = { Text("캡처", style = MaterialTheme.typography.labelMedium) },
                                colors = railColors
                            )
                            NavigationRailItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = fixIcon,
                                label = { Text("사진 보정", style = MaterialTheme.typography.labelMedium) },
                                colors = railColors
                            )
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                // 두 화면을 모두 켜 둬서 탭을 바꿔도 상태가 유지됨
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(if (tab == 0) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                        CaptureTab(vehicle, Modifier)
                    }
                    Box(if (tab == 1) Modifier.fillMaxSize() else Modifier.size(0.dp)) {
                        FixScreen(Modifier)
                    }
                }
            }
        }
    }
}