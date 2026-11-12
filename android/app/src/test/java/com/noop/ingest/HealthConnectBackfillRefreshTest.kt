package com.noop.ingest

import com.noop.data.DailyMetric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A day the Health Connect import backfilled under "my-whoop" must stay refreshable on later imports.
 * Before this, the importer counted its own earlier rows as strap coverage, so every backfilled day
 * froze at its first import and a night the watch uploaded after that sync never arrived.
 */
class HealthConnectBackfillRefreshTest {

    private fun hcRow(day: String) = DailyMetric(deviceId = "my-whoop", day = day, restingHr = 55, avgHrv = 60.0)

    @Test fun ownBackfilledDaysAreRefreshable() {
        val rows = listOf(hcRow("2026-11-10"), hcRow("2026-11-11"))
        assertEquals(
            setOf("2026-11-10", "2026-11-11"),
            HealthConnectImporter.ownBackfillDays(rows, setOf("2026-11-10", "2026-11-11")),
        )
    }

    @Test fun daysNotWrittenByTheImportStayProtected() {
        // A "my-whoop" row we never recorded as ours (WHOOP export, or data from before this fix).
        val rows = listOf(hcRow("2026-11-09"))
        assertTrue(HealthConnectImporter.ownBackfillDays(rows, emptySet()).isEmpty())
    }

    @Test fun dayReplacedByRealWhoopImportStaysProtected() {
        // We backfilled it once, but a WHOOP export has since written recovery / strain over it.
        val whoop = hcRow("2026-11-08").copy(recovery = 72.0, strain = 11.4, deepMin = 80.0)
        assertFalse(HealthConnectImporter.isHcShaped(whoop))
        assertTrue(HealthConnectImporter.ownBackfillDays(listOf(whoop), setOf("2026-11-08")).isEmpty())
    }

    @Test fun hcShapedMatchesWhatTheImportWrites() {
        assertTrue(
            HealthConnectImporter.isHcShaped(
                DailyMetric(
                    deviceId = "my-whoop", day = "2026-11-12", totalSleepMin = 420.0, restingHr = 52,
                    avgHrv = 64.0, spo2Pct = 96.0, respRateBpm = 14.5, exerciseCount = 1,
                ),
            ),
        )
    }
}
