package net.zuntan.svgclock_ad

import android.content.Context
import android.util.AttributeSet
import androidx.preference.EditTextPreference

class CustomEditTextPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.preference.R.attr.editTextPreferenceStyle
) : EditTextPreference(context, attrs, defStyleAttr) {

    override fun getDialogLayoutResource(): Int {
        return super.getDialogLayoutResource()
    }

    interface DialogCloseListener {
        fun onDialogClosed(key: String, success: Boolean, newValue: String?)
    }
}

