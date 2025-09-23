package net.zuntan.svgclock_ad

import android.content.res.Configuration
import android.os.Bundle
import android.text.style.UpdateLayout
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

val CLOCKVIEW_SIZE_DP : Int = 200

class MainActivity : AppCompatActivity() {

    lateinit var bl : LinearLayout
    lateinit var cv : ClockView
    /*
    lateinit var fcv : FragmentContainerView
    lateinit var sf :SettingsFragment
    lateinit var pref :SharedPreferences
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bl = findViewById<LinearLayout>( R.id.baseLayout )
        cv = findViewById<ClockView>( R.id.clockView )

        /*
        fcv = findViewById<FragmentContainerView>( R.id.fragmentContainerView )
        sf = fcv.getFragment<SettingsFragment>()
        sf.listener = Preference.OnPreferenceChangeListener { preference, newValue ->
            Logcat.d( "K:%s V:%s", preference.key, newValue )
            true
        }
        pref = PreferenceManager.getDefaultSharedPreferences( this )
        pref.registerOnSharedPreferenceChangeListener( this )
        */

        updateOrientation( getResources().getConfiguration().orientation )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        /*
        Logcat.d( "%s", newConfig )
        Logcat.d( "%s", newConfig.orientation )
        */

        updateOrientation( newConfig.orientation )
    }

    fun updateOrientation( orientation : Int )
    {
        when( orientation )
        {
            Configuration.ORIENTATION_PORTRAIT -> {
                bl.orientation = LinearLayout.VERTICAL
                cv.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                cv.layoutParams.height = ( CLOCKVIEW_SIZE_DP * resources.displayMetrics.density ).toInt()
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                bl.orientation = LinearLayout.HORIZONTAL
                cv.layoutParams.width = ( CLOCKVIEW_SIZE_DP * resources.displayMetrics.density ).toInt()
                cv.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            }
        }
    }
}