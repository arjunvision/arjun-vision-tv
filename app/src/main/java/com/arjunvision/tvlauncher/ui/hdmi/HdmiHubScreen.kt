package com.arjunvision.tvlauncher.ui.hdmi

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.HdmiDetection
import com.arjunvision.tvlauncher.domain.models.HdmiInput
import com.arjunvision.tvlauncher.domain.models.InputConnection
import com.arjunvision.tvlauncher.ui.HubDevice
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.CinematicBackdrop
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors
import kotlinx.coroutines.delay

private val AirtelRed = Color(0xFFE11D48)
private val PlayStationBlue = Color(0xFF2563EB)

/**
 * SMART HDMI HUB: the two HDMI devices with their own identity, the TV's real input list
 * (TvInputManager: label, type, state, CEC name, custom name), the switching method in use
 * and the manual fallback. Only documented APIs; no success claims the app can't verify.
 */
@Composable
fun HdmiHubScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val motion = if (state.settings.liteMode) 0f else state.settings.animation.factor
    val first = remember { FocusRequester() }
    val airtelChange = remember { FocusRequester() }
    val ps5Change = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) {
        runCatching { first.requestFocus() }
        delay(80)
        listState.scrollToItem(0) // the first focus must not nudge the cards under the header
    }
    // When the Change-input picker closes, put focus back on the button that opened it.
    val picker by viewModel.hubPicker.collectAsState()
    var lastPicker by remember { mutableStateOf<HubDevice?>(null) }
    LaunchedEffect(picker) {
        val closed = lastPicker
        if (picker == null && closed != null) {
            runCatching { (if (closed == HubDevice.AIRTEL) airtelChange else ps5Change).requestFocus() }
        }
        lastPicker = picker
    }

    val airtel = viewModel.hubInputFor(HubDevice.AIRTEL, state)
    val ps5 = viewModel.hubInputFor(HubDevice.PS5, state)
    val method = (airtel ?: ps5)?.let { viewModel.inputAppFor(it.inputId) }

    Box(Modifier.fillMaxSize()) {
        CinematicBackdrop(wallpaperPath = null, animate = false)
        Column(Modifier.fillMaxSize().padding(top = 28.dp)) {
        // Header stays put; only the content below scrolls (so focus never pushes the title away).
        Column(Modifier.padding(horizontal = 48.dp)) {
            Text("SMART HDMI HUB", color = LauncherColors.Blue, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Text("Airtel DTH and PlayStation 5", color = LauncherColors.TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (method != null) "Switching through the TV's own input app ($method), standard Android TV Input Framework."
                else "The TV did not offer an input app for switching. Use the remote's INPUT button (steps below).",
                color = LauncherColors.TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    DeviceCard(
                        device = HubDevice.AIRTEL,
                        input = airtel,
                        cecName = airtel?.let { HdmiDetection.deviceNameFor(it, state.hdmiInputs) },
                        accent = AirtelRed,
                        openLabel = "Watch",
                        motion = motion,
                        firstFocus = first,
                        changeFocus = airtelChange,
                        onOpen = viewModel::openCable,
                        onTest = { viewModel.testSwitch(HubDevice.AIRTEL) },
                        onChange = { viewModel.changeHubInput(HubDevice.AIRTEL) },
                    )
                    DeviceCard(
                        device = HubDevice.PS5,
                        input = ps5,
                        cecName = ps5?.let { HdmiDetection.deviceNameFor(it, state.hdmiInputs) },
                        accent = PlayStationBlue,
                        openLabel = "Play",
                        motion = motion,
                        firstFocus = null,
                        changeFocus = ps5Change,
                        onOpen = viewModel::openPlayStation,
                        onTest = { viewModel.testSwitch(HubDevice.PS5) },
                        onChange = { viewModel.changeHubInput(HubDevice.PS5) },
                    )
                }
            }
            item {
                Text("Inputs reported by the TV", color = LauncherColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                if (state.hdmiInputs.isEmpty()) {
                    Text("The TV reports no HDMI inputs right now.", color = Color(0xFFF59E0B), fontSize = 15.sp)
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
                    items(state.hdmiInputs, key = { it.inputId }) { input ->
                        InputCard(
                            input = input,
                            usedBy = when (input.inputId) {
                                airtel?.inputId -> "AIRTEL DTH"
                                ps5?.inputId -> "PLAYSTATION 5"
                                else -> null
                            },
                            cecName = HdmiDetection.deviceNameFor(input, state.hdmiInputs),
                            motion = motion,
                            onClick = { viewModel.refreshDevices() },
                        )
                    }
                }
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(LauncherColors.Surface.copy(alpha = 0.8f))
                        .padding(20.dp),
                ) {
                    Text("If switching ever fails (manual steps)", color = LauncherColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "1. Press the INPUT (source) button on the original Xiaomi remote.\n" +
                            "2. Choose ${airtel?.label ?: "the Airtel HDMI port"} for AIRTEL DTH, or ${ps5?.label ?: "PlayStation 5"} for the PS5.\n" +
                            "3. Press Home to come back, then open ARJUN VISION TV from the Apps row.",
                        color = LauncherColors.TextSecondary,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun DeviceCard(
    device: HubDevice,
    input: HdmiInput?,
    cecName: String?,
    accent: Color,
    openLabel: String,
    motion: Float,
    firstFocus: FocusRequester?,
    changeFocus: FocusRequester,
    onOpen: () -> Unit,
    onTest: () -> Unit,
    onChange: () -> Unit,
) {
    Column(
        Modifier
            .width(420.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.40f), LauncherColors.Surface.copy(alpha = 0.92f))))
            .padding(22.dp),
    ) {
        Text(device.title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(device.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateDot(input?.connection)
            Spacer(Modifier.width(8.dp))
            Text(
                text = input?.let { listOfNotNull(it.label, it.connection.label.ifBlank { null }).joinToString(" · ") } ?: "No input found",
                color = LauncherColors.TextPrimary,
                fontSize = 16.sp,
            )
        }
        Text(
            text = when {
                input == null -> "Use Change input to pick it once."
                cecName != null && input.parentId != null -> "HDMI-CEC: $cecName · can wake the console"
                cecName != null -> "HDMI-CEC device: $cecName"
                else -> "No HDMI-CEC name (normal for set-top boxes)"
            },
            color = LauncherColors.TextSecondary,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HubButton(openLabel, accent, motion, firstFocus?.let { Modifier.focusRequester(it) } ?: Modifier, onOpen)
            HubButton("Test switch", LauncherColors.Surface, motion, Modifier, onTest)
            HubButton("Change input", LauncherColors.Surface, motion, Modifier.focusRequester(changeFocus), onChange)
        }
    }
}

@Composable
private fun HubButton(text: String, color: Color, motion: Float, modifier: Modifier, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(50),
        background = Brush.linearGradient(listOf(color.copy(alpha = 0.85f), color.copy(alpha = 0.6f))),
        modifier = modifier.height(46.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}

@Composable
private fun InputCard(input: HdmiInput, usedBy: String?, cecName: String?, motion: Float, onClick: () -> Unit) {
    FocusCard(
        onClick = onClick,
        motion = motion,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.width(230.dp).height(132.dp),
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StateDot(input.connection)
                Spacer(Modifier.width(8.dp))
                Text(input.customLabel ?: input.label, color = LauncherColors.TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column {
                Text(
                    listOfNotNull(input.typeLabel, input.connection.label.ifBlank { null }, if (input.parentId != null) "CEC device" else null).joinToString(" · "),
                    color = LauncherColors.TextSecondary,
                    fontSize = 13.sp,
                )
                if (cecName != null && input.parentId == null) Text("CEC: $cecName", color = LauncherColors.TextSecondary, fontSize = 13.sp)
                if (input.customLabel != null) Text("TV name: ${input.label}", color = LauncherColors.TextSecondary, fontSize = 12.sp)
                Text(usedBy?.let { "Used by $it" } ?: "Not assigned", color = if (usedBy != null) LauncherColors.Blue else LauncherColors.TextSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun StateDot(connection: InputConnection?) {
    val color = when (connection) {
        InputConnection.CONNECTED -> Color(0xFF22C55E)
        InputConnection.STANDBY -> Color(0xFFF59E0B)
        InputConnection.DISCONNECTED -> Color(0xFFEF4444)
        else -> LauncherColors.TextSecondary
    }
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}
