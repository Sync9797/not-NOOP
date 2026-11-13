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

    val COLUMNS = arrayOf(
        "recovery", "rest", "effort", "hr", "hr_stale", "battery", "connected", "updated_at", "hr_series",
        "stress_series", "coach_brief", "coach_brief_at", "hr_day",
    )

    /** How far back [dayTrace] reaches, and the width of each averaged point in it. */
    const val DAY_SEC = 24 * 3600L
    const val DAY_BUCKET_SEC = 300L

    /**
     * The last [DAY_SEC] of heart rate from NOOP's own history, one averaged point per
     * [DAY_BUCKET_SEC], as the Today chart draws it. Unlike the snapshot's 3-hour trace, which only
     * grows while live heart rate streams, this includes everything offloaded from the strap.
     */
    suspend fun dayTrace(context: Context, nowSec: Long = System.currentTimeMillis() / 1000): List<HrPoint> {
        val app = context.applicationContext
        val active = (app as? com.noop.NoopApplication)?.activeDeviceId?.takeIf { it.isNotBlank() }
            ?: com.noop.data.WhoopRepository.WHOOP_SOURCE
        val repo = com.noop.data.WhoopRepository(com.noop.data.WhoopDatabase.get(app))
        return repo.hrBucketsUnion(active, nowSec - DAY_SEC, nowSec, DAY_BUCKET_SEC)
            .filter { it.avgBpm > 0 }
            .map { HrPoint(it.bucket, Math.round(it.avgBpm).toInt()) }
    }

    /** The Coach brief as the Coach Brief widget shows it, and when it was written (0 when there is none). */
    data class Brief(val text: String = "", val atMs: Long = 0L)

    /** The brief the Coach Brief widget reads, from the same prefs. */
    fun brief(context: Context): Brief {
        val p = context.getSharedPreferences("noop_widget", Context.MODE_PRIVATE)
        return Brief(p.getString("coachBriefText", null).orEmpty(), p.getLong("coachBriefDateMs", 0L))
    }

    /**
     * One row of [COLUMNS] for [snap] and [brief]; -1 marks a value NOOP has not got yet. The stress
     * curve is today's only ([WidgetSnapshotStore.load] drops any other day's). Pure, for tests.
     */
    fun row(snap: WidgetSnapshot, brief: Brief = Brief(), day: List<HrPoint> = emptyList()): Array<Any> = arrayOf(
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
        brief.text,
        brief.atMs,
        HrTrace.encode(day),
    )

    /** Whether [caller] may read the snapshot. */
    fun allowed(caller: String?): Boolean = caller == COMPANION

    /**
     * Tells the companion the snapshot changed: at most once a minute for heart rate, or right away
     * with [now] for the rare changes (a scored stress hour, a new brief) a minute's gap could swallow.
     */
    fun ping(context: Context, nowMs: Long = System.currentTimeMillis(), now: Boolean = false) {
        if (!now && nowMs - lastPingMs < PING_GAP_MS) return
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
        // Binder thread, so the database read may block here; a failed read shares no day trace.
        val day = runCatching { kotlinx.coroutines.runBlocking { WidgetShare.dayTrace(context) } }.getOrDefault(emptyList())
        return MatrixCursor(WidgetShare.COLUMNS, 1).apply {
            addRow(WidgetShare.row(WidgetSnapshotStore.load(context), WidgetShare.brief(context), day))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
