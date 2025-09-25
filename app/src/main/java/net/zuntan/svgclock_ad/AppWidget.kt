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
import android.util.Log
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import kotlin.math.min

/**
 * Implementation of App Widget functionality.
 */
class AppWidget : AppWidgetProvider() {

    override fun onReceive(context: Context,  intent:Intent )
    {
        Logcat.d( "onReceive" )
        super.onReceive(context, intent)

        if( intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE && intent.getExtras() == null )
        {
            val appWidgetManager = AppWidgetManager.getInstance(context )
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName( context, AppWidget::class.java )
            )

            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        // val myApplication = context.applicationContext as MyApplication
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds)
        {
            updateAppWidget(context, appWidgetManager, appWidgetId)
            updateAppWidgetClick(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        Logcat.d( "onDeleted" )
        super.onDeleted(context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        Logcat.d( "onEnabled" )
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        Logcat.d( "onDisabled" )
        super.onDisabled(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        Logcat.d( "onAppWidgetOptionsChanged" )
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions )
    }
}

internal fun updateAppWidget(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    val opt = appWidgetManager.getAppWidgetOptions( appWidgetId )

    val minWidth = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
    val maxWidth = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
    val minHeight = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
    val maxHeight = opt.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

    val metrics = DisplayMetrics()
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    windowManager.defaultDisplay.getMetrics( metrics )

    val pxWidth = minWidth * metrics.density
    val pxHeight = minHeight * metrics.density
    val pxmWidth = maxWidth * metrics.density
    val pxmHeight = maxHeight * metrics.density

    val pxsz = min( pxWidth, pxHeight )
    val pxmsz = min( pxmWidth, pxmHeight )

    val p = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    val p1 = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3.0f
        isAntiAlias = true
    }

    val p2 = Paint().apply {
        color = Color.YELLOW
        textSize = 40f
        isAntiAlias = true
    }

    val bitmap = Bitmap.createBitmap(pxWidth.toInt(), pxHeight.toInt(), Bitmap.Config.ARGB_8888)

    val canvas = Canvas(bitmap)

    canvas.drawCircle( pxWidth/2, pxHeight/2, pxsz / 2, p  )
    canvas.drawText( currentTime, pxWidth/2, pxHeight/2, p2 )
    canvas.drawRect( 2f, 2f, pxWidth - 1 , pxHeight - 1, p1 )

    val views = RemoteViews(context.packageName, R.layout.app_widget)

    // views.setTextViewText(R.id.appwidget_text, currentTime)

    views.setImageViewBitmap( R.id.appwidget_image, bitmap )

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal fun updateAppWidgetClick(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetId: Int
) {
    val intent = Intent(context, MainActivity::class.java)

    // PendingIntentを作成
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