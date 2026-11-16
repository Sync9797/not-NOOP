package com.noop.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.URLDecoder

/** The prefilled GitHub bug-report link built by Settings → Support → Report a bug. */
class BugReportTest {

    private fun params(url: String): Map<String, String> =
        url.substringAfter('?').split('&').associate {
            val (k, v) = it.split('=', limit = 2)
            k to URLDecoder.decode(v, "UTF-8")
        }

    @Test fun theLinkTargetsThisRepositorysBugForm() {
        val url = BugReport.issueUrl("12.3.2 (563)", "Android 16 (SDK 36) · Google Pixel 9", "WHOOP 4.0", "noop-strap-log-x.txt")
        assertTrue(url.startsWith("https://github.com/Sync9797/not-NOOP/issues/new?"))
        val p = params(url)
        assertEquals("bug_report.yml", p["template"])
        assertEquals("12.3.2 (563)", p["version"])
        assertEquals("Android", p["platform"])
        assertEquals("Android 16 (SDK 36) · Google Pixel 9", p["os_version"])
        assertEquals("WHOOP 4.0", p["strap"])
        assertTrue(p.getValue("log").contains("noop-strap-log-x.txt"))
    }

    @Test fun everyPrefilledKeyIsAFieldOfTheForm() {
        // GitHub ignores unknown keys silently, so a renamed field id would quietly stop prefilling.
        val form = File("../../.github/ISSUE_TEMPLATE/bug_report.yml").takeIf { it.exists() }
            ?: File(".github/ISSUE_TEMPLATE/bug_report.yml")
        val ids = Regex("""(?m)^\s+id:\s*(\S+)""").findAll(form.readText()).map { it.groupValues[1] }.toSet()
        val keys = params(BugReport.issueUrl("v", "os", "WHOOP 4.0", "f.txt")).keys - "template"
        assertTrue("unknown form fields: ${keys - ids}", ids.containsAll(keys))
    }

    @Test fun anUnknownStrapIsLeftForTheReporter() {
        val p = params(BugReport.issueUrl("v", "os", null, null))
        assertFalse("strap" in p)
        assertTrue(p.getValue("log").contains("Share strap log"))
    }

    @Test fun onlyAPositivelyIdentifiedFourPointZeroIsPrefilled() {
        assertEquals("WHOOP 4.0", BugReport.strapOption("4.0", "WHOOP"))
        assertEquals("WHOOP 4.0", BugReport.strapOption("WHOOP 4.0", null))
        // 5.0 and MG share one registry label; the legacy "WHOOP" says nothing; a ring is not a WHOOP.
        assertNull(BugReport.strapOption("5.0 MG", "WHOOP"))
        assertNull(BugReport.strapOption("WHOOP", "WHOOP"))
        assertNull(BugReport.strapOption("4.0", "Oura"))
        assertNull(BugReport.strapOption(null, null))
    }

    @Test fun spacesAreEncodedAsPercentTwenty() {
        // A "+" would show up literally in GitHub's form fields.
        val url = BugReport.issueUrl("v", "Android 16", null, null)
        assertFalse(url.contains('+'))
        assertTrue(url.length <= BugReport.MAX_URL_CHARS)
    }
}
