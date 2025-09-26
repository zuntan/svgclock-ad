package net.zuntan.svgclock_ad

import android.os.Debug
import android.util.Log

object Logcat {

    private var isEnabled = true

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    private val ignoreClassNames = setOf(
        Logcat::class.java.name,
        Debug::class.java.name,
    )

    private fun println(level: Int, t: Throwable?, message: String?, args: Array<out Any?>) {
        if (!isEnabled) return

        val element =
            Throwable().stackTrace.firstOrNull { it.className !in ignoreClassNames } ?: return
        val tag = element.className.extractSimpleClassName()
        val m =
            if (message == null) {
                "<null>"
            } else if (message.isEmpty() || args.isEmpty()) {
                message
            } else {
                message.format(*args)
            }

        if (t == null) {
            Log.println(level, tag, m)
        } else {
            Log.println(level, tag, "$m\n${Log.getStackTraceString(t)}")
        }
    }

    private fun String.extractSimpleClassName(): String {
        val startIndex = lastIndexOf('.').let { if (it == -1) 0 else it + 1 }
        val endIndex = indexOf('$', startIndex).let { if (it == -1) length else it }
        return substring(startIndex, endIndex)
    }

    fun d(message: String?, vararg args: Any?) {
        println(Log.DEBUG, null, message, args)
    }

    fun d(message: String?) {
        println(Log.DEBUG, null, message, emptyArray())
    }

    fun d(t: Throwable?, message: String?, vararg args: Any?) {
        println(Log.DEBUG, t, message, args)
    }

    fun d(t: Throwable?, message: String?) {
        println(Log.DEBUG, t, message, emptyArray())
    }

    fun d(t: Throwable?) {
        println(Log.DEBUG, t, "", emptyArray())
    }

    //

    fun w(message: String?, vararg args: Any?) {
        println(Log.WARN, null, message, args)
    }

    fun w(message: String?) {
        println(Log.WARN, null, message, emptyArray())
    }

    fun w(t: Throwable?, message: String?, vararg args: Any?) {
        println(Log.WARN, t, message, args)
    }

    fun w(t: Throwable?, message: String?) {
        println(Log.WARN, t, message, emptyArray())
    }

    fun w(t: Throwable?) {
        println(Log.WARN, t, "", emptyArray())
    }

    //

    fun e(message: String?, vararg args: Any?) {
        println(Log.ERROR, null, message, args)
    }

    fun e(message: String?) {
        println(Log.ERROR, null, message, emptyArray())
    }

    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        println(Log.ERROR, t, message, args)
    }

    fun e(t: Throwable?, message: String?) {
        println(Log.ERROR, t, message, emptyArray())
    }

    fun e(t: Throwable?) {
        println(Log.ERROR, t, "", emptyArray())
    }

}