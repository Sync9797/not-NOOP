package com.noop.ui

import com.noop.protocol.DeviceFamily
import java.net.URLEncoder

/**
 * Not-Noop's in-app "Report a bug" (Settings → Support): save the scrubbed strap log where GitHub's file
 * picker can reach it, then open a new issue on this repository's bug form with what the app already
 * knows filled in.
 *
 * Nothing is sent by the app. GitHub's issue form takes its field values from the link's query string
 * (one parameter per field `id` in .github/ISSUE_TEMPLATE/bug_report.yml), and the reporter still reads
 * the form, attaches the file, describes the problem and presses Submit, from their own GitHub account.
 * That keeps the scope rule intact: data leaves the device only when the user sends it, to a destination
 * they can see. The strap log is the same PII-scrubbed export the "Share strap log" button produces.
 *
 * Pure, so the link is unit-tested without a Context.
 */
object BugReport {

    const val NEW_ISSUE_URL = "https://github.com/Sync9797/not-NOOP/issues/new"
    const val TEMPLATE = "bug_report.yml"

    /** GitHub rejects very long links; prefills here stay far below this, and anything longer is cut. */
    internal const val MAX_URL_CHARS = 6_000

    /**
     * The strap dropdown option to preselect, or null to leave the reporter to choose.
     *
     * Only a positively identified WHOOP 4.0 is filled in. The registry cannot tell a 5.0 from an MG, or
     * one Oura generation from another, and the legacy "WHOOP" label says nothing at all, so a guess there
     * would put a confident wrong answer on a report. Resolved through the one canonical family resolver,
     * never a string compare of the model label (#171).
     */
    fun strapOption(model: String?, brand: String?): String? =
        when {
            model == null -> null
            DeviceFamily.forRegistryDevice(model, brand) == DeviceFamily.WHOOP4 -> "WHOOP 4.0"
            else -> null
        }

    /** The OS line, in the form the bug form's own example uses ("Android 15"), with the SDK and phone. */
    fun osLine(release: String, sdk: Int, manufacturer: String, model: String): String =
        "Android $release (SDK $sdk) · $manufacturer $model"

    /** What the log field says, pointing the reporter at the file they need to attach. */
    fun logFieldText(savedFileName: String?): String =
        if (savedFileName != null) {
            "Attached: $savedFileName (NOOP saved it to Downloads; serial numbers and Bluetooth addresses " +
                "are removed). Drag it into this box or use \"Paste, drop, or click to add files\" below."
        } else {
            "Please attach the strap log: Settings → Share strap log in NOOP."
        }

    /** The prefilled new-issue link. Null fields are left out so the form shows its own empty state. */
    fun issueUrl(
        version: String,
        osLine: String,
        strapOption: String?,
        savedFileName: String?,
    ): String {
        val fields = linkedMapOf(
            "template" to TEMPLATE,
            "version" to version,
            "platform" to "Android",
            "os_version" to osLine,
            "strap" to strapOption,
            "log" to logFieldText(savedFileName),
        )
        val query = fields.entries
            .filter { it.value != null }
            .joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8").replace("+", "%20")}" }
        return "$NEW_ISSUE_URL?$query".take(MAX_URL_CHARS)
    }
}
