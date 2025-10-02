package net.zuntan.svgclock_ad

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import android.animation.TimeAnimator
import android.content.Intent
import java.time.LocalDateTime

import android.content.SharedPreferences
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.caverock.androidsvg.SVG
import kotlin.let

/**
 */
class ClockView : View, SharedPreferences.OnSharedPreferenceChangeListener {

    private var stopMovement: Boolean = true
    private var pTime: Long = 0
    private var dTime: Long = 0
    private var imageInfo: ImageInfo? = null

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.DKGRAY
    }

    private val pHour = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.CYAN
        strokeWidth = 15.0f
    }

    private val pMin = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.GREEN
        strokeWidth = 7.0f
    }

    private val pSec = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.RED
        strokeWidth = 3.0f
    }

    private val ani = TimeAnimator().apply {
        setDuration(1000)
        repeatCount = TimeAnimator.INFINITE
        setTimeListener { animation, totalTime, deltaTime ->
            this@ClockView.onTimeUpdate(
                totalTime
            )
        }
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

        setupBySharedPreference(PreferenceManager.getDefaultSharedPreferences(this.context), null)

        dTime = 125
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pTime = 0
        ani.start()
        PreferenceManager.getDefaultSharedPreferences(this.context)
            .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ani.pause()
        PreferenceManager.getDefaultSharedPreferences(this.context)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        setupBySharedPreference(sharedPreferences, key)
    }

    fun setupBySharedPreference(sharedPreferences: SharedPreferences?, key: String?) {
        stopMovement = true

        sharedPreferences?.let {

            if (listOf(
                    "confPresetTheme",
                    "confEnableCustomTheme",
                    "confCustomThemeLocation",
                    null
                ).any { it -> it == key }
            ) {
                var done = false

                val cect = sharedPreferences.getBoolean("confEnableCustomTheme", false)

                if (cect) {

                    try {
                        val uri = sharedPreferences.getString("confCustomThemeLocation", null )?.toUri()

                        val contentResolver = this.context.contentResolver
                        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                        uri?.let { it ->
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
                        sharedPreferences.edit().putBoolean( "confEnableCustomTheme", false ).apply()
                        Logcat.d( e )
                    }
                }

                if( !done ) {
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
            }
        }

        stopMovement = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun onTimeUpdate(
        totalTime: Long
    ) {
        val d = totalTime - pTime

        if (d >= dTime) {
            pTime = totalTime

            if (!stopMovement) {
                invalidate()
            }
        }
    }

    private fun onDrawB(canvas: Canvas) {
        imageInfo?.drawTo(canvas)
    }

    @Suppress("unused")
    private fun onDrawA(canvas: Canvas) {

        val t = LocalDateTime.now().toLocalTime()
        val s = t.toSecondOfDay().toFloat()

        val hHour = s / (12f * 60f * 60f) * 360f
        val hMin = s / (60f * 60f) * 360f

        val sd: Float = if (false) {
            val ns1 = t.toNanoOfDay()
            val ns0 = t.toSecondOfDay().toLong() * 1_000_000_000
            ((ns1 - ns0).toDouble() / 1_000_000_000).toFloat()
        } else {
            0.0f
        }

        val hSec = (((t.second).toFloat() + sd) / 60) * 360.0f

        val cw = width - paddingLeft - paddingRight
        val ch = height - paddingTop - paddingBottom
        val csz = min(cw, ch).toFloat()

        val x0 = width.toFloat() / 2
        val y0 = height.toFloat() / 2

        canvas.drawCircle(x0, y0, csz / 2, p)

        canvas.save()
        canvas.translate(x0, y0)
        canvas.rotate(hHour)
        canvas.drawLine(0.0f, 0.0f, 0.0f, -y0 * .5f, pHour)
        canvas.restore()

        canvas.save()
        canvas.translate(x0, y0)
        canvas.rotate(hMin)
        canvas.drawLine(0.0f, 0.0f, 0.0f, -y0 * .9f, pMin)
        canvas.restore()

        canvas.save()
        canvas.translate(x0, y0)
        canvas.rotate(hSec)
        canvas.drawLine(0.0f, 0.0f, 0.0f, -y0, pSec)
        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        onDrawB(canvas)
    }
}