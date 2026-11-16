package com.noop.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noop.NoopApplication
import com.noop.analytics.RestScorer
import com.noop.ui.logicalDayKeyNow
import com.noop.ui.widgetAnchorRow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Periodic refresh for the main, compact and heart-rate widgets while the strap is NOT streaming
 * (Not-Noop; reported upstream as ryanbr/noop#2807, "widget is empty after the strap loses connection").
 *
 * The same gap [StressWidgetRefresh] closed for the stress widget (#2185), left open for the others.
 * Their XMLs carry `updatePeriodMillis="0"`, so Android never rebuilds them itself, and their only
 * producers are the connection service, whose push rides the live link, and the view model, which only
 * runs with the app open. A strap out of range, charging or dropped meant no push at all: the scores
 * froze across the 04:00 day rollover, and when the launcher re-created the widget in that window it
 * stayed on the empty initial layout with nothing ever asking Glance to draw it again.
 *
 * Every [INTERVAL_MINUTES] this recomputes today's three scores from banked rows (the same
 * [widgetAnchorRow] / [RestScorer] path both producers use), keeps the last-known battery and heart
 * rate (dimmed, via the store's own carry-over) and the "last seen" time, marks the strap disconnected,
 * and redraws every placed widget. No Bluetooth is touched. While the service is streaming it does
 * nothing: [shouldRefresh] reads the freshness of the last push, and the service pushes far more often
 * than [LIVE_PUSH_MAX_AGE_MS].
 */
object HomeWidgetRefresh {

    private const val WORK = "noop.homeWidgetRefresh"

    /** WorkManager's floor, and the stress widget's cadence. */
    internal const val INTERVAL_MINUTES = 15L

    /** A connected snapshot younger than this means the service is alive and pushing: PushGate re-admits
     *  an unchanged snapshot at least once a minute while heart rate streams. */
    internal const val LIVE_PUSH_MAX_AGE_MS = 5 * 60_000L

    /** Pure: refresh unless the stored snapshot shows a live, recently-pushing connection. */
    internal fun shouldRefresh(storedConnected: Boolean, storedUpdatedAtMs: Long, nowMs: Long): Boolean =
        !(storedConnected && nowMs - storedUpdatedAtMs in 0 until LIVE_PUSH_MAX_AGE_MS)

    /** Start the periodic refresh. KEEP, so repeated calls (app start, every widget update) are no-ops. */
    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<HomeWidgetRefreshWorker>(INTERVAL_MINUTES, TimeUnit.MINUTES)
            .build()
        runCatching {
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    fun cancel(context: Context) {
        runCatching { WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK) }
    }

    /** How many main, compact and heart-rate widgets are placed, or null when the host did not answer. */
    internal suspend fun placedCount(context: Context): Int? = runCatching {
        val m = GlanceAppWidgetManager(context)
        m.getGlanceIds(NoopGlanceWidget::class.java).size +
            m.getGlanceIds(NoopCompactGlanceWidget::class.java).size +
            m.getGlanceIds(HrGlanceWidget::class.java).size
    }.getOrNull()

    /** Redraw every placed main / compact / heart-rate widget from the stored snapshot. */
    internal suspend fun redrawAll(context: Context) {
        val app = context.applicationContext
        runCatching { NoopGlanceWidget().updateAll(app) }
        runCatching { NoopCompactGlanceWidget().updateAll(app) }
        runCatching { HrGlanceWidget().updateAll(app) }
    }
}

class HomeWidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        val placed = HomeWidgetRefresh.placedCount(context)
        // A settled "none placed" retires the schedule; an unanswered check keeps it (a transient host
        // failure must not cancel the safety net, the same rule StressWidgetRefresh follows).
        if (placed == 0) {
            HomeWidgetRefresh.cancel(context)
            return Result.success()
        }
        val stored = runCatching { WidgetSnapshotStore.load(context) }.getOrDefault(WidgetSnapshot())
        val now = System.currentTimeMillis()
        if (!HomeWidgetRefresh.shouldRefresh(stored.connected, stored.updatedAtMs, now)) {
            return Result.success()
        }
        val app = context as? NoopApplication
        val anchor = app?.let {
            runCatching {
                val days = it.repository.recentDaysMergedFlow(it.activeDeviceId).first()
                widgetAnchorRow(days, logicalDayKeyNow(), java.time.LocalDate.now().toString())
            }.getOrNull()
        }
        // Scores only replace what is stored when the read succeeded; a failed read redraws as-is.
        val refreshed = if (app != null && anchor != null) {
            stored.copy(
                recoveryPct = anchor.recovery?.roundToInt(),
                restPct = RestScorer.restFromDaily(anchor)?.roundToInt(),
                effortPct = anchor.strain?.roundToInt(),
            )
        } else {
            stored
        }
        runCatching {
            WidgetSnapshotStore.save(
                context,
                refreshed.copy(
                    // Null HR leaves the last reading in place, marked as a carry-over (dimmed).
                    heartRate = null,
                    connected = false,
                    // Says nothing about stress, so the stored curve is left alone.
                    stressSeries = emptyList(),
                    stressDay = null,
                    // "Last seen" stays when the strap was last heard from, not when this ran.
                    updatedAtMs = stored.updatedAtMs.takeIf { it > 0L } ?: now,
                ),
            )
        }
        HomeWidgetRefresh.redrawAll(context)
        return Result.success()
    }
}
