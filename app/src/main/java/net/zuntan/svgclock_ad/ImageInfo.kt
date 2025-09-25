package net.zuntan.svgclock_ad

import android.graphics.Canvas
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlPullParser

import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableAffineTransform
import androidx.ink.geometry.AffineTransform
import androidx.ink.geometry.ImmutableAffineTransform

import kotlinx.serialization.*
import kotlinx.serialization.decodeFromString

import com.akuleshov7.ktoml.Toml

import com.caverock.androidsvg.SVG
import java.io.InputStream
import java.time.LocalDateTime
import kotlin.math.min

const val NAMESPACE_SVG = "" // ""http://www.w3.org/2000/svg"
const val NAMESPACE_INKSCAPE = "http://www.inkscape.org/namespaces/inkscape"

val RE_FLOAT = """[-+]?([0-9]*\.[0-9]+|[0-9]+\.?[0-9]*)([eE][-+]?[0-9]+)?""".toRegex()
val RE_TRAN =
    """(translate|scale|rotate|skewX|skewY|matrix)\s*\(([^)]+)\)""".toRegex(RegexOption.IGNORE_CASE)

private fun parseFloatList(value: String?): List<Float>? {

    if (value == null) return null

    return RE_FLOAT.findAll(value).let {
        val ret = ArrayList<Float>()

        for (m in it) {
            try {
                ret.add(m.value.toFloat())
            } catch (_: NumberFormatException) {
                // pass
            }
        }

        ret
    }
}

private fun parseSvgTransformValue(value: String?): AffineTransform {

    val affine = MutableAffineTransform()

    if (value != null) {

        for (m in RE_TRAN.findAll(value)) {
            val op = m.groups[1]?.value?.lowercase()
            val arg = parseFloatList(m.groups[2]?.value)

            var a: AffineTransform? = null

            if (op != null && arg != null) {
                if (op == "translate" && arg.count() == 2) {
                    a = ImmutableAffineTransform.translate(ImmutableVec(arg[0], arg[1]))
                } else if (op == "scale" && arg.count() == 2) {
                    a = ImmutableAffineTransform.scale(arg[0], arg[1])
                } else if (op == "rotate") {
                    if (arg.count() == 1) {
                        a = ImmutableAffineTransform.rotate(arg[0])
                    } else if (arg.count() == 3) {
                        a = MutableAffineTransform()
                        AffineTransform.multiply(
                            a,
                            ImmutableAffineTransform.translate(ImmutableVec(arg[1], arg[2])),
                            a
                        )
                        AffineTransform.multiply(a, ImmutableAffineTransform.rotate(arg[0]), a)
                    }
                } else if (op == "skewx" && arg.count() == 1) {
                    a = ImmutableAffineTransform.shearX(arg[0])
                } else if (op == "skewy" && arg.count() == 1) {
                    a = ImmutableAffineTransform.shearY(arg[0])
                } else if (op == "matrix" && arg.count() == 6) {
                    a = ImmutableAffineTransform(arg[0], arg[1], arg[2], arg[3], arg[4], arg[5])
                }
            }

            if (a != null) {
                AffineTransform.multiply(affine, a, affine)
            }
        }
    }

    return affine
}

class ImageInfo {

    var srcOrg: String? = null
    var srcBase: String? = null
    var srcBaseText: String? = null
    var srcLongHandle: String? = null
    var srcShortHandle: String? = null
    var srcSecondHandle: String? = null
    var srcCenterCircle: String? = null
    var srcSubsecondBase: String? = null
    var srcSubsecondHandle: String? = null
    var srcSubsecondCenterCircle: String? = null
    var srcConfig: String? = null

    var sz: ImmutableVec? = null
    var vboxXY: ImmutableVec? = null
    var vboxWH: ImmutableVec? = null

    var baseCenter: ImmutableVec? = null
    var subsecondCenter: ImmutableVec? = null

    var svgBase: SVG? = null
    var svgLongHandle: SVG? = null
    var svgShortHandle: SVG? = null
    var svgSecondHandle: SVG? = null
    var svgCenterCircle: SVG? = null
    var svgSubsecondBase: SVG? = null
    var svgSubsecondHandle: SVG? = null
    var svgSubsecondCenterCircle: SVG? = null

    var config: Config? = null

    var showSecond: Boolean = true
    var enableSubSecond: Boolean = false
    var enableSecondSmoothly: Boolean = true

    constructor(src: InputStream) {
        val src = java.io.InputStreamReader(src)
        val tmp = java.io.StringWriter()

        var buf = CharArray(1024)

        while (true) {
            val l = src.read(buf)
            if (l == -1) {
                break
            }
            tmp.write(buf, 0, l)
        }

        init(tmp.toString())
    }

    constructor(src: String) {
        init(src)
    }

    private fun init(src: String) {
        srcOrg = src
        srcBase = filterXml("base")
        srcBaseText = filterXml("base_text")
        srcLongHandle = filterXml("long_handle")
        srcShortHandle = filterXml("short_handle")
        srcSecondHandle = filterXml("second_handle")
        srcCenterCircle = filterXml("center_circle")
        srcSubsecondBase = filterXml("sub_second_base")
        srcSubsecondHandle = filterXml("sub_second_handle")
        srcSubsecondCenterCircle = filterXml("sub_second_center_circle")
        srcConfig = filterXml("config")

        parseXmlSzAndVbox().let {
            sz = it.first
            vboxXY = it.second
            vboxWH = it.third
        }

        baseCenter = parseXmlCenter(srcCenterCircle)
        subsecondCenter = parseXmlCenter(srcSubsecondCenterCircle)

        config = parseXmlConfig()

        svgBase = srcBase?.let { SVG.getFromString(it) }
        svgLongHandle = srcLongHandle?.let { SVG.getFromString(it) }
        svgShortHandle = srcShortHandle?.let { SVG.getFromString(it) }
        svgSecondHandle = srcSecondHandle?.let { SVG.getFromString(it) }
        svgCenterCircle = srcCenterCircle?.let { SVG.getFromString(it) }
        svgSubsecondBase = srcSubsecondBase?.let { SVG.getFromString(it) }
        svgSubsecondHandle = srcSubsecondHandle?.let { SVG.getFromString(it) }
        svgSubsecondCenterCircle = srcSubsecondCenterCircle?.let { SVG.getFromString(it) }
    }

    private fun parseXmlSzAndVbox(): Triple<ImmutableVec?, ImmutableVec?, ImmutableVec?> {

        val f = XmlPullParserFactory.newInstance()

        f.isNamespaceAware = true
        f.isValidating = false

        val xpp = f.newPullParser()

        xpp.setInput(java.io.StringReader(srcOrg))

        var evt: Int = xpp.getEventType()

        while (true) {
            when (evt) {
                XmlPullParser.END_DOCUMENT -> {
                    break
                }

                XmlPullParser.START_TAG -> {
                    if (xpp.name == "svg") {
                        var sz: ImmutableVec? = null

                        val width = xpp.getAttributeValue(NAMESPACE_SVG, "width")?.toFloat()
                        val height = xpp.getAttributeValue(NAMESPACE_SVG, "height")?.toFloat()

                        if (width != null && height != null) {
                            sz = ImmutableVec(width, height)
                        }

                        var vboxXY: ImmutableVec? = null
                        var vboxWH: ImmutableVec? = null

                        val viewBox =
                            parseFloatList(xpp.getAttributeValue(NAMESPACE_SVG, "viewBox"))

                        if (viewBox != null && viewBox.count() == 4) {
                            vboxXY = ImmutableVec(viewBox[0], viewBox[1])
                            vboxWH = ImmutableVec(viewBox[2], viewBox[3])
                        }

                        if (sz == null || vboxXY == null /* || vboxWH == null */) {
                            sz = null
                            vboxXY = null
                            vboxWH = null
                        }

                        return Triple(sz, vboxXY, vboxWH)
                    }
                }
            }

            evt = xpp.next()
        }

        return Triple(null, null, null)
    }

    private fun parseXmlCenter(src: String?): ImmutableVec {

        var ret = ImmutableVec(0f, 0f)

        if (src != null) {

            val f = XmlPullParserFactory.newInstance()

            f.isNamespaceAware = true
            f.isValidating = false

            val xpp = f.newPullParser()

            xpp.setInput(java.io.StringReader(src))

            var evt: Int = xpp.getEventType()

            val affineList: ArrayDeque<AffineTransform> = ArrayDeque<AffineTransform>()

            while (true) {
                when (evt) {
                    XmlPullParser.END_DOCUMENT -> {
                        break
                    }

                    XmlPullParser.START_TAG -> {
                        affineList.addLast(
                            parseSvgTransformValue(
                                xpp.getAttributeValue(
                                    NAMESPACE_SVG,
                                    "transform"
                                )
                            )
                        )

                        if (xpp.depth == 3 && (xpp.name == "ellipse" || xpp.name == "circle")) {
                            val cx = xpp.getAttributeValue(NAMESPACE_SVG, "cx")?.toFloat()
                            val cy = xpp.getAttributeValue(NAMESPACE_SVG, "cy")?.toFloat()

                            if (cx != null && cy != null) {
                                ret = ImmutableVec(cx, cy)

                                val affine = MutableAffineTransform()

                                affineList.forEach { AffineTransform.multiply(affine, it, affine) }

                                ret = affine.applyTransform(ret)
                            }

                            break;
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        affineList.removeLast()
                    }
                }

                evt = xpp.next()
            }
        }

        return ret
    }

    @Serializable
    data class Config(
        val theme_name: String? = null,
        val theme_description: String? = null,
        val with_text_time_zone: Boolean? = null,
        val with_text_date: Boolean? = null,
        val with_text_time: Boolean? = null,
        val with_text_segment: Boolean? = null,
        val enable_rotate_center_circle: Boolean? = null,
        val enable_update_region_every_time: Boolean? = null
    )

    private fun parseXmlConfig(): Config {

        var ret = Config()

        if (srcConfig != null) {
            val sink = StringBuilder()

            val f = XmlPullParserFactory.newInstance()

            f.isNamespaceAware = true
            f.isValidating = false

            val xpp = f.newPullParser()

            xpp.setInput(java.io.StringReader(srcConfig))

            var evt: Int = xpp.getEventType()

            var flg = -1

            while (true) {
                when (evt) {
                    XmlPullParser.END_DOCUMENT -> {
                        break
                    }

                    XmlPullParser.START_TAG -> {
                        if (flg == -1 && xpp.name == "text") {
                            flg = xpp.depth
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        if (flg != -1 && xpp.name == "tspan") {
                            sink.append("\n")
                        }
                        if (flg == xpp.depth) {
                            break
                        }
                    }

                    XmlPullParser.TEXT -> {
                        if (flg != -1) {
                            sink.append(xpp.text)
                        }
                    }

                    XmlPullParser.CDSECT -> {
                        if (flg != -1) {
                            sink.append(xpp.text)
                        }
                    }

                    XmlPullParser.ENTITY_REF -> {
                        if (flg != -1) {
                            sink.append(xpp.text)
                        }
                    }

                    XmlPullParser.IGNORABLE_WHITESPACE -> {
                        if (flg != -1) {
                            sink.append(xpp.text)
                        }
                    }
                }

                evt = xpp.nextToken()
            }

            try {
                ret = Toml.decodeFromString<Config>(sink.toString())
            } catch (e: Exception) {
                Logcat.e(e)
            }
        }

        return ret
    }

    private fun filterXml(layerName: String): String? {

        val sw = java.io.StringWriter()

        val f = XmlPullParserFactory.newInstance()

        f.isNamespaceAware = true
        f.isValidating = false

        val xpp = f.newPullParser()
        val xpps = f.newSerializer()

        xpp.setInput(java.io.StringReader(srcOrg))
        xpps.setOutput(sw)

        var evt: Int = xpp.getEventType()

        var found = false
        var depthDisOutput = -1

        while (true) {
            when (evt) {
                XmlPullParser.END_DOCUMENT -> {
                    xpps.endDocument()
                    break
                }

                XmlPullParser.START_DOCUMENT -> {
                    xpps.startDocument("utf8", true)
                }

                XmlPullParser.START_TAG -> {
                    if (xpp.depth == 2 && xpp.name == "g") {
                        val groupmode = xpp.getAttributeValue(NAMESPACE_INKSCAPE, "groupmode")
                        val label = xpp.getAttributeValue(NAMESPACE_INKSCAPE, "label")

                        if (groupmode == "layer" && label == layerName) {
                            found = true
                        } else {
                            depthDisOutput = xpp.depth
                        }
                    }

                    if (depthDisOutput == -1) {
                        xpps.startTag(xpp.namespace, xpp.name)

                        for (i in 0 until xpp.attributeCount) {
                            val namespace = xpp.getAttributeNamespace(i)
                            val name = xpp.getAttributeName(i)
                            val value = xpp.getAttributeValue(i)
                            xpps.attribute(namespace, name, value)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if (depthDisOutput == -1) {
                        xpps.endTag(xpps.namespace, xpps.name)
                    } else if (xpp.depth == depthDisOutput) {
                        depthDisOutput = -1
                    }
                }

                XmlPullParser.TEXT -> {
                    if (depthDisOutput == -1) xpps.text(xpp.text)
                }

                XmlPullParser.CDSECT -> {
                    if (depthDisOutput == -1) xpps.cdsect(xpp.text)
                }

                XmlPullParser.ENTITY_REF -> {
                    if (depthDisOutput == -1) xpps.entityRef(xpp.name)
                }

                XmlPullParser.PROCESSING_INSTRUCTION -> {
                    if (depthDisOutput == -1) xpps.processingInstruction(xpp.text)
                }

                XmlPullParser.IGNORABLE_WHITESPACE -> {
                    if (depthDisOutput == -1) xpps.ignorableWhitespace(xpp.text)
                }

                XmlPullParser.COMMENT -> {
                    if (depthDisOutput == -1) xpps.comment(xpp.text)
                }
            }

            evt = xpp.nextToken()
        }

        return if (found) {
            sw.toString()
        } else {
            null
        }
    }

    fun drawTo(canvas: Canvas) {
        if (svgBase == null) return

        val cw = canvas.width
        val ch = canvas.height

        val dw = sz!!.x
        val dh = sz!!.y

        val zw = cw.toFloat() / dw.toFloat()
        val zh = ch.toFloat() / dh.toFloat()

        val z = min(zw, zh)

        val ddw = dw * z
        val ddh = dh * z
        val ddl = (cw - ddw) / 2
        val ddt = (ch - ddh) / 2

        val vp = android.graphics.RectF(ddl, ddt, ddl + ddw, ddt + ddh)

        val bcx = ddl + ddw * (baseCenter!!.x / (vboxWH!!.x - vboxXY!!.x))
        val bcy = ddt + ddh * (baseCenter!!.y / (vboxWH!!.y - vboxXY!!.y))
        val scx = ddl + ddw * (subsecondCenter!!.x / (vboxWH!!.x - vboxXY!!.x))
        val scy = ddt + ddh * (subsecondCenter!!.y / (vboxWH!!.y - vboxXY!!.y))

        val t = LocalDateTime.now().toLocalTime()
        val s = t.toSecondOfDay().toFloat()

        val hHour = s / (12f * 60f * 60f) * 360f
        val hMin = s / (60f * 60f) * 360f

        val sd: Float = if (enableSecondSmoothly) {
            val ns1 = t.toNanoOfDay()
            val ns0 = t.toSecondOfDay().toLong() * 1_000_000_000
            ((ns1 - ns0).toDouble() / 1_000_000_000).toFloat()
        } else {
            0.0f
        }

        val hSec = (((t.second).toFloat() + sd) / 60) * 360.0f

        svgBase?.run {
            documentWidth = vp.width()
            documentHeight = vp.height()
            renderToCanvas(canvas, vp)
        }

        if (showSecond && enableSubSecond) {

            svgSubsecondBase?.run {
                documentWidth = vp.width()
                documentHeight = vp.height()
                renderToCanvas(canvas, vp)
            }

            svgSubsecondHandle?.run {
                documentWidth = vp.width()
                documentHeight = vp.height()

                canvas.save()

                canvas.translate(scx, scy)
                canvas.rotate(hSec)
                canvas.translate(-scx, -scy)

                renderToCanvas(canvas, vp)

                canvas.restore()
            }

            svgSubsecondCenterCircle?.run {
                documentWidth = vp.width()
                documentHeight = vp.height()
                renderToCanvas(canvas, vp)
            }
        }

        svgLongHandle?.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()

            canvas.translate(bcx, bcy)
            canvas.rotate(hMin)
            canvas.translate(-bcx, -bcy)

            renderToCanvas(canvas, vp)

            canvas.restore()
        }

        svgShortHandle?.run {
            documentWidth = vp.width()
            documentHeight = vp.height()

            canvas.save()

            canvas.translate(bcx, bcy)
            canvas.rotate(hHour)
            canvas.translate(-bcx, -bcy)

            renderToCanvas(canvas, vp)

            canvas.restore()
        }

        if (showSecond && !enableSubSecond) {
            svgSecondHandle?.run {
                documentWidth = vp.width()
                documentHeight = vp.height()

                canvas.save()

                canvas.translate(bcx, bcy)
                canvas.rotate(hSec)
                canvas.translate(-bcx, -bcy)

                renderToCanvas(canvas, vp)

                canvas.restore()
            }
        }

        if (!enableSubSecond) {
            svgCenterCircle?.run {
                documentWidth = vp.width()
                documentHeight = vp.height()

                canvas.save()
                renderToCanvas(canvas, vp)
                canvas.restore()
            }
        }
    }
}