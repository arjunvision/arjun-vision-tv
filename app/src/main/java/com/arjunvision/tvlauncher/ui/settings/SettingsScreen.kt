package com.arjunvision.tvlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arjunvision.tvlauncher.domain.models.AnimationLevel
import com.arjunvision.tvlauncher.domain.models.CardSize
import com.arjunvision.tvlauncher.domain.models.Profile
import com.arjunvision.tvlauncher.ui.LauncherUiState
import com.arjunvision.tvlauncher.ui.LauncherViewModel
import com.arjunvision.tvlauncher.ui.components.FocusCard
import com.arjunvision.tvlauncher.ui.theme.LauncherColors

private data class SettingRow(
    val title: String,
    val value: String,
    val enabled: Boolean = true,
    val onSelect: () -> Unit = {},
)

/** Returns the item after [current], wrapping around. A value not in the list starts at the first item. */
private fun <T> List<T>.nextAfter(current: T): T {
    val index = indexOf(current)
    return this[(index + 1).mod(size)]
}

@Composable
fun SettingsScreen(state: LauncherUiState, viewModel: LauncherViewModel) {
    val settings = state.settings
    val firstRow = remember { FocusRequester() }
    val motion = settings.animation.factor
    val hdmiOptions: List<String?> = listOf<String?>(null) + state.hdmiInputs.map { it.inputId }

    LaunchedEffect(Unit) {
        runCatching { firstRow.requestFocus() }
    }

    val rows = listOf(
        SettingRow(
            title = "Profile",
            value = settings.profile.title,
            onSelect = {
                viewModel.setProfile(Profile.entries.nextAfter(settings.profile))
            },
        ),
        SettingRow(
            title = "Clock format",
            value = if (settings.clock24h) "24-hour" else "12-hour",
            onSelect = { viewModel.updateSettings { it.copy(clock24h = !it.clock24h) } },
        ),
        SettingRow(
            title = "Card size",
            value = settings.cardSize.label,
            onSelect = {
                viewModel.updateSettings { it.copy(cardSize = CardSize.entries.nextAfter(it.cardSize)) }
            },
        ),
        SettingRow(
            title = "Animation intensity",
            value = settings.animation.label,
            onSelect = {
                viewModel.updateSettings { it.copy(animation = AnimationLevel.entries.nextAfter(it.animation)) }
            },
        ),
        SettingRow(
            title = "Startup animation",
            value = if (settings.startupAnimation) "On" else "Off",
            onSelect = { viewModel.updateSettings { it.copy(startupAnimation = !it.startupAnimation) } },
        ),
        SettingRow(
            title = "Smart HDMI Hub (Airtel DTH, PlayStation 5, test switch)",
            value = "Open",
            onSelect = { viewModel.show(com.arjunvision.tvlauncher.ui.Screen.HdmiHub) },
        ),
        SettingRow(
            title = "Airtel DTH HDMI input (Father's TV)",
            value = state.hdmiLabel(settings.cableInputId) ?: "Not set",
            onSelect = {
                viewModel.updateSettings { it.copy(cableInputId = hdmiOptions.nextAfter(it.cableInputId)) }
            },
        ),
        SettingRow(
            title = "PlayStation 5 HDMI input (My Gaming)",
            value = state.hdmiLabel(settings.playStationInputId) ?: "Not set",
            onSelect = {
                viewModel.updateSettings {
                    it.copy(playStationInputId = hdmiOptions.nextAfter(it.playStationInputId))
                }
            },
        ),
        SettingRow(
            title = "Refresh apps and HDMI inputs",
            value = "${state.apps.size} apps, ${state.hdmiInputs.size} HDMI",
            onSelect = { viewModel.refreshDevices() },
        ),
        SettingRow(
            title = "Reset to defaults",
            value = "Clears saved settings",
            onSelect = { viewModel.resetSettings() },
        ),
        SettingRow(
            title = "Online Theme Studio (wallpaper, accent, festivals)",
            value = com.arjunvision.tvlauncher.ui.theme.LauncherStyle.themeName,
            onSelect = { viewModel.show(com.arjunvision.tvlauncher.ui.Screen.ThemeStudio) },
        ),
        SettingRow(
            title = "Lite Mode (no aurora motion or particles)",
            value = if (settings.liteMode) "On" else "Off",
            onSelect = { viewModel.updateSettings { it.copy(liteMode = !it.liteMode) } },
        ),
        SettingRow(
            title = "Clock screensaver",
            value = if (settings.screensaverMinutes == 0) "Off" else "After ${settings.screensaverMinutes} min",
            onSelect = {
                viewModel.updateSettings { it.copy(screensaverMinutes = listOf(0, 2, 5, 10).nextAfter(it.screensaverMinutes)) }
            },
        ),
        SettingRow(
            title = "Default home app (return to the Xiaomi launcher)",
            value = "Open Android settings",
            onSelect = { viewModel.openSystemHomeSettings() },
        ),
        SettingRow(title = "Company logo", value = "Arjun Vision bulb logo (built in)", enabled = false),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LauncherColors.Background, LauncherColors.Surface)))
            .padding(horizontal = 48.dp, vertical = 40.dp),
    ) {
        Text(
            text = "Settings",
            color = LauncherColors.TextPrimary,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(6.dp))
        val inputSummary = if (state.hdmiInputs.isEmpty()) {
            "The TV did not report any HDMI inputs. Connect the device, then choose Refresh."
        } else {
            "HDMI inputs reported by the TV: " + state.hdmiInputs.joinToString { input ->
                listOfNotNull(input.label, input.connection.label.ifBlank { null }).joinToString(" · ")
            }
        }
        Text(
            text = inputSummary,
            color = LauncherColors.TextSecondary,
            fontSize = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(rows) { index, row ->
                FocusCard(
                    onClick = row.onSelect,
                    motion = motion,
                    shape = RoundedCornerShape(20.dp),
                    background = Brush.horizontalGradient(
                        listOf(LauncherColors.Surface, LauncherColors.Background),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .then(if (index == 0) Modifier.focusRequester(firstRow) else Modifier),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                    ) {
                        Text(
                            text = row.title,
                            color = if (row.enabled) LauncherColors.TextPrimary else LauncherColors.TextSecondary,
                            fontSize = 21.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = row.value,
                            color = if (row.enabled) LauncherColors.Blue else LauncherColors.TextSecondary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
