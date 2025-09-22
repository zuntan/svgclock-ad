package net.zuntan.svgclock_ad

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SandboxInstTest {
    @Test
    fun t01_sandbox()
    {
        Log.d( "TEST", "test" )

        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val src = java.io.InputStreamReader( appContext.resources.openRawResource(R.raw.clock_theme_1 ) )
        val sink = java.io.StringWriter()

        // src.transferTo( sink )

        val buf = CharArray( 1024 )

        while( true )
        {
            val l = src.read( buf )
            if( l == -1 ) { break }
            sink.write( buf, 0, l )
        }

        val ii = ImageInfo( sink.toString() )

        Logcat.d( ii.srcCenterCircle )
        Logcat.d( ii.srcConfig )
        Logcat.d( "%s",ii.sz )
        Logcat.d( "%s",ii.vboxXY )
        Logcat.d( "%s",ii.vboxWH )
        Logcat.d( "%s",ii.config )
    }
}