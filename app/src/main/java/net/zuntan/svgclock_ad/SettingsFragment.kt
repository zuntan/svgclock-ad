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
    Triple(1, makeConfPresetThemeValue(1), R.raw.clock_theme_1),
    Triple(2, makeConfPresetThemeValue(2), R.raw.clock_theme_2),
    Triple(3, makeConfPresetThemeValue(3), R.raw.clock_theme_3),
    Triple(4, makeConfPresetThemeValue(4), R.raw.clock_theme_4),
    Triple(5, makeConfPresetThemeValue(5), R.raw.clock_theme_5),
    Triple(6, makeConfPresetThemeValue(6), R.raw.clock_theme_6),
    Triple(7, makeConfPresetThemeValue(7), R.raw.clock_theme_7),
    Triple(8, makeConfPresetThemeValue(8), R.raw.clock_theme_8),
)

class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceChangeListener, CustomEditTextPreference.DialogCloseListener {

    companion object {
        const val REQUEST_CODE_DOWNLOADED_IMAGE = 200
    }

    var listener: Preference.OnPreferenceChangeListener? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        for (x in arrayOf(
            "confShowSecond", "confEnableSubSecond", "confEnableSecondSmoothly",
            "confPresetTheme", "confEnableCustomTheme", "confCustomThemeLocation"
        )) {
            findPreference<Preference>(x)?.onPreferenceChangeListener = this
        }

        val themeEntries = ArrayList<CharSequence>()
        val themeEntryValues = ArrayList<CharSequence>()

        var currentPresetThemeFound = false
        val currentPresetTheme =
            preferenceManager.sharedPreferences?.getString("confPresetTheme", null)

        for (x in LIST_PRESET_THEME) {

            val src = resources.openRawResource(x.third)
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

        findPreference<ListPreference>("confPresetTheme")?.apply {
            entries = themeEntries.toTypedArray()
            entryValues = themeEntryValues.toTypedArray()

            if (!currentPresetThemeFound) {
                setValueIndex(0)
            }
        }

        findPreference<EditTextPreference>("confCustomThemeLocation")?.apply {
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
                        "confShowSecond", "confEnableSecondSmoothly",
                    )) {
                        findPreference<Preference>(x)?.isEnabled = it.haveSecondHandle()
                    }

                    for (x in arrayOf(
                        "confEnableSubSecond"
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