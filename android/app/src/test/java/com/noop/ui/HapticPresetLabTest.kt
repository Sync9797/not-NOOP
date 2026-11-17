package com.noop.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The Test Centre vibration-preset experiment's bookkeeping. */
@RunWith(RobolectricTestRunner::class)
class HapticPresetLabTest {

    @Test fun recordedFeelsAreSummarisedInIdOrder() {
        val ctx = RuntimeEnvironment.getApplication()
        assertEquals("", HapticPresetLab.summary(ctx))
        HapticPresetLab.saveFeel(ctx, 5, "nothing")
        HapticPresetLab.saveFeel(ctx, 1, "two")
        HapticPresetLab.saveFeel(ctx, 1, "one short")   // a re-test replaces, never duplicates
        assertEquals("1: one short, 5: nothing", HapticPresetLab.summary(ctx))
        assertNull(HapticPresetLab.savedFeel(ctx, 2))
    }

    @Test fun theOfferedIdsIncludeTheKnownPresetAndFitInAByte() {
        assertTrue(2 in HapticPresetLab.PRESET_IDS)
        assertTrue(HapticPresetLab.PRESET_IDS.all { it in 0..255 })
    }

    @Test fun logKeysAreDistinct() {
        val keys = HapticPresetLab.FEELS.map { it.first }
        assertEquals(keys.size, keys.toSet().size)
    }
}
