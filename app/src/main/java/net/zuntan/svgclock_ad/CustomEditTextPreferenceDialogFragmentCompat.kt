package net.zuntan.svgclock_ad

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceDialogFragmentCompat


class CustomEditTextPreferenceDialogFragmentCompat : PreferenceDialogFragmentCompat() {


    companion object {
        fun newInstance(key: String): CustomEditTextPreferenceDialogFragmentCompat {
            val fragment = CustomEditTextPreferenceDialogFragmentCompat()
            val bundle = Bundle(1)
            bundle.putString(ARG_KEY, key)
            fragment.arguments = bundle
            return fragment
        }

        const val SAVE_STATE_TEXT: String = "CustomEditTextPreferenceDialogFragment.text"
    }
    private lateinit var editText: EditText
    private var mText: CharSequence? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mText = getEditTextPreference().getText()
        } else {
            mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
        }
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        editText = view.findViewById(android.R.id.edit)
            ?: throw IllegalStateException("Dialog view must contain an EditText with id android.R.id.edit")

        setText( mText )
    }

    fun setText( text: CharSequence? )
    {
        editText.requestFocus()
        editText.setText(text)
        editText.setSelection( editText.getText().length )
    }

    private fun getEditTextPreference(): CustomEditTextPreference {
        return getPreference() as CustomEditTextPreference
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = getEditTextPreference()
        val finalValue: String?

        if (positiveResult) {
            val value = editText.getText().toString()
            if (preference.callChangeListener(value)) {
                preference.setText(value)
                finalValue = value
            }
            else{
                finalValue = preference.text
            }
        } else {
            finalValue = null
        }

        val target = targetFragment
        if (target is CustomEditTextPreference.DialogCloseListener) {
            target.onDialogClosed(preference.key, positiveResult, finalValue)
        }
    }
}