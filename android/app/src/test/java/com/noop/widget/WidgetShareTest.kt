package com.noop.widget

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
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
            arrayOf<Any>(75, 91, 45, 62, 1, -1, 1, 1234L, "100:60,160:64", "", "", 0L),
            WidgetShare.row(snap),
        )
    }

    @Test
    fun aRowCarriesTodaysStressAndTheCoachBrief() {
        val snap = WidgetSnapshot(
            stressSeries = listOf(StressPoint(3600, 1.5), StressPoint(7200, null), StressPoint(10800, 2.25, moving = true)),
            stressDay = 1L,
        )
        val row = WidgetShare.row(snap, WidgetShare.Brief("Charge is 67%.", 99L))
        assertEquals("3600:1.5:0,7200:-:0,10800:2.25:1", row[9])
        assertEquals("Charge is 67%.", row[10])
        assertEquals(99L, row[11])
        assertEquals(WidgetShare.COLUMNS.size, row.size)
    }
}
