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
    )

    /** One row of [COLUMNS] for [snap]; -1 marks a value NOOP has not got yet. Pure, for tests. */
    fun row(snap: WidgetSnapshot): Array<Any> = arrayOf(
        snap.recoveryPct ?: -1,
        snap.restPct ?: -1,
        snap.effortPct ?: -1,
        snap.heartRate ?: -1,
        if (snap.heartRateStale) 1 else 0,
        snap.batteryPct ?: -1,
        if (snap.connected) 1 else 0,
        snap.updatedAtMs,
        HrTrace.encode(snap.hrSeries),
    )

    /** Whether [caller] may read the snapshot. */
    fun allowed(caller: String?): Boolean = caller == COMPANION

    /** Tells the companion the snapshot changed (at most once a minute). */
    fun ping(context: Context, nowMs: Long = System.currentTimeMillis()) {
        if (nowMs - lastPingMs < PING_GAP_MS) return
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
        return MatrixCursor(WidgetShare.COLUMNS, 1).apply { addRow(WidgetShare.row(WidgetSnapshotStore.load(context))) }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
