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

    /** The columns AI Widget Maker 0.9.13 reads for its Stress and Coach brief widgets, by name. */
    @Test
    fun theCompanionsStressAndBriefColumnsAreShared() {
        val cols = WidgetShare.COLUMNS.toList()
        assertTrue(cols.containsAll(listOf("stress_series", "coach_brief", "coach_brief_at")))
        // Appended after the original nine, so nothing an older companion reads has moved.
        assertEquals(
            listOf("recovery", "rest", "effort", "hr", "hr_stale", "battery", "connected", "updated_at", "hr_series"),
            cols.take(9),
        )
    }

    /** Stress goes out in the "epochSec:level:moving" form the companion parses, "-" unscored. */
    @Test
    fun aRowCarriesTodaysStressAndTheBrief() {
        val snap = WidgetSnapshot(
            recoveryPct = 75, restPct = null, effortPct = null, heartRate = null, heartRateStale = false,
            batteryPct = null, connected = false, hrSeries = emptyList(), updatedAtMs = 1L,
            stressSeries = listOf(StressPoint(32400, 2.8), StressPoint(36000, null, moving = true), StressPoint(39600, 1.25)),
            stressDay = 0L,
        )
        val row = WidgetShare.row(snap, "### Brief\n* Rest today.", 1700L)
        val i = WidgetShare.COLUMNS.indexOf("stress_series")
        assertEquals("32400:2.8:0,36000:-:1,39600:1.25:0", row[i])
        assertEquals("### Brief\n* Rest today.", row[WidgetShare.COLUMNS.indexOf("coach_brief")])
        assertEquals(1700L, row[WidgetShare.COLUMNS.indexOf("coach_brief_at")])
        // No brief: empty text and a zero time, which the companion reads as "no coach brief".
        val none = WidgetShare.row(snap, "  ", 1700L)
        assertEquals("", none[WidgetShare.COLUMNS.indexOf("coach_brief")].toString().trim())
        assertEquals(0L, none[WidgetShare.COLUMNS.indexOf("coach_brief_at")])
    }
}
