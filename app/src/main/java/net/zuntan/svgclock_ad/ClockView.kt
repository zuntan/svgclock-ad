package net.zuntan.svgclock_ad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import android.animation.TimeAnimator
import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDateTime

import com.caverock.androidsvg.SVG

/**
 * TODO: document your custom view class.
 */
class ClockView : View {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.DKGRAY
    }

    private val pHour = Paint( Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.CYAN
        strokeWidth = 15.0f
    }

    private val pMin = Paint( Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
        strokeWidth = 7.0f
    }

    private val pSec = Paint( Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
        strokeWidth = 3.0f
    }

    private val ani = TimeAnimator().apply {
        setDuration( 1000 )
        setRepeatCount( TimeAnimator.INFINITE )
        setTimeListener { animation, totalTime, deltaTime ->
            this@ClockView.onTimeUpdate(
                totalTime
            )
        }
    }

    private var pTime : Long = 0
    private var dTime : Long = 0

    private var imageInfo : ImageInfo? = null

    private fun setupSource( inp : java.io.InputStream )
    {
        val inp = java.io.InputStreamReader( inp )
        val tmp = java.io.StringWriter()

        var buf = CharArray( 1024 )

        while(true)
        {
            val l = inp.read( buf )
            if( l == -1 ) { break }
            tmp.write( buf, 0, l )
        }

        imageInfo = ImageInfo( tmp.toString() )
    }

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.ClockView, defStyle, 0
        )

        a.recycle()

        setupSource( resources.openRawResource( R.raw.clock_theme_7 ) )

        dTime = 50
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pTime = 0
        ani.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ani.pause()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // setMeasuredDimension(160, 160 )
    }

    fun onTimeUpdate(
        totalTime: Long
    ) {
        val d = totalTime - pTime

        if( d >= dTime )
        {
            pTime = totalTime
            Logcat.d( "t[%d]:d[%d]", totalTime, d )
            invalidate()
        }
    }

    private fun onDrawB(canvas: Canvas) {
        if( imageInfo == null || imageInfo!!.svgBase == null ) return

        val cl = paddingLeft
        val ct = paddingTop
        val cr = width - paddingRight
        val cb = height - paddingBottom

        val cw = cr - cl
        val ch = cb - ct

        val dw = imageInfo!!.sz!!.x
        val dh = imageInfo!!.sz!!.y

        val zw = cw.toFloat() / dw.toFloat()
        val zh = ch.toFloat() / dh.toFloat()

        val z = min(zw, zh)

        val ddw = dw * z
        val ddh = dh * z
        val ddl = (cw - ddw) / 2
        val ddt = (ch - ddh) / 2

        val vp = android.graphics.RectF(ddl, ddt, ddl + ddw, ddt + ddh)

        val bcx =  ddl + ddw * ( imageInfo!!.baseCenter.x / ( imageInfo!!.vboxWH!!.x - imageInfo!!.vboxXY!!.x ) )
        val bcy =  ddt + ddh * ( imageInfo!!.baseCenter.y / ( imageInfo!!.vboxWH!!.y - imageInfo!!.vboxXY!!.y ) )
        val scx =  ddl + ddw * ( imageInfo!!.subsecondCenter.x / ( imageInfo!!.vboxWH!!.x - imageInfo!!.vboxXY!!.x ) )
        val scy =  ddt + ddh * ( imageInfo!!.subsecondCenter.y / ( imageInfo!!.vboxWH!!.y - imageInfo!!.vboxXY!!.y ) )

        Logcat.d("%s", vp)

        val t = LocalDateTime.now().toLocalTime()
        val s = t.toSecondOfDay().toFloat()

        val hHour = s / ( 12f * 60f * 60f ) * 360f
        val hMin  = s / ( 60f * 60f ) * 360f

        val ms = ( t.toNanoOfDay() / 1_000_000 ).toFloat()

        val sd : Float = if( true ) { ( ms / 1000.0f ) % 1 } else { 0.0f }

        val hSec =  ( ( ( t.second ).toFloat() + sd ) / 60 ) * 360.0f

        imageInfo!!.svgBase!!.run {
            documentWidth = vp.width()
            documentHeight = vp.height()
            renderToCanvas(canvas, vp)
        }

        imageInfo!!.svgLongHandle!!.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()

            canvas.translate( bcx, bcy )
            canvas.rotate( hMin )
            canvas.translate( -bcx, -bcy )

            renderToCanvas(canvas, vp)

            canvas.restore()
        }

        imageInfo!!.svgShortHandle!!.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()

            canvas.translate( bcx, bcy )
            canvas.rotate( hHour )
            canvas.translate( -bcx, -bcy )

            renderToCanvas(canvas, vp)

            canvas.restore()
        }

        imageInfo!!.svgSecondHandle!!.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()

            canvas.translate( bcx, bcy )
            canvas.rotate( hSec )
            canvas.translate( -bcx, -bcy )

            renderToCanvas(canvas, vp)

            canvas.restore()
        }

        imageInfo!!.svgCenterCircle!!.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()
            renderToCanvas(canvas, vp)
            canvas.restore()
        }
    }

    private fun onDrawA(canvas: Canvas) {
        val t = LocalDateTime.now().toLocalTime()
        val s = t.toSecondOfDay().toFloat()

        val hHour = s / ( 12f * 60f * 60f ) * 360f
        val hMin  = s / ( 60f * 60f ) * 360f

        val ms = ( t.toNanoOfDay() / 1_000_000 ).toFloat()

        val sd : Float = if( false ) { ( ms / 1000.0f ) % 1 } else { 0.0f }

        val hSec =  ( ( ( t.second ).toFloat() + sd ) / 60 ) * 360.0f

        val cw = width - paddingLeft - paddingRight
        val ch = height - paddingTop - paddingBottom
        val csz = min( cw, ch ).toFloat()

        val x0 = width.toFloat() / 2
        val y0 = height.toFloat() / 2

        canvas.drawCircle( x0, y0, csz / 2, p )

        canvas.save()
        canvas.translate( x0, y0 )
        canvas.rotate( hHour )
        canvas.drawLine( 0.0f, 0.0f, 0.0f, -y0 * .5f, pHour )
        canvas.restore()

        canvas.save()
        canvas.translate( x0, y0 )
        canvas.rotate( hMin )
        canvas.drawLine( 0.0f, 0.0f, 0.0f, -y0 * .9f, pMin )
        canvas.restore()

        canvas.save()
        canvas.translate( x0, y0 )
        canvas.rotate( hSec )
        canvas.drawLine( 0.0f, 0.0f, 0.0f, -y0, pSec )
        canvas.restore()
    }

    @SuppressLint("DrawAllocation", "UseKtx")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        onDrawB(canvas)
        //onDrawA(canvas)
    }
}