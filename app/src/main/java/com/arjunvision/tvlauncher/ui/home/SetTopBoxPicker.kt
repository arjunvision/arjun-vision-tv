package com.arjunvision.tvlauncher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.HdmiInput
import com.arjunvision.tvlauncher.domain.models.InputConnection
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

/**
 * Asked once, only if the Airtel box's HDMI port could not be detected. The choice is saved and
 * every later OK on AIRTEL DTH switches straight to it.
 */
@Composable
fun SetTopBoxPicker(choices: List<HdmiInput>, onPick: (HdmiInput) -> Unit, onDismiss: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(LauncherColors.Surface).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Which HDMI is the Airtel box on?", color = LauncherColors.TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Asked only once. AIRTEL DTH will open this input directly from now on.", color = LauncherColors.TextSecondary, fontSize = 16.sp)
            Spacer(Modifier.height(24.dp))
            if (choices.isEmpty()) {
                Text("The TV reports no free HDMI inputs. Check the cable, then try again.", color = Color(0xFFF59E0B), fontSize = 16.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                choices.forEachIndexed { i, input ->
                    FocusCard(
                        onClick = { onPick(input) },
                        background = Brush.linearGradient(listOf(Color(0xFFE11D48).copy(alpha = 0.3f), LauncherColors.Surface)),
                        modifier = Modifier.width(200.dp).height(110.dp).then(if (i == 0) Modifier.focusRequester(first) else Modifier),
                    ) {
                        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center) {
                            Text(input.label, color = LauncherColors.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = input.connection.label.ifBlank { "State unknown" },
                                color = if (input.connection == InputConnection.CONNECTED) Color(0xFF22C55E) else LauncherColors.TextSecondary,
                                fontSize = 14.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
