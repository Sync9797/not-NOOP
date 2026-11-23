package com.noop.data

import android.net.Uri
import com.noop.ui.BackupSync
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * The single-file backup destination (Not-Noop, ryanbr/noop#2868): a backup replaces the user's ONE file
 * in place, so it must truncate what was there, never delete it, and leave it untouched when nothing could
 * be written. Driven end to end against a real Room database and real files.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BackupSyncSingleFileTest {

    private val app get() = RuntimeEnvironment.getApplication()

    // Room's database is a process-wide singleton, but Robolectric gives each test a fresh app with fresh
    // storage, so a singleton kept from an earlier test points at a folder that is gone. The companion
    // close() closes AND forgets it, so each test builds its own.
    @Before fun freshDatabase() = WhoopDatabase.close()

    @After fun releaseDatabase() = WhoopDatabase.close()

    /** The app always runs a backup off the main thread, and Room refuses main-thread access. */
    private fun <T> offMain(block: () -> T): T {
        var out: Result<T>? = null
        val t = Thread { out = runCatching(block) }
        t.start()
        t.join()
        return out!!.getOrThrow()
    }

    @Test fun aLargerOldFileIsReplacedExactlyWithNoLeftoverTail() {
        // Make sure a database exists to back up.
        offMain { WhoopDatabase.get(app).openHelper.writableDatabase }
        val target = File(app.filesDir, "chosen-backup.noopbak")
        // An old file far bigger than a fresh, near-empty backup: a write that does not truncate would
        // leave most of this behind the new archive.
        target.writeBytes(ByteArray(4 * 1024 * 1024) { 0x5A })

        val result = offMain { BackupSync.writeToFile(app, Uri.fromFile(target)) }

        assertEquals(BackupSync.FileWriteResult.OK, result)
        assertTrue("old tail must be gone", target.length() < 4 * 1024 * 1024)
        assertTrue(target.inputStream().use { DataBackup.backupStreamIsIntact(it) })
        val tailWindow = target.readBytes().let { it.copyOfRange(maxOf(0, it.size - 65_557), it.size) }
        assertTrue("archive must end in its own central directory", DataBackup.hasEndOfCentralDirectory(tailWindow))
        // The staging copy in the cache is always cleaned up.
        assertFalse(File(app.cacheDir, "backup-sync-staging.noopbak").exists())
    }

    @Test fun anUnwritableTargetIsReportedAsNotWrittenAndLeftAlone() {
        offMain { WhoopDatabase.get(app).openHelper.writableDatabase }
        // A file inside a directory that does not exist cannot be opened for writing in any mode.
        val missingDir = File(app.filesDir, "no-such-dir-${System.nanoTime()}")
        val target = File(missingDir, "x.noopbak")
        assertEquals(BackupSync.FileWriteResult.NOT_WRITTEN, offMain { BackupSync.writeToFile(app, Uri.fromFile(target)) })
        assertFalse(target.exists())
    }

    @Test fun anExistingFileIsNotTouchedWhenItCannotBeOpened() {
        offMain { WhoopDatabase.get(app).openHelper.writableDatabase }
        val target = File(app.filesDir, "read-only.noopbak")
        val original = byteArrayOf(1, 2, 3, 4)
        target.writeBytes(original)
        target.setWritable(false, false)
        try {
            val result = offMain { BackupSync.writeToFile(app, Uri.fromFile(target)) }
            // Running as root ignores the read-only bit, in which case the write simply succeeds; either
            // way the file is never deleted and is never left as anything but the old bytes or a good backup.
            assertTrue(target.exists())
            if (result == BackupSync.FileWriteResult.NOT_WRITTEN) {
                assertArrayEquals(original, target.readBytes())
            } else {
                assertEquals(BackupSync.FileWriteResult.OK, result)
            }
        } finally {
            target.setWritable(true, false)
        }
    }

    @Test fun aPlainWriteCountsOnlyWithAnExactKnownSize() {
        // Truncating write: content check alone is enough when the size is unknown.
        assertTrue(BackupSync.fileCopyComplete(100, null, verdictIntact = true, truncated = true))
        // Plain "w": an unknown size cannot rule out a leftover tail.
        assertFalse(BackupSync.fileCopyComplete(100, null, verdictIntact = true, truncated = false))
        assertTrue(BackupSync.fileCopyComplete(100, 100, verdictIntact = true, truncated = false))
        // A size mismatch is never complete, whichever mode.
        assertFalse(BackupSync.fileCopyComplete(100, 4_000_000, verdictIntact = true, truncated = true))
        assertFalse(BackupSync.fileCopyComplete(100, 4_000_000, verdictIntact = true, truncated = false))
        // Content that does not read back is never complete.
        assertFalse(BackupSync.fileCopyComplete(100, 100, verdictIntact = false, truncated = true))
    }
}
