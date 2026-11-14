package com.noop.ingest

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.noop.NoopApplication
import com.noop.ui.NoopPrefs
import com.noop.ui.ProfileStore
import java.util.concurrent.TimeUnit

/**
 * OPTIONAL background Health Connect sync (Not-Noop). OFF by default.
 *
 * The app normally reads Health Connect only while it is open (the on-open catch-up in AppViewModel),
 * which is what upstream NOOP chose: it needs no extra permission, costs no battery while closed, and
 * opening the app is enough for most people. This is the opt-in for those who want Health Connect data
 * kept fresh between visits:
 *  - Android asks for its separate "read health data in the background" permission
 *    ([PERMISSION]) when the user turns it on; without that grant nothing is scheduled.
 *  - It reads only the last [WINDOW_DAYS] days ([HealthConnectImporter.importRecent]), not the full
 *    ten-year history the on-open import reads.
 *  - It runs at most every [intervalHours] hours (the user's own sync interval, floored at 1 h), only
 *    when the battery is not low, and shares the importer's lock with the in-app sync.
 * Strap data does not depend on any of this; it arrives over Bluetooth.
 */
object HealthConnectBackgroundSync {

    /** Health Connect's permission for reading while the app is in the background. */
    val PERMISSION: String = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    /** Days a background pass re-reads: enough to pick up a late-arriving night or a revised day. */
    const val WINDOW_DAYS: Int = 3

    private const val WORK = "hc-background-sync"

    /** The background interval for the user's sync-interval setting (hours, at least 1). */
    internal fun intervalHours(syncHours: Int): Long = syncHours.coerceAtLeast(1).toLong()

    /** Schedule (or reschedule with a new interval) the periodic pass. UPDATE keeps the existing timing
     *  when only the interval changes, rather than restarting the clock. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        val hours = intervalHours(NoopPrefs.hcSyncHours(app))
        val request = PeriodicWorkRequestBuilder<HealthConnectBackgroundSyncWorker>(hours, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()
        runCatching {
            WorkManager.getInstance(app).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }

    /** Stop the periodic pass. */
    fun cancel(context: Context) {
        runCatching { WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK) }
    }

    /** Whether a background pass should be scheduled: the option is on AND auto-sync (its parent) is on. */
    fun wanted(context: Context): Boolean =
        NoopPrefs.hcBackgroundSync(context) && NoopPrefs.hcAutoSync(context)

    /** Keep the schedule in step with the settings (on and scheduled, or off and gone). Called at app
     *  start and whenever either setting or the interval changes. */
    fun reconcile(context: Context) {
        if (wanted(context)) schedule(context) else cancel(context)
    }
}

/** One background pass. Re-checks everything, since the setting or a grant can change at any time. */
class HealthConnectBackgroundSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (!HealthConnectBackgroundSync.wanted(context)) {
            HealthConnectBackgroundSync.cancel(context)
            return Result.success()
        }
        if (HealthConnectImporter.sdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) return Result.success()
        val granted = runCatching {
            HealthConnectImporter.client(context).permissionController.getGrantedPermissions()
        }.getOrNull() ?: return Result.retry()
        // Background access revoked in Android settings: turn the option off rather than fail every pass.
        if (HealthConnectBackgroundSync.PERMISSION !in granted) {
            NoopPrefs.setHcBackgroundSync(context, false)
            HealthConnectBackgroundSync.cancel(context)
            return Result.success()
        }
        val repo = (context as? NoopApplication)?.repository ?: return Result.success()
        val summary = runCatching {
            HealthConnectImporter.importRecent(
                context, repo,
                heightCm = ProfileStore.from(context).heightCm,
                windowDays = HealthConnectBackgroundSync.WINDOW_DAYS,
            )
        }.getOrNull()
        if (summary?.succeeded != true) return Result.retry()
        NoopPrefs.setHcLastSync(context, System.currentTimeMillis())
        return Result.success()
    }
}
