package com.arjunvision.tvlauncher.platform.hdmi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            .filter { info -> runCatching { !info.isHidden(context) }.getOrDefault(true) }
            .map { info ->
                HdmiInput(
                    inputId = info.id,
                    label = info.loadLabel(context)?.toString().orEmpty().ifBlank { "HDMI input" },
                    // The physical port number is not in the public TvInputInfo API, so it is not guessed.
                    portNumber = null,
                    connection = stateOf(manager, info.id),
                    parentId = info.parentId,
                    customLabel = runCatching { info.loadCustomLabel(context)?.toString() }.getOrNull()?.ifBlank { null },
                    typeLabel = typeLabelOf(info.type),
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

    /**
     * Asks the TV's own input app to show [inputId]. The intent must resolve to a TV app
     * (on this TV `com.mitv.livetv`); anything else (a file manager, say) is refused so a wrong
     * app never opens. Returns false when nothing suitable handles it: show manual steps then.
     * A true result means "the TV app accepted the request", not "the picture changed".
     */
    fun openPassthroughInput(inputId: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
        @Suppress("DEPRECATION")
        val target = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
            ?: return false
        if (!isTvInputApp(target.packageName)) return false
        intent.setClassName(target.packageName, target.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /**
     * Which app would handle switching to [inputId], for the Hub's diagnostics. Null if none.
     * Route order on this TV: (1) Xiaomi `com.xiaomi.mitv.tvplayer` – not installed on the
     * MiTV-MOOR2, so skipped; (2) standard TV Input Framework passthrough URI → `com.mitv.livetv`;
     * (3) manual steps with the original remote.
     */
    fun inputAppFor(inputId: String): String? {
        val intent = Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(inputId))
        @Suppress("DEPRECATION")
        val pkg = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
        return pkg?.takeIf { isTvInputApp(it) }
    }

    private fun typeLabelOf(type: Int): String = when (type) {
        TvInputInfo.TYPE_HDMI -> "HDMI"
        TvInputInfo.TYPE_DISPLAY_PORT -> "DisplayPort"
        TvInputInfo.TYPE_DVI -> "DVI"
        TvInputInfo.TYPE_TUNER -> "Tuner"
        else -> "Other"
    }

    private fun isTvInputApp(pkg: String): Boolean =
        pkg == "com.mitv.livetv" || pkg == "com.android.tv" || pkg == "com.google.android.tv" ||
            pkg.contains("livetv") || pkg.contains("tvplayer")

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
