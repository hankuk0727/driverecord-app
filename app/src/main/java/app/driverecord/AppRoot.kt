package app.driverecord

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrAmberContainer
import app.driverecord.ui.theme.DrBackground
import app.driverecord.ui.theme.DrSurface
import app.driverecord.ui.theme.DrTextSecondary

@Composable
fun AppRoot(vehicle: VehicleDataSource) {
    var tab by remember { mutableStateOf(0) }

    // 선택은 포인트색 글자·아이콘 + 어두운 알약, 비선택은 보조 글자색
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
                        modifier = Modifier.heightIn(min = 80.dp)
                    ) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Text("●", style = MaterialTheme.typography.titleMedium) },
                            label = { Text("캡처", style = MaterialTheme.typography.labelMedium) },
                            colors = barColors
                        )
                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Text("✦", style = MaterialTheme.typography.titleMedium) },
                            label = { Text("사진 보정", style = MaterialTheme.typography.labelMedium) },
                            colors = barColors
                        )
                    }
                }
            }
        ) { inner ->
            Row(Modifier.fillMaxSize().padding(inner)) {
                if (sideRail) {
                    NavigationRail(
                        containerColor = DrSurface,
                        windowInsets = WindowInsets()
                    ) {
                        Spacer(Modifier.weight(1f))
                        NavigationRailItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Text("●", style = MaterialTheme.typography.titleMedium) },
                            label = { Text("캡처", style = MaterialTheme.typography.labelMedium) },
                            colors = railColors
                        )
                        NavigationRailItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Text("✦", style = MaterialTheme.typography.titleMedium) },
                            label = { Text("사진 보정", style = MaterialTheme.typography.labelMedium) },
                            colors = railColors
                        )
                        Spacer(Modifier.weight(1f))
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