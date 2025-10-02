package net.zuntan.svgclock_ad

import android.app.Application
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.SystemClock
import androidx.preference.PreferenceManager
import kotlin.synchronized

interface ThemeChangedHandler {
    fun onThemeChanged()
}

class AppApplication : Application(), SharedPreferences.OnSharedPreferenceChangeListener {

    private var imageInfo: ImageInfo? = null
    private val imageUpdate = HashMap<Int, Long>()

    var onThemeChanged: ThemeChangedHandler? = null

    var serviceOn: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Logcat.d("onCreate")

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        setupBySharedPreference(PreferenceManager.getDefaultSharedPreferences(this), null)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        setupBySharedPreference(sharedPreferences, key)
    }

    fun setupBySharedPreference(sharedPreferences: SharedPreferences?, key: String?) {

        synchronized(this)
        {
            Logcat.d("setupBySharedPreference")

            sharedPreferences?.let {

                if (listOf(
                        "confPresetTheme",
                        "confEnableCustomTheme",
                        "confCustomThemeLocation",
                        null
                    ).any { it -> it == key }
                ) {
                    val cect = sharedPreferences.getBoolean("confEnableCustomTheme", false)

                    if (cect) {

                    } else {
                        val cpt = sharedPreferences.getString("confPresetTheme", null)
                        var theme = LIST_PRESET_THEME.find { it -> it.second == cpt }

                        if (theme == null) {
                            theme = LIST_PRESET_THEME.first()
                        }

                        imageInfo = ImageInfo(resources.openRawResource(theme.third))
                    }
                }

                imageInfo?.apply {
                    showSecond = it.getBoolean("confShowSecond", true)
                    enableSubSecond = it.getBoolean("confEnableSubSecond", false)
                    enableSecondSmoothly = it.getBoolean("confEnableSecondSmoothly", true)

                    onThemeChanged?.onThemeChanged()
                }
            }
        }
    }

    fun haveSecondHandle(): Boolean = ( imageInfo?.srcSecondHandle != null )
    fun haveSubSecond(): Boolean = ( imageInfo?.srcSubsecondBase != null && imageInfo?.srcSubsecondHandle != null )

    private fun getRoundClock( ms: Long ): Long
    {
        return ( ms / 500 ) * 500
    }

    fun checkUpdate(appWidgetId: Int): Boolean {

        synchronized(this)
        {
            imageInfo?.run {
                if (enableSecondSmoothly) {
                    return true
                }
            }

            val tp = imageUpdate.get(appWidgetId)
            if (tp == null) return true

            if( getRoundClock( SystemClock.uptimeMillis() ) != getRoundClock(tp ) ) return true
        }

        return false
    }

    fun drawTo(canvas: Canvas, appWidgetId: Int) {
        synchronized(this)
        {
            if( imageInfo == null ) {
                canvas.drawBitmap( BitmapFactory.decodeResource( this.resources, R.drawable.logo128_gray ), 0f, 0f, null )
            }
            else {
                imageInfo?.drawTo(canvas)
            }

            imageUpdate.put(appWidgetId, getRoundClock( SystemClock.uptimeMillis() ) )
        }
    }
}