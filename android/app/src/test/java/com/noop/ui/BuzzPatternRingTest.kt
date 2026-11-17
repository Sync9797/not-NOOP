package com.noop.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The calls-only Ring pattern (WHOOP 4.0 preset 1). */
@RunWith(RobolectricTestRunner::class)
class BuzzPatternRingTest {

    @Test fun ringIsOfferedForCallsButNeverForApps() {
        assertTrue(BuzzPattern.Ring in BuzzPattern.entries)
        assertFalse(BuzzPattern.Ring in BuzzPattern.forApps)
        assertEquals(BuzzPattern.entries.size - 1, BuzzPattern.forApps.size)
    }

    @Test fun callsDefaultToRingButKeepAnEarlierChoice() {
        val ctx = RuntimeEnvironment.getApplication()
        ctx.getSharedPreferences("noop_notif_prefs", android.content.Context.MODE_PRIVATE).edit().clear().commit()
        assertEquals(BuzzPattern.Ring, NotifPrefs.callPattern(ctx))
        NotifPrefs.setCallPattern(ctx, BuzzPattern.Double)
        assertEquals(BuzzPattern.Double, NotifPrefs.callPattern(ctx))
    }
}
