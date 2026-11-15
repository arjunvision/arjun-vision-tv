package com.arjunvision.tvlauncher.platform.hdmi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import com.arjunvision.tvlauncher.domain.models.HdmiInput
import com.arjunvision.tvlauncher.domain.models.InputConnection

/**
 * HDMI discovery uses the public TvInputManager API, so port numbers and labels come from the TV,
 * never from guesses.
 *
 * Switching uses the documented TV-app route: ACTION_VIEW on a passthrough-input channel URI
 * (TvContract.buildChannelUriForPassthroughInput). Whether the Mi TV's TV app honours it is
 * verified on the device, not assumed. If it does not, the UI falls back to instructions.
 */
class HdmiInputRepository(private val context: Context) {

    fun discoverHdmiInputs(): List<HdmiInput> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
            ?: return emptyList()
        return manager.tvInputList
            .filter { it.type == TvInputInfo.TYPE_HDMI || (it.isPassthroughInput && it.type !in ANALOG_TYPES) }
            .map { info ->
                HdmiInput(
                    inputId = info.id,
                    label = info.loadLabel(context)?.toString().orEmpty().ifBlank { "HDMI input" },
                    // The physical port number is not in the public TvInputInfo API, so it is not guessed.
                    portNumber = null,
                    connection = stateOf(manager, info.id),
                )
            }
    }

    private fun stateOf(manager: TvInputManager, id: String): InputConnection = runCatching {
        when (manager.getInputState(id)) {
            TvInputManager.INPUT_STATE_CONNECTED -> InputConnection.CONNECTED
            TvInputManager.INPUT_STATE_CONNECTED_STANDBY -> InputConnection.STANDBY
            TvInputManager.INPUT_STATE_DISCONNECTED -> InputConnection.DISCONNECTED
            else -> InputConnection.UNKNOWN
        }
    }.getOrDefault(InputConnection.UNKNOWN)

    /** Returns true if the TV app accepted the request. False means the caller should show fallback instructions. */
    fun openPassthroughInput(inputId: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private companion object {
        /** Analog inputs (AV, component, SCART, VGA, S-Video) are not offered for Cable/PS5. */
        val ANALOG_TYPES = setOf(
            TvInputInfo.TYPE_COMPOSITE,
            TvInputInfo.TYPE_COMPONENT,
            TvInputInfo.TYPE_SCART,
            TvInputInfo.TYPE_VGA,
            TvInputInfo.TYPE_SVIDEO,
        )
    }
}
