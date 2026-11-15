package com.arjunvision.tvlauncher.domain.models

/**
 * Works out which TV input belongs to which device, using only what TvInputManager reports
 * (labels, CEC child inputs, connection state). Pure Kotlin so it is unit-tested on the JVM.
 *
 * On the MiTV-MOOR2 (verified 2026-11-15): HDMI 1 = HW4 with CEC child HDMI100004 "PlayStation 5";
 * HDMI 2 = HW5 connected with no CEC name (the Airtel box); HDMI 3 = HW6 standby.
 */
object HdmiDetection {

    /** The PS5's own CEC input (preferred: selecting it wakes the console over HDMI-CEC). */
    fun detectPlayStation(inputs: List<HdmiInput>): HdmiInput? =
        inputs.firstOrNull { it.parentId != null && it.label.contains("PlayStation", ignoreCase = true) }

    /**
     * The Airtel set-top box: the only connected HDMI port that is not the PS5's port.
     * Returns null when this is not certain (none or several candidates) so the UI can ask once.
     */
    fun detectSetTopBox(inputs: List<HdmiInput>, playStationInputId: String?): HdmiInput? {
        val ps5 = inputs.firstOrNull { it.inputId == playStationInputId } ?: detectPlayStation(inputs)
        val ps5Port = ps5?.parentId ?: ps5?.inputId
        val ports = inputs.filter { it.parentId == null && it.inputId != ps5Port }
        val connected = ports.filter { it.connection == InputConnection.CONNECTED }
        return connected.singleOrNull()
    }

    /** Ports the user can pick for the set-top box (top-level inputs, PS5 port excluded). */
    fun setTopBoxChoices(inputs: List<HdmiInput>, playStationInputId: String?): List<HdmiInput> {
        val ps5 = inputs.firstOrNull { it.inputId == playStationInputId } ?: detectPlayStation(inputs)
        val ps5Port = ps5?.parentId ?: ps5?.inputId
        return inputs.filter { it.parentId == null && it.inputId != ps5Port }
            .sortedBy { if (it.connection == InputConnection.CONNECTED) 0 else 1 }
    }

    /** The CEC device name shown for [input]: its own label if it is a CEC child, else its child's label. */
    fun deviceNameFor(input: HdmiInput, inputs: List<HdmiInput>): String? =
        if (input.parentId != null) input.label
        else inputs.firstOrNull { it.parentId == input.inputId }?.label
}
