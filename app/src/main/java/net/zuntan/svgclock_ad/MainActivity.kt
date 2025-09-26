package net.zuntan.svgclock_ad

import android.Manifest
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone

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
        /*
        val fcv = findViewById<FragmentContainerView>( R.id.fragmentContainerView )
        val sf = fcv.getFragment<SettingsFragment>()
        sf.listener = Preference.OnPreferenceChangeListener { preference, newValue ->
            Logcat.d( "K:%s V:%s", preference.key, newValue )
            true
        }
        val pref = PreferenceManager.getDefaultSharedPreferences( this )
        pref.registerOnSharedPreferenceChangeListener( this )
        */

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

        findViewById<Switch>(R.id.swService)?.apply {
            val app = applicationContext as AppApplication
            isChecked = app.serviceOn

            setOnCheckedChangeListener { _, isToggled ->
                val serviceIntent = Intent(this.context, AppService::class.java)

                if (isToggled) {
                    startService(serviceIntent)
                } else {
                    stopService(serviceIntent)
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
}