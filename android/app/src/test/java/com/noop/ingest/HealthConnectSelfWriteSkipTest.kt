package com.noop.ingest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #528 — the importer must NOT re-ingest records NOOP itself wrote to Health Connect. Without this,
 * turning on "share back" and then importing would re-read NOOP's own daily totals and sum them on
 * top of the original source records (cumulative steps / active energy / sleep would ~double). Pins
 * the pure package-match decision used at [HealthConnectImporter] readAll's choke point.
 */
class HealthConnectSelfWriteSkipTest {

    private val self = "com.not.noop.oop"

    @Test fun skipsRecordsNoopWroteItself() {
        assertTrue(HealthConnectImporter.isSelfWritten(self, self))
    }

    @Test fun keepsRecordsFromOtherSources() {
        assertFalse(HealthConnectImporter.isSelfWritten("com.google.android.apps.fitness", self))
        assertFalse(HealthConnectImporter.isSelfWritten("com.sec.android.app.shealth", self))
    }

    @Test fun skipsEveryNoopAndNotNoopBuild() {
        // Not-Noop moved off the original app id, so the original NOOP app and the other Not-Noop
        // variants are no longer "self", but their Health Connect writes are still NOOP strap data.
        for (pkg in listOf(
            "com.noop.whoop", "com.noop.whoop.debug", "com.noop.whoop.staging", "com.noop.whoop.demo",
            "com.notnoop.whoop", "com.notnoop.whoop.debug",
            "com.not.noop.oop.debug", "com.not.noop.oop.staging", "com.not.noop.oop.demo",
        )) {
            assertTrue(pkg, HealthConnectImporter.isSelfWritten(pkg, self))
        }
    }

    @Test fun doesNotSkipLookalikePackages() {
        assertFalse(HealthConnectImporter.isSelfWritten("com.noop.whoopsie", self))
        assertFalse(HealthConnectImporter.isSelfWritten("com.notnoop", self))
        assertFalse(HealthConnectImporter.isSelfWritten("com.not.noop.oopsie", self))
    }

    @Test fun keepsWhenOriginUnknownOrSelfBlank() {
        // Err toward keeping data: if we can't determine self, skip nothing.
        assertFalse(HealthConnectImporter.isSelfWritten("", self))
        assertFalse(HealthConnectImporter.isSelfWritten("com.google.android.apps.fitness", ""))
    }
}
