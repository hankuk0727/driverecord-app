package app.driverecord

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// "폰으로 보내기" 버튼. 누르면 사진(스티커 포함)을 올리고 QR 코드 창을 띄운다.
@Composable
fun ShareButton(moment: DriveMoment, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        OutlinedButton(
            enabled = !busy && moment.photoPath.isNotEmpty(),
            onClick = {
                busy = true
                errorText = null
                scope.launch {
                    try {
                        val photo = withContext(Dispatchers.IO) {
                            val bmp = BitmapFactory.decodeFile(moment.photoPath)
                                ?: error("사진을 읽을 수 없어요")
                            if (moment.sticker) StickerRender.apply(bmp, moment) else bmp
                        }
                        val id = ShareUploader.upload(photo)
                        qr = withContext(Dispatchers.Default) {
                            QrMaker.make(ShareLink.url(id))
                        }
                    } catch (e: Exception) {
                        errorText = "보내기 실패: " + (e.message ?: "알 수 없는 오류")
                    } finally {
                        busy = false
                    }
                }
            }
        ) {
            Text(if (busy) "올리는 중…" else "폰으로 보내기")
        }

        errorText?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    qr?.let { code ->
        AlertDialog(
            onDismissRequest = { qr = null },
            title = { Text("휴대폰 카메라로 찍어보세요") },
            text = {
                Column {
                    Image(
                        bitmap = code.asImageBitmap(),
                        contentDescription = "QR 코드",
                        modifier = Modifier.size(260.dp)
                    )
                    Text(
                        text = "24시간 동안 사진을 볼 수 있어요.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { qr = null }) { Text("닫기") }
            }
        )
    }
}