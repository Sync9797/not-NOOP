package com.noop.widget

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetShareTest {
    @Test
    fun onlyTheCompanionMayRead() {
        assertTrue(WidgetShare.allowed("com.sidequest.widgetmaker"))
        assertFalse(WidgetShare.allowed("com.example.other"))
        assertFalse(WidgetShare.allowed(null))
    }

    @Test
    fun aRowCarriesTheSnapshotWithBlanksAsMinusOne() {
        val snap = WidgetSnapshot(
            recoveryPct = 75, restPct = 91, effortPct = 45, heartRate = 62, heartRateStale = true,
            batteryPct = null, connected = true, hrSeries = listOf(HrPoint(100, 60), HrPoint(160, 64)),
            updatedAtMs = 1234L,
        )
        assertArrayEquals(
            arrayOf<Any>(75, 91, 45, 62, 1, -1, 1, 1234L, "100:60,160:64"),
            WidgetShare.row(snap),
        )
    }
}
