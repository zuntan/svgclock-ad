package net.zuntan.svgclock_ad

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.view.View
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.fragment.app.FragmentContainerView
import androidx.preference.Preference
import androidx.preference.SwitchPreference
import com.caverock.androidsvg.SVG


class MainActivity : AppCompatActivity() {

    companion object {
        const val CLOCKVIEW_SIZE_DP: Int = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.baseLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        updateOrientation(getResources().configuration.orientation)

        val cv = findViewById<View>(R.id.clockView)
        val l2nd = findViewById<LinearLayout>(R.id.layout2nd)

        cv.setOnClickListener {
            if (l2nd.isGone) {
                l2nd?.visibility = View.VISIBLE
                updateOrientation(resources.configuration.orientation)
            } else {
                l2nd?.visibility = View.GONE
                cv.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                cv.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT

                Toast.makeText(
                    this,
                    R.string.restore_clock_size,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        cv.setOnLongClickListener {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val provider = ComponentName(this, AppWidget::class.java)

            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val successCallback = null
                appWidgetManager.requestPinAppWidget(provider, null, successCallback)
            } else {
                // Toast.makeText(this, "Auto-adding widgets is not supported.", Toast.LENGTH_LONG).show()
            }

            true
        }

        findViewById<Switch>(R.id.swService)?.apply {
            val app = applicationContext as AppApplication
            isChecked = app.serviceOn

            setOnCheckedChangeListener { _, isToggled ->
                val serviceIntent = Intent( application, AppService::class.java)

                if (isToggled) {
                    startForegroundService( serviceIntent)
                    // startService(serviceIntent)
                } else {
                    stopService(serviceIntent)
                }
            }
        }

        val that = this

        findViewById<FragmentContainerView>( R.id.fragmentContainerView ).apply {
            getFragment<SettingsFragment>().apply {
                listener = Preference.OnPreferenceChangeListener { preference, newValue ->

                    Logcat.d( "K:%s V:%s", preference.key, newValue )

                    if( preference.key == "confCustomThemeLocation" )
                    {
                        var ok = false

                        val uri = newValue.toString().toUri()
                        val contentResolver = requireContext().contentResolver
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                        contentResolver.takePersistableUriPermission(uri, flags)

                        try {
                            contentResolver.openInputStream( uri ).use { inp ->
                                SVG.getFromInputStream( inp )
                                ok = true
                            }
                        }
                        catch ( e: Exception )
                        {
                            Logcat.d( e )
                        }

                        Logcat.d( "OK:${ok}" )

                        if( !ok )
                        {
                            Toast.makeText(
                                that,
                                "The specified file is not an svg file.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else {
                            /*
                            context?.apply {
                                val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this )
                                sharedPreferences.edit().putBoolean( "confEnableCustomTheme", true ).apply()
                            }*/

                            findViewById<FragmentContainerView>( R.id.fragmentContainerView ).apply {
                                getFragment<SettingsFragment>().apply {
                                    findPreference<SwitchPreference>("confEnableCustomTheme" )?.isChecked = true
                                }
                            }
                        }

                        ok
                    }
                    else {
                        true
                    }
                }
            }
        }

        checkNotificationPermission()
    }

    private fun checkNotificationPermission() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permCheckOk = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!permCheckOk) {
            val requestPermissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                    if (isGranted) {
                        Toast.makeText(
                            this,
                            R.string.notification_enabled,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            R.string.notification_enable_error,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation(newConfig.orientation)
    }

    fun updateOrientation(orientation: Int) {
        val bl = findViewById<LinearLayout>(R.id.baseLayout)
        val cv = findViewById<ClockView>(R.id.clockView)

        when (orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                bl?.orientation = LinearLayout.VERTICAL
                cv?.layoutParams?.apply {
                    width = LinearLayout.LayoutParams.MATCH_PARENT
                    height = (CLOCKVIEW_SIZE_DP * resources.displayMetrics.density).toInt()
                }
            }

            Configuration.ORIENTATION_LANDSCAPE -> {
                bl?.orientation = LinearLayout.HORIZONTAL
                cv?.layoutParams?.apply {
                    width = (CLOCKVIEW_SIZE_DP * resources.displayMetrics.density).toInt()
                    height = LinearLayout.LayoutParams.MATCH_PARENT
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Logcat.d( "onResume" )
/*
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
 */
    }
}