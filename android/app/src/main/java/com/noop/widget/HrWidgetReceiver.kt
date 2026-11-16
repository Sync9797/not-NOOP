package com.noop.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Manifest entry point for the heart-rate widget — all rendering lives in [HrGlanceWidget]. */
class HrWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HrGlanceWidget()

    /** First widget placed: start the refresh that keeps it drawn while the strap is away
     *  ([HomeWidgetRefresh]); it retires itself once no main, compact or heart-rate widget is left. */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        HomeWidgetRefresh.ensureScheduled(context)
    }

    /** Also on every APPWIDGET_UPDATE, which Android sends after an app update, so a widget placed
     *  before this existed is covered without opening NOOP. KEEP makes repeats free. */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        HomeWidgetRefresh.ensureScheduled(context)
    }
}
