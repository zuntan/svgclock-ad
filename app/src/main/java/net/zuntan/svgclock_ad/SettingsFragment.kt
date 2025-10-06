package net.zuntan.svgclock_ad

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat

private fun makeConfPresetThemeValue(no: Int): String {
    return "Theme_%d".format(no)
}

val LIST_PRESET_THEME = listOf(
    Triple(1, makeConfPresetThemeValue(1), "theme/clock_theme_1.svg"),
    Triple(2, makeConfPresetThemeValue(2), "theme/clock_theme_2.svg"),
    Triple(3, makeConfPresetThemeValue(3), "theme/clock_theme_3.svg"),
    Triple(4, makeConfPresetThemeValue(4), "theme/clock_theme_4.svg"),
    Triple(5, makeConfPresetThemeValue(5), "theme/clock_theme_5.svg"),
    Triple(6, makeConfPresetThemeValue(6), "theme/clock_theme_6.svg"),
    Triple(7, makeConfPresetThemeValue(7), "theme/clock_theme_7.svg"),
    Triple(8, makeConfPresetThemeValue(8), "theme/clock_theme_8.svg"),
)

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, CustomEditTextPreference.DialogCloseListener {

    companion object {
        const val REQUEST_CODE_DOWNLOADED_IMAGE = 200

        const val CONF_SHOW_SECOND = "confShowSecond"
        const val CONF_ENABLE_SUBSECOND = "confEnableSubSecond"
        const val CONF_ENABLE_SECOND_SMOOTHLY = "confEnableSecondSmoothly"
        const val CONF_PRESET_THEME = "confPresetTheme"
        const val CONF_ENABLE_CUSTOM_THEME = "confEnableCustomTheme"
        const val CONF_CUSTOM_THEME_LOCATION = "confCustomThemeLocation"

    }

    var listener: Preference.OnPreferenceChangeListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        for (x in arrayOf(
            CONF_SHOW_SECOND, CONF_ENABLE_SUBSECOND, CONF_ENABLE_SECOND_SMOOTHLY,
            CONF_PRESET_THEME, CONF_ENABLE_CUSTOM_THEME, CONF_CUSTOM_THEME_LOCATION
        )) {
            findPreference<Preference>(x)?.onPreferenceChangeListener = this
        }

        val themeEntries = ArrayList<CharSequence>()
        val themeEntryValues = ArrayList<CharSequence>()

        var currentPresetThemeFound = false
        val currentPresetTheme =
            preferenceManager.sharedPreferences?.getString(CONF_PRESET_THEME, null)

        for (x in LIST_PRESET_THEME) {

            context?.assets?.open( x.third )?.use { src ->
                val im = ImageInfo(src)

                val nm = if (im.config == null || im.config!!.theme_name == null) {
                    ""
                } else {
                    im.config!!.theme_name
                }

                themeEntries.add("[No.%d] %s".format(x.first, nm))
                themeEntryValues.add(x.second)

                if (x.second == currentPresetTheme) {
                    currentPresetThemeFound = true
                }
            }
        }

        findPreference<ListPreference>(CONF_PRESET_THEME )?.apply {
            entries = themeEntries.toTypedArray()
            entryValues = themeEntryValues.toTypedArray()

            if (!currentPresetThemeFound) {
                setValueIndex(0)
            }
        }

        findPreference<EditTextPreference>(CONF_CUSTOM_THEME_LOCATION )?.apply {
            setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    type = "image/*"
                }

                startActivityForResult(intent, REQUEST_CODE_DOWNLOADED_IMAGE )

                true
            }
        }

        val app = context?.let { it.applicationContext as AppApplication }

        app?.let {

            it.onThemeChanged = object : ThemeChangedHandler {
                override fun onThemeChanged() {

                    for (x in arrayOf(
                        CONF_SHOW_SECOND, CONF_ENABLE_SECOND_SMOOTHLY,
                    )) {
                        findPreference<Preference>(x)?.isEnabled = it.haveSecondHandle()
                    }

                    for (x in arrayOf(
                        CONF_ENABLE_SUBSECOND
                    )) {
                        findPreference<Preference>(x)?.isEnabled = it.haveSubSecond()
                    }
                }
            }

            it.onThemeChanged?.onThemeChanged()
        }
    }

    var dialogFragment: CustomEditTextPreferenceDialogFragmentCompat? = null

    override fun onActivityResult( requestCode: Int, resultCode: Int, data: Intent? ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_DOWNLOADED_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->

                dialogFragment?.setText( uri.toString() )
            }
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {

        if (preference is CustomEditTextPreference) {

            dialogFragment =
                CustomEditTextPreferenceDialogFragmentCompat.newInstance(preference.key)

            dialogFragment?.setTargetFragment(this, 0)
            dialogFragment?.show(
                parentFragmentManager,
                "androidx.preference.PreferenceFragment.DIALOG" // 標準のタグを使用
            )

            return
        }

        super.onDisplayPreferenceDialog(preference)
    }

    override fun onDialogClosed(key: String, success: Boolean, newValue: String?) {
        dialogFragment = null
    }

    override fun onPreferenceChange(
        preference: Preference,
        newValue: Any?
    ): Boolean {

        listener?.let {
            return it.onPreferenceChange(preference, newValue)
        }

        return true
    }
}