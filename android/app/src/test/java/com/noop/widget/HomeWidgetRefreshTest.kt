package com.noop.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The home-widget safety net (ryanbr/noop#2807) steps aside only while the service is visibly pushing. */
class HomeWidgetRefreshTest {
    private val now = 1_800_000_000_000L

    @Test fun aLiveRecentPushMeansTheServiceHasIt() {
        assertFalse(HomeWidgetRefresh.shouldRefresh(storedConnected = true, storedUpdatedAtMs = now - 30_000, nowMs = now))
    }

    @Test fun disconnectedAlwaysRefreshes() {
        assertTrue(HomeWidgetRefresh.shouldRefresh(storedConnected = false, storedUpdatedAtMs = now - 30_000, nowMs = now))
    }

    @Test fun aConnectedSnapshotThatStoppedUpdatingIsNotTrusted() {
        // The service died while "Connected" was stored: the widget must not stay frozen on it.
        assertTrue(HomeWidgetRefresh.shouldRefresh(true, now - HomeWidgetRefresh.LIVE_PUSH_MAX_AGE_MS, now))
        assertTrue(HomeWidgetRefresh.shouldRefresh(true, now - 3_600_000, now))
    }

    @Test fun aStampFromTheFutureIsNotTrustedEither() {
        // A clock change can leave the stored stamp ahead of now.
        assertTrue(HomeWidgetRefresh.shouldRefresh(true, now + 60_000, now))
    }
}
