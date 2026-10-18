package com.drs.nexus.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.drs.nexus.app.MainActivity
import com.drs.nexus.app.NexusApplication
import com.drs.nexus.app.R
import com.drs.nexus.core.tools.ToolRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Real home-screen AppWidget: shows live usage stats straight from the Room database
 * (total runs, favorites count, most recent tool) and opens the app on tap.
 * Data is read off the main thread via runBlocking on Dispatchers.IO inside
 * the provider callback budget (small indexed queries, sub-millisecond on device).
 */
class NexusWidgetProvider : AppWidgetProvider() {

    data class WidgetData(val totalRuns: Int, val favorites: Int, val lastToolName: String)

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val data = loadData(context)
        for (id in ids) {
            manager.updateAppWidget(id, buildViews(context, data))
        }
    }

    private fun loadData(context: Context): WidgetData {
        val app = context.applicationContext as? NexusApplication
        return if (app == null) {
            WidgetData(0, 0, "")
        } else {
            runBlocking(Dispatchers.IO) {
                val repo = app.container.toolRepository
                val runs = repo.allRunsOnce()
                val favs = repo.favoritesOnce()
                val last = runs.firstOrNull()?.toolId
                WidgetData(
                    totalRuns = runs.size,
                    favorites = favs.size,
                    lastToolName = last?.let { toolId ->
                        ToolRegistry.byId(toolId)?.nameKey?.let { key -> nameForKey(context, key) }
                    } ?: ""
                )
            }
        }
    }

    /** Maps a registry nameKey to the localized string resource (device locale applies). */
    private fun nameForKey(context: Context, nameKey: String): String {
        val res = context.resources
        val id = res.getIdentifier(nameKey, "string", context.packageName)
        return if (id != 0) context.getString(id) else nameKey
    }

    private fun buildViews(context: Context, data: WidgetData): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_nexus)
        views.setTextViewText(R.id.widget_runs, data.totalRuns.toString())
        views.setTextViewText(R.id.widget_favorites, data.favorites.toString())
        views.setTextViewText(
            R.id.widget_last,
            if (data.lastToolName.isEmpty()) context.getString(R.string.widget_no_runs)
            else context.getString(R.string.widget_last_used, data.lastToolName)
        )
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pending)
        return views
    }

    companion object {
        /** Re-renders every placed widget with fresh Room data. */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, NexusWidgetProvider::class.java))
            if (ids.isEmpty()) return
            NexusWidgetProvider().onUpdate(context, manager, ids)
        }
    }
}
