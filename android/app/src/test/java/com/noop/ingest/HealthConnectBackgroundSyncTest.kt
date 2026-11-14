package com.noop.ingest

import org.junit.Assert.assertEquals
import org.junit.Test

/** The optional background Health Connect sync follows the user's sync interval, floored at one hour. */
class HealthConnectBackgroundSyncTest {

    @Test fun intervalFollowsTheSyncSetting() {
        assertEquals(6L, HealthConnectBackgroundSync.intervalHours(6))
        assertEquals(12L, HealthConnectBackgroundSync.intervalHours(12))
        assertEquals(24L, HealthConnectBackgroundSync.intervalHours(24))
    }

    @Test fun intervalIsNeverBelowOneHour() {
        assertEquals(1L, HealthConnectBackgroundSync.intervalHours(0))
        assertEquals(1L, HealthConnectBackgroundSync.intervalHours(-3))
    }

    @Test fun windowIsShort() {
        assertEquals(3, HealthConnectBackgroundSync.WINDOW_DAYS)
    }
}
