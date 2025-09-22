package net.zuntan.svgclock_ad

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

    val srcOrg: String?
    val srcBase: String?
    val srcBaseText: String?
    val srcLongHandle: String?
    val srcShortHandle: String?
    val srcSecondHandle: String?
    val srcCenterCircle: String?
    val srcSubsecondBase: String?
    val srcSubsecondHandle: String?
    val srcSubsecondCenterCircle: String?
    val srcConfig: String?

    val sz: ImmutableVec?
    val vboxXY: ImmutableVec?
    val vboxWH: ImmutableVec?

    val baseCenter: ImmutableVec
    val subsecondCenter: ImmutableVec

    val svgBase: SVG?
    val svgLongHandle: SVG?
    val svgShortHandle: SVG?
    val svgSecondHandle: SVG?
    val svgSubsecondBase: SVG?
    val svgSubsecondHandle: SVG?
    val svgSubsecondCenterCircle: SVG?

    val config: Config?

    constructor(src: String) {
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

        svgBase = srcBase?.let { SVG.getFromString( it ) }
        svgLongHandle = srcLongHandle?.let { SVG.getFromString( it ) }
        svgShortHandle = srcShortHandle?.let { SVG.getFromString( it ) }
        svgSecondHandle = srcSecondHandle?.let { SVG.getFromString( it ) }
        svgSubsecondBase = srcSubsecondBase?.let { SVG.getFromString( it ) }
        svgSubsecondHandle = srcSubsecondHandle?.let { SVG.getFromString( it ) }
        svgSubsecondCenterCircle = srcSubsecondCenterCircle?.let { SVG.getFromString( it ) }
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
}