package net.zuntan.svgclock_ad

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.IBinder
import android.util.Log
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.core.app.NotificationCompat

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.schedulers.Schedulers

import java.util.concurrent.TimeUnit

class PowerStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        val serviceIntent = Intent(context, AppService::class.java)

        when (intent?.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d("PowerStateReceiver", "充電器が接続されました")
                serviceIntent.action = AppService.ACTION_TO_STATE_UPDATE
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d("PowerStateReceiver", "充電器が切断されました")
                serviceIntent.action = AppService.ACTION_TO_STATE_UPDATE
            }

            Intent.ACTION_SCREEN_ON -> {
                Log.d("PowerStateReceiver", "ACTION_SCREEN_ON")
                serviceIntent.action = AppService.ACTION_TO_STATE_UPDATE
            }

            Intent.ACTION_SCREEN_OFF -> {
                Log.d("PowerStateReceiver", "ACTION_SCREEN_OFF")
                serviceIntent.action = AppService.ACTION_TO_STATE_UPDATE
            }
        }

        context?.startService(serviceIntent)
    }
}

class AppService : Service() {

    companion object {

        const val INTERVAL_FAST_MS = 125L
        const val INTERVAL_MIDDLE_MS = 250L
        const val INTERVAL_SLOW_MS = 10000L

        const val ACTION_TO_STATE_UPDATE = "ACTION_TO_STATE_UPDATE"

        const val NOTIFICATION_ID = 101
        const val NOTIFICATION_CHANNEL_ID = "ClockServiceForWidgetChannel"
    }

    private val disposables = CompositeDisposable()

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d("MyService", "onStartCommand %s:%s:%s".format(intent, flags, startId))

        setupInterval()

        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.notification_content_title ),
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(serviceChannel)

        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification: Notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_content_title))
            .setContentText( getString( R.string.notification_content_text ) )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private lateinit var powerStateReceiver: PowerStateReceiver

    override fun onCreate() {
        super.onCreate()

        Log.d("MyService", "onCreate")

        powerStateReceiver = PowerStateReceiver()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        registerReceiver(powerStateReceiver, filter)

        val app = applicationContext as AppApplication
        app.serviceOn = true
    }

    override fun onDestroy() {
        Log.d("MyService", "onDestroy")
        disposables.dispose()
        unregisterReceiver(powerStateReceiver)
        super.onDestroy()

        val app = applicationContext as AppApplication
        app.serviceOn = false

        /*
        val serviceIntent = Intent(this, MyService::class.java)
        startService(serviceIntent)
        */
    }

    fun setupInterval() {
        val batteryStatus: Intent? = registerReceiver(null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1

        val isPlugged: Boolean = plugged == BatteryManager.BATTERY_PLUGGED_AC

        val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager

        val isOn = displayManager.displays.any { it.state != Display.STATE_OFF }

        val period = if (isOn) {
            if (isCharging || isPlugged) {
                INTERVAL_FAST_MS
            } else {
                INTERVAL_MIDDLE_MS
            }
        } else {
            INTERVAL_SLOW_MS
        }

        Log.d("MyService", "setupInterval period:%s".format(period))

        disposables.clear()

        val intent = Intent(this, AppWidget::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Observable.interval(period, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .timeInterval(TimeUnit.MILLISECONDS)
            .subscribe {
                pendingIntent.send()
            }.addTo(disposables)
    }
}