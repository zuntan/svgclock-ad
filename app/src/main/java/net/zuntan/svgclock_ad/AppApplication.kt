package net.zuntan.svgclock_ad

import android.app.Application
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.SystemClock
import androidx.core.net.toUri
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
                        SettingsFragment.CONF_PRESET_THEME,
                        SettingsFragment.CONF_ENABLE_CUSTOM_THEME,
                        SettingsFragment.CONF_CUSTOM_THEME_LOCATION,
                        null
                    ).any { it -> it == key }
                ) {
                    var done = false

                    val cect = sharedPreferences.getBoolean(SettingsFragment.CONF_ENABLE_CUSTOM_THEME, false)

                    if (cect) {

                        try {
                            val uri = sharedPreferences.getString(SettingsFragment.CONF_CUSTOM_THEME_LOCATION, null )?.toUri()

                            val contentResolver = this.contentResolver
                            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                            uri?.let {
                                contentResolver.takePersistableUriPermission(it, flags)
                                contentResolver.openInputStream(it).use { inp ->
                                    inp?.let {
                                        imageInfo = ImageInfo(it )
                                        done = true
                                    }
                                }
                            }
                        }
                        catch ( e: Exception )
                        {
                            Logcat.d( e )
                        }
                    }

                    if( !done ) {
                        val cpt = sharedPreferences.getString(SettingsFragment.CONF_PRESET_THEME, null)
                        var theme = LIST_PRESET_THEME.find { it -> it.second == cpt }

                        if (theme == null) {
                            theme = LIST_PRESET_THEME.first()
                        }

                        imageInfo = ImageInfo(assets.open(theme.third))
                    }
                }

                imageInfo?.apply {
                    showSecond = it.getBoolean(SettingsFragment.CONF_SHOW_SECOND, true)
                    enableSubSecond = it.getBoolean(SettingsFragment.CONF_ENABLE_SUBSECOND, false)
                    enableSecondSmoothly = it.getBoolean(SettingsFragment.CONF_ENABLE_SECOND_SMOOTHLY, true)

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