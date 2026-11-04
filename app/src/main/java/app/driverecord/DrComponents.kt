package app.driverecord

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.driverecord.ui.theme.DrAmber
import app.driverecord.ui.theme.DrDisabledContent
import app.driverecord.ui.theme.DrError
import app.driverecord.ui.theme.DrOnAmber
import app.driverecord.ui.theme.DrOutline
import app.driverecord.ui.theme.DrOutlineVariant
import app.driverecord.ui.theme.DrSurface
import app.driverecord.ui.theme.DrSurfaceHigh
import app.driverecord.ui.theme.DrSurfaceRaised
import app.driverecord.ui.theme.DrTextPrimary
import app.driverecord.ui.theme.DrTextSecondary

// 앱 전체에서 같이 쓰는 버튼·카드·스위치 부품. 크기나 색을 바꿀 때는 여기만 고치면 된다.
//
// 강조 규칙: 밝게 채우는 버튼(DrPrimaryButton, DrCaptureButton)은 한 화면에 하나만 쓴다.
// 나머지(DrButton, DrLargeButton, DrDangerButton)는 어두운 버튼이다.

private val ButtonShape = RoundedCornerShape(12.dp)
private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

// 공통 버튼 모양. 글자는 필요하면 2줄까지 늘어나고 말줄임하지 않는다.
@Composable
private fun DrButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    minHeight: Dp,
    container: Color,
    content: Color,
    textStyle: TextStyle,
    border: BorderStroke? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = ButtonShape,
        contentPadding = ButtonPadding,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = DrSurfaceRaised,
            disabledContentColor = DrDisabledContent
        ),
        modifier = modifier.heightIn(min = minHeight).widthIn(min = 112.dp)
    ) {
        Text(text, style = textStyle, textAlign = TextAlign.Center)
    }
}

// 주요 버튼: 밝게 채움, 최소 높이 72dp (화면의 가장 중요한 행동 하나에만)
@Composable
fun DrPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 72.dp,
    DrAmber, DrOnAmber, MaterialTheme.typography.labelLarge
)

// 큰 어두운 버튼: 모드 전환처럼 크게 눌러야 하지만 강조는 낮은 행동, 최소 높이 72dp
@Composable
fun DrLargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 72.dp,
    DrSurfaceRaised, DrTextPrimary, MaterialTheme.typography.labelLarge,
    BorderStroke(1.dp, DrOutlineVariant)
)

// 일반 버튼: 어두운 회색, 최소 높이 56dp
@Composable
fun DrButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 56.dp,
    DrSurfaceRaised, DrTextPrimary, MaterialTheme.typography.labelLarge,
    BorderStroke(1.dp, DrOutlineVariant)
)

// 삭제 버튼: 일반 버튼 모양 + 오류색 글자
@Composable
fun DrDangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = DrButtonBase(
    text, onClick, modifier, enabled, 56.dp,
    DrSurfaceRaised, DrError, MaterialTheme.typography.labelLarge,
    BorderStroke(1.dp, DrOutlineVariant)
)

// 캡처 버튼: 가로 전체, 최소 높이 160dp, 모서리 24dp, 글자 32sp
@Composable
fun DrCaptureButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DrAmber, contentColor = DrOnAmber),
        modifier = modifier.heightIn(min = 160.dp)
    ) {
        Text(
            text,
            style = TextStyle(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 32.sp,
                lineHeight = 40.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

// 카드: 모서리 16dp, 안쪽 여백 20dp, 안쪽 항목 간격 12dp, 그림자 없음
@Composable
fun DrCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
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
// 켜졌을 때만 포인트색, 꺼졌을 때는 중립색
@Composable
fun DrSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
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
                checkedThumbColor = DrOnAmber,
                checkedTrackColor = DrAmber,
                checkedBorderColor = DrAmber,
                uncheckedThumbColor = DrTextSecondary,
                uncheckedTrackColor = DrSurfaceHigh,
                uncheckedBorderColor = DrOutline
            )
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DrTextPrimary)
    }
}