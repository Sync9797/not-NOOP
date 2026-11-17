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
        HapticPresetLab.saveFeel(ctx, 2, "two")
        HapticPresetLab.saveFeel(ctx, 0, "three")
        HapticPresetLab.saveFeel(ctx, 0, "long")   // a re-test replaces, never duplicates
        assertEquals("0: long, 2: two", HapticPresetLab.summary(ctx))
        assertNull(HapticPresetLab.savedFeel(ctx, 1))
    }

    @Test fun theOfferedIdsAreTheThreeAWhoop4Has() {
        // Firmware 41.17.6.0: GET_ALL_HAPTICS_PATTERN reported 0x07 and 3-15 were refused.
        assertEquals(listOf(0, 1, 2), HapticPresetLab.PRESET_IDS)
    }

    @Test fun logKeysAreDistinct() {
        val keys = HapticPresetLab.FEELS.map { it.first }
        assertEquals(keys.size, keys.toSet().size)
    }
}
