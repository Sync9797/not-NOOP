package com.noop.widget

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * Shares the widget snapshot with one companion app on the same phone, AI Widget Maker, so its
 * widgets can show NOOP's own Charge, Rest, Effort, heart rate and strap battery without a round trip
 * through Health Connect.
 *
 * On-device only: nothing leaves the phone. The provider answers only the allow-listed package
 * (checked against the caller Android reports), and the change ping is an explicit broadcast to that
 * package alone. The values are the ones NOOP's own widgets already render, read from the same store.
 */
object WidgetShare {
    /** The only app the snapshot is shared with. */
    const val COMPANION = "com.sidequest.widgetmaker"

    /** Told to the companion when the snapshot changes, so its widgets redraw. */
    const val ACTION_CHANGED = "com.sidequest.widgetmaker.NOOP_CHANGED"

    /** Pings are spaced at least this far apart: the snapshot can change about once a minute while
     *  the strap streams, and the companion only needs to hear about it once in that time. */
    private const val PING_GAP_MS = 60_000L

    @Volatile private var lastPingMs = 0L

    /** Where NOOP's own Coach brief widget reads the latest brief (written by CoachBriefScheduler). */
    private const val BRIEF_PREFS = "noop_widget"
    private const val BRIEF_TEXT = "coachBriefText"
    private const val BRIEF_AT = "coachBriefDateMs"

    /**
     * The shared columns. The last three were added for the companion's Stress and Coach brief widgets
     * (AI Widget Maker 0.9.13): `stress_series` is today's curve in [StressTrace.encode]'s
     * "epochSec:level:moving" form ("-" for an unscored hour), `coach_brief` the brief's Markdown and
     * `coach_brief_at` when it was written (epoch ms). Appended, so an older companion reading by
     * column name is unaffected.
     */
    val COLUMNS = arrayOf(
        "recovery", "rest", "effort", "hr", "hr_stale", "battery", "connected", "updated_at", "hr_series",
        "stress_series", "coach_brief", "coach_brief_at",
    )

    /**
     * One row of [COLUMNS] for [snap] and the Coach [brief]; -1 marks a value NOOP has not got yet, and
     * an empty string / 0 a stress curve or brief it has not got. Pure, for tests.
     */
    fun row(snap: WidgetSnapshot, brief: String? = null, briefAtMs: Long = 0L): Array<Any> = arrayOf(
        snap.recoveryPct ?: -1,
        snap.restPct ?: -1,
        snap.effortPct ?: -1,
        snap.heartRate ?: -1,
        if (snap.heartRateStale) 1 else 0,
        snap.batteryPct ?: -1,
        if (snap.connected) 1 else 0,
        snap.updatedAtMs,
        HrTrace.encode(snap.hrSeries),
        StressTrace.encode(snap.stressSeries),
        brief.orEmpty(),
        if (brief.isNullOrBlank()) 0L else briefAtMs,
    )

    /** The latest Coach brief and when it was written, as NOOP's own brief widget reads them. */
    fun brief(context: Context): Pair<String?, Long> {
        val p = context.applicationContext.getSharedPreferences(BRIEF_PREFS, Context.MODE_PRIVATE)
        return p.getString(BRIEF_TEXT, null) to p.getLong(BRIEF_AT, 0L)
    }

    /**
     * Whether the companion is installed. It now draws a Stress widget from [COLUMNS], so its presence
     * counts as a placed stress widget for scoring (see WidgetSnapshotStore.stressWidgetPlacement).
     * Visible to this app through the manifest's <queries> entry for it.
     */
    fun companionInstalled(context: Context): Boolean = runCatching {
        context.packageManager.getPackageInfo(COMPANION, 0)
        true
    }.getOrDefault(false)

    /** Whether [caller] may read the snapshot. */
    fun allowed(caller: String?): Boolean = caller == COMPANION

    /** Tells the companion the snapshot changed (at most once a minute, unless [force]d: a new Coach
     *  brief is rare and should not wait for the next heart-rate push to be noticed). */
    fun ping(context: Context, nowMs: Long = System.currentTimeMillis(), force: Boolean = false) {
        if (!force && nowMs - lastPingMs < PING_GAP_MS) return
        lastPingMs = nowMs
        runCatching { context.sendBroadcast(Intent(ACTION_CHANGED).setPackage(COMPANION)) }
    }
}

/** Read-only, single-row view of the widget snapshot for [WidgetShare.COMPANION]. */
class WidgetShareProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (!WidgetShare.allowed(callingPackage)) return null
        val context = context ?: return null
        val (brief, briefAt) = WidgetShare.brief(context)
        return MatrixCursor(WidgetShare.COLUMNS, 1).apply {
            addRow(WidgetShare.row(WidgetSnapshotStore.load(context), brief, briefAt))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
