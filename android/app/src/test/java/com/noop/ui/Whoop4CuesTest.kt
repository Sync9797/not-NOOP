package com.noop.ui

import com.noop.protocol.LiveSessionHaptics.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Multi-tap cues become one command on a WHOOP 4.0, where each command plays a whole preset. */
class Whoop4CuesTest {

    @Test fun liveSessionCuesAreOneCommandEachAndCannotBeConfused() {
        val push = Whoop4Cues.liveSession(Signal.PUSH)
        val ease = Whoop4Cues.liveSession(Signal.EASE_OFF)
        assertEquals(Whoop4Cues.Command(presetId = 2, loops = 1), push)   // two pairs of short buzzes
        assertEquals(Whoop4Cues.Command(presetId = 0, loops = 1), ease)   // one long buzz
        assertNotEquals(push.presetId, ease.presetId)
    }

    @Test fun onlyPresetsTheStrapReportsAreUsed() {
        // Firmware 41.17.6.0 reported presets 0-2 and refused 3-15.
        for (s in Signal.entries) assertTrue(Whoop4Cues.liveSession(s).presetId in 0..2)
    }

    @Test fun intervalsCountdownIsSilentOnAWhoop4Only() {
        for (r in 1..3) {
            assertTrue(Whoop4Cues.intervalsCountdownTick(r, oneCommandPerCue = false))
            assertFalse(Whoop4Cues.intervalsCountdownTick(r, oneCommandPerCue = true))
        }
        assertFalse(Whoop4Cues.intervalsCountdownTick(4, oneCommandPerCue = false))
        assertFalse(Whoop4Cues.intervalsCountdownTick(0, oneCommandPerCue = false))
    }
}
