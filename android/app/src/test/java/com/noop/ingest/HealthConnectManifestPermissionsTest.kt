package com.noop.ingest

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every Health Connect permission the app REQUESTS must be DECLARED in AndroidManifest.xml. Health
 * Connect silently drops a request for an undeclared permission, so the insert or read behind it throws
 * SecurityException forever and the user has no way to grant it. That is how VO2 max writeback left
 * the Data Sources card stuck on "Sharing paused — permission was revoked": the writer requested
 * WRITE_VO2_MAX, but the manifest only declared READ_VO2_MAX.
 */
class HealthConnectManifestPermissionsTest {

    private val declared: Set<String> by lazy {
        // Gradle runs unit tests from the module directory; fall back to the repo root layout.
        val manifest = listOf("src/main/AndroidManifest.xml", "app/src/main/AndroidManifest.xml", "android/app/src/main/AndroidManifest.xml")
            .map(::File).first { it.exists() }
        Regex("""<uses-permission\s+android:name="(android\.permission\.health\.[A-Z0-9_]+)"""")
            .findAll(manifest.readText()).map { it.groupValues[1] }.toSet()
    }

    private fun assertDeclared(label: String, requested: Set<String>) {
        val missing = requested - declared
        assertTrue("$label requests permissions the manifest does not declare: $missing", missing.isEmpty())
    }

    @Test fun writebackPermissionsAreDeclared() =
        assertDeclared("HealthConnectWriter.PERMISSIONS", HealthConnectWriter.PERMISSIONS)

    @Test fun vo2MaxWritePermissionIsDeclared() =
        assertDeclared("HealthConnectWriter.VO2MAX_PERMISSIONS", HealthConnectWriter.VO2MAX_PERMISSIONS)

    @Test fun exerciseWritePermissionsAreDeclared() =
        assertDeclared("HealthConnectWriter.EXERCISE_PERMISSIONS", HealthConnectWriter.EXERCISE_PERMISSIONS)

    @Test fun importReadPermissionsAreDeclared() =
        assertDeclared("HealthConnectImporter.PERMISSIONS", HealthConnectImporter.PERMISSIONS)

    @Test fun backgroundReadPermissionIsDeclared() =
        assertDeclared("HealthConnectBackgroundSync.PERMISSION", setOf(HealthConnectBackgroundSync.PERMISSION))
}
