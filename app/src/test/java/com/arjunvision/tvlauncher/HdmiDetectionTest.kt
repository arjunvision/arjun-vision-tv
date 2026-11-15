package com.arjunvision.tvlauncher

import com.arjunvision.tvlauncher.domain.models.HdmiDetection
import com.arjunvision.tvlauncher.domain.models.HdmiInput
import com.arjunvision.tvlauncher.domain.models.InputConnection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HdmiDetectionTest {

    private val base = "com.mediatek.tvinput/.hdmi.HDMIInputService/"

    /** The real input list read from the MiTV-MOOR2 on 2026-11-15. */
    private val moor2 = listOf(
        HdmiInput(base + "HW4", "HDMI 1", null, InputConnection.CONNECTED),
        HdmiInput(base + "HW5", "HDMI 2", null, InputConnection.CONNECTED),
        HdmiInput(base + "HW6", "HDMI 3", null, InputConnection.STANDBY),
        HdmiInput(base + "HDMI100004", "PlayStation 5", null, InputConnection.CONNECTED, parentId = base + "HW4"),
    )

    @Test fun findsPs5CecInput() {
        assertEquals(base + "HDMI100004", HdmiDetection.detectPlayStation(moor2)?.inputId)
    }

    @Test fun airtelIsTheOtherConnectedPort() {
        assertEquals(base + "HW5", HdmiDetection.detectSetTopBox(moor2, null)?.inputId)
        assertEquals(base + "HW5", HdmiDetection.detectSetTopBox(moor2, base + "HDMI100004")?.inputId)
    }

    @Test fun unsureWhenTwoUnknownPortsAreConnected() {
        val two = moor2.map { if (it.inputId.endsWith("HW6")) it.copy(connection = InputConnection.CONNECTED) else it }
        assertNull(HdmiDetection.detectSetTopBox(two, null))
        assertEquals(2, HdmiDetection.setTopBoxChoices(two, null).count { it.connection == InputConnection.CONNECTED })
    }

    @Test fun choicesNeverOfferThePs5Port() {
        val ids = HdmiDetection.setTopBoxChoices(moor2, null).map { it.inputId }
        assertEquals(listOf(base + "HW5", base + "HW6"), ids)
    }

    @Test fun cecNameShownOnPortAndChild() {
        val port = moor2.first { it.inputId.endsWith("HW4") }
        val child = moor2.first { it.inputId.endsWith("HDMI100004") }
        assertEquals("PlayStation 5", HdmiDetection.deviceNameFor(port, moor2))
        assertEquals("PlayStation 5", HdmiDetection.deviceNameFor(child, moor2))
        assertNull(HdmiDetection.deviceNameFor(moor2.first { it.inputId.endsWith("HW5") }, moor2))
    }
}
