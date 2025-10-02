package net.zuntan.svgclock_ad

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.view.WindowManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.RemoteViews

import android.graphics.Canvas
import android.os.Build
import androidx.core.graphics.createBitmap

/**
*/
class AppWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        //Logcat.d( "onReceive" )
        super.onReceive(context, intent)

        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.extras == null) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, AppWidget::class.java)
            )

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            updateAppWidgetClick(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        Logcat.d("onDeleted")
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Logcat.d("onEnabled")
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        Logcat.d("onDisabled")
        super.onDisabled(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        Logcat.d("onAppWidgetOptionsChanged")
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }
}

@Suppress("DEPRECATION")
internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val app = context.applicationContext as AppApplication

    if (!app.checkUpdate(appWidgetId)) {
        return
    }

    val opt = appWidgetManager.getAppWidgetOptions(appWidgetId)

    val minWidth = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val minHeight = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
    //val maxWidth = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    // val maxHeight = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

    val metrics = DisplayMetrics()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics(metrics)

    val scale = 0.9f

    val pxWidth = minWidth * metrics.density * scale
    val pxHeight = minHeight * metrics.density * scale
    // val pxmWidth = maxWidth * metrics.density
    // val pxmHeight = maxHeight * metrics.density

    // val pxsz = min(pxWidth, pxHeight)
    // val pxmsz = min(pxmWidth, pxmHeight)

    val bitmap = createBitmap(pxWidth.toInt(), pxHeight.toInt())
    val canvas = Canvas(bitmap)

    app.drawTo(canvas, appWidgetId)

    val views = RemoteViews(context.packageName, R.layout.app_widget)
    views.setImageViewBitmap(R.id.appwidget_image, bitmap)

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun updateAppWidgetClick(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val intent = Intent(context, MainActivity::class.java)

    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
    )

    val views = RemoteViews(context.packageName, R.layout.app_widget)
    views.setOnClickPendingIntent(R.id.appwidget_image, pendingIntent)
    appWidgetManager.updateAppWidget(appWidgetId, views)
}