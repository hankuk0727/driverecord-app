package app.driverecord

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.driverecord.ui.theme.AstaSans
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrDisabledContent
import app.driverecord.ui.theme.DrError
import app.driverecord.ui.theme.DrMode
import app.driverecord.ui.theme.DrOnAmber
import app.driverecord.ui.theme.DrOutline
import app.driverecord.ui.theme.DrSurface
import app.driverecord.ui.theme.DrSurfaceHigh
import app.driverecord.ui.theme.DrSurfaceRaised
import app.driverecord.ui.theme.DrSwitchOn
import app.driverecord.ui.theme.DrTextPrimary

// 앱 전체에서 같이 쓰는 버튼·카드·스위치 부품. 크기나 색을 바꿀 때는 여기만 고치면 된다.
//
// Pleos 공식 디자인 파일의 모양을 따른다.
// - 버튼/카드에는 테두리선이 없고, 바탕보다 살짝 밝은 면으로만 구분한다. (공식: 반투명 흰색 5~10%)
// - 모서리: 큰 버튼 24, 작은 버튼 16, 카드 24, 캡처 카드 32 (공식 비율을 폰 크기에 맞춰 줄임)
// - 밝게 채우는 버튼(DrPrimaryButton)은 한 화면에 하나만 쓴다.

private val LargeShape = RoundedCornerShape(24.dp)
private val SmallShape = RoundedCornerShape(16.dp)
private val ButtonPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)

// 공통 버튼 모양. 글자는 필요하면 2줄까지 늘어나고 말줄임하지 않는다.
@Composable
private fun DrButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    minHeight: Dp,
    shape: RoundedCornerShape,
    container: Color,
    content: Color,
    textStyle: TextStyle
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = DrSurface,
            disabledContentColor = DrDisabledContent
        ),
        modifier = modifier.heightIn(min = minHeight).widthIn(min = 112.dp)
    ) {
        Text(text, style = textStyle, textAlign = TextAlign.Center)
    }
}

// 주요 버튼: 밝게 채움(공식 button/filled), 최소 높이 72dp (화면의 가장 중요한 행동 하나에만)
@Composable
fun DrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 72.dp, LargeShape,
    DrAmber, DrOnAmber, MaterialTheme.typography.labelLarge
)

// 큰 일반 버튼: 모드 전환처럼 크게 눌러야 하지만 강조는 낮은 행동, 최소 높이 72dp
@Composable
fun DrLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 72.dp, LargeShape,
    DrSurfaceRaised, DrTextPrimary, MaterialTheme.typography.labelLarge
)

// 일반 버튼: 최소 높이 56dp
@Composable
fun DrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 56.dp, SmallShape,
    DrSurfaceRaised, DrTextPrimary, MaterialTheme.typography.labelLarge
)

// 삭제 버튼: 일반 버튼 모양 + 오류색 글자
@Composable
fun DrDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 56.dp, SmallShape,
    DrSurfaceRaised, DrError, MaterialTheme.typography.labelLarge
)

// 캡처 버튼: 큰 카드 + 셔터 모양 링 + 글자 32sp. 최소 높이 160dp, 모서리 32dp, 테두리선 없음
@Composable
fun DrCaptureButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DrSurface, contentColor = DrTextPrimary),
        modifier = modifier.heightIn(min = 160.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 셔터: 바깥 링 + 안쪽 채운 원
            Canvas(Modifier.size(72.dp)) {
                val ring = 5.dp.toPx()
                val r = size.minDimension / 2f
                drawCircle(DrAmber, radius = r - ring / 2f, style = Stroke(width = ring))
                drawCircle(DrAmber, radius = r - ring * 2.4f)
            }
            Text(
                text,
                style = TextStyle(
                    fontFamily = AstaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 40.sp
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 카드: 모서리 24dp(공식 Radius/l), 안쪽 여백 20dp, 안쪽 항목 간격 12dp, 그림자 없음
@Composable
fun DrCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DrSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

// 스위치 줄: 글자까지 포함해서 줄 전체가 하나의 터치 영역 (최소 높이 64dp)
// 공식 스위치: 켜짐은 초록(switch/on), 꺼짐은 중립색
@Composable
fun DrSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val offThumb = if (DrMode.day) Color.White else Color(0xFFA4A8AE)   // 공식 switch/knob_off
    val offTrack = if (DrMode.day) DrOutline else DrSurfaceHigh
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,   // 줄 전체가 토글하므로 여기선 null
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DrSwitchOn,
                checkedBorderColor = DrSwitchOn,
                uncheckedThumbColor = offThumb,
                uncheckedTrackColor = offTrack,
                uncheckedBorderColor = offTrack
            )
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DrTextPrimary)
    }
}