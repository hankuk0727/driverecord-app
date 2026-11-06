package app.driverecord

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.driverecord.ui.theme.DrError
import app.driverecord.ui.theme.DrTextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val PREFS_NAME = "share_prefs"
private const val KEY_CONSENT = "share_consent"

// "폰으로 보내기" 버튼. 처음 누르면 안내·동의 창, 동의하면 사진(스티커 포함)을 올리고 QR 코드 창을 띄운다.
@Composable
fun ShareButton(moment: DriveMoment, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var qr by remember { mutableStateOf<Bitmap?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showConsent by remember { mutableStateOf(false) }

    // 실제로 사진을 올리고 QR을 만든다 (동의한 뒤에만 호출)
    fun startUpload() {
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DrPrimaryButton(
            text = if (busy) "올리는 중…" else "폰으로 보내기",
            enabled = !busy && moment.photoPath.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (prefs.getBoolean(KEY_CONSENT, false)) {
                    startUpload()
                } else {
                    showConsent = true
                }
            }
        )

        errorText?.let {
            Text(
                text = it,
                color = DrError,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 안내·동의 창
    if (showConsent) {
        AlertDialog(
            onDismissRequest = { showConsent = false },
            title = { Text("폰으로 보내기 전에 확인해 주세요") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "이 장면의 사진을 인터넷 서버에 올려서 폰에서 볼 수 있게 만들어요.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• 올라가는 것: 사진 한 장. 차량 정보 스티커를 켜 두었다면 시각, 장소(위치), 속도, 주행 모드, 메모가 사진에 함께 들어가요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                    Text(
                        text = "• 올라가는 곳: 이 앱의 서버(Google Firebase).",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                    Text(
                        text = "• 볼 수 있는 기간: 24시간. 그 뒤에는 볼 수 없어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                    Text(
                        text = "• 볼 수 있는 사람: 화면에 뜨는 QR 코드(주소)를 아는 사람이에요. 다른 사람에게 보여 주지 않도록 주의해 주세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                    Text(
                        text = "동의하지 않으면 사진은 올라가지 않고, 앱의 다른 기능은 그대로 쓸 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                }
            },
            confirmButton = {
                DrPrimaryButton(
                    text = "동의하고 보내기",
                    onClick = {
                        prefs.edit().putBoolean(KEY_CONSENT, true).apply()
                        showConsent = false
                        startUpload()
                    }
                )
            },
            dismissButton = {
                DrButton("취소", onClick = { showConsent = false })
            }
        )
    }

    qr?.let { code ->
        AlertDialog(
            onDismissRequest = { qr = null },
            title = { Text("휴대폰 카메라로 찍어보세요") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        bitmap = code.asImageBitmap(),
                        contentDescription = "QR 코드",
                        modifier = Modifier.size(260.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Text(
                        text = "24시간 동안 사진을 볼 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DrTextSecondary
                    )
                }
            },
            confirmButton = {
                DrButton("닫기", onClick = { qr = null })
            }
        )
    }
}