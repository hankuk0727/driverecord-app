package app.driverecord

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppRoot(vehicle: VehicleDataSource) {
    var tab by remember { mutableStateOf(0) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("●") },
                    label = { Text("캡처") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("✦") },
                    label = { Text("사진 보정") }
                )
            }
        }
    ) { inner ->
        Box(if (tab == 0) Modifier.fillMaxSize().padding(inner) else Modifier.size(0.dp)) {
            CaptureTab(vehicle, Modifier)
        }
        Box(if (tab == 1) Modifier.fillMaxSize().padding(inner) else Modifier.size(0.dp)) {
            FixScreen(Modifier)
        }
    }
}