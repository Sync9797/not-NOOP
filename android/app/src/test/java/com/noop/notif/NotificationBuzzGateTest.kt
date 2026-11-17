package com.noop.notif

import com.noop.notif.NotificationBuzzGate.Decision
import org.junit.Assert.assertEquals
import org.junit.Test

/** One wrist buzz per notification, however many times the app re-posts it. */
class NotificationBuzzGateTest {

    @Test fun theLoggedBurstBecomesOneBuzz() {
        // A strap log showed one message firing three buzzes a second apart (12:34:45-47).
        val gate = NotificationBuzzGate()
        assertEquals(Decision.BUZZ, gate.decide("msg", onlyAlertOnce = false, nowMs = 0))
        assertEquals(Decision.SKIP_SAME_NOTIFICATION, gate.decide("msg", false, 1_000))
        assertEquals(Decision.SKIP_SAME_NOTIFICATION, gate.decide("msg", false, 2_000))
    }

    @Test fun aQuietUpdateNeverBuzzesAgain() {
        val gate = NotificationBuzzGate()
        gate.decide("msg", false, 0)
        assertEquals(Decision.SKIP_QUIET_UPDATE, gate.decide("msg", onlyAlertOnce = true, nowMs = 120_000))
    }

    @Test fun differentAppsInOneBurstGiveOneTap() {
        val gate = NotificationBuzzGate()
        assertEquals(Decision.BUZZ, gate.decide("a", false, 0))
        assertEquals(Decision.SKIP_BURST, gate.decide("b", false, 1_500))
        assertEquals(Decision.BUZZ, gate.decide("b", false, NotificationBuzzGate.BURST_WINDOW_MS))
    }

    @Test fun aNewMessageLaterStillBuzzes() {
        val gate = NotificationBuzzGate()
        gate.decide("msg", false, 0)
        // Same conversation, a real new message after the window.
        assertEquals(Decision.BUZZ, gate.decide("msg", false, NotificationBuzzGate.SAME_KEY_WINDOW_MS))
    }

    @Test fun aDismissedNotificationIsForgotten() {
        val gate = NotificationBuzzGate()
        gate.decide("msg", false, 0)
        gate.forget("msg")
        assertEquals(Decision.BUZZ, gate.decide("msg", onlyAlertOnce = true, nowMs = 10_000))
    }

    @Test fun aSkipDoesNotExtendTheWindow() {
        val gate = NotificationBuzzGate()
        gate.decide("a", false, 0)
        gate.decide("b", false, 3_000)          // skipped burst: must not restart the 4 s clock
        assertEquals(Decision.BUZZ, gate.decide("c", false, 4_000))
    }
}
