package net.zuntan.svgclock_ad

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.LinearLayout
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    companion object {
        const val CLOCKVIEW_SIZE_DP : Int = 200
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

        updateOrientation( getResources().getConfiguration().orientation )

        val cv = findViewById<View>( R.id.clockView )
        val l2nd = findViewById<LinearLayout>( R.id.layout2nd )

        cv?.apply {
            setOnClickListener {
                if( l2nd.visibility == View.GONE ) {
                    l2nd?.visibility = View.VISIBLE
                    updateOrientation( getResources().getConfiguration().orientation )
                }
                else {
                    l2nd?.visibility = View.GONE
                    cv.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                    cv.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                }
            }
        }

        findViewById<Switch>( R.id.swService )?.apply {
            val app = applicationContext as AppApplication
            isChecked = app.serviceOn

            setOnCheckedChangeListener { _, isToggled ->
                val serviceIntent = Intent(this.context, AppService::class.java)

                if( isToggled ) {
                    startService(serviceIntent)
                }
                else {
                    stopService(serviceIntent)
                }
            }
        }
   }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateOrientation( newConfig.orientation )
    }

    fun updateOrientation( orientation : Int )
    {
        val bl = findViewById<LinearLayout>( R.id.baseLayout )
        val cv = findViewById<ClockView>( R.id.clockView )

        when( orientation )
        {
            Configuration.ORIENTATION_PORTRAIT -> {
                bl?.orientation = LinearLayout.VERTICAL
                cv?.layoutParams?.apply {
                    width =LinearLayout.LayoutParams.MATCH_PARENT
                    height = ( CLOCKVIEW_SIZE_DP * resources.displayMetrics.density ).toInt()
                }
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                bl?.orientation = LinearLayout.HORIZONTAL
                cv?.layoutParams?.apply {
                    width = ( CLOCKVIEW_SIZE_DP * resources.displayMetrics.density ).toInt()
                    height = LinearLayout.LayoutParams.MATCH_PARENT
                }
            }
        }
    }
}