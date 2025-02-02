package net.accelf.itc_lms_unofficial.settings

import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import net.accelf.itc_lms_unofficial.Prefs
import net.accelf.itc_lms_unofficial.R
import net.accelf.itc_lms_unofficial.di.EncryptedDataStore
import net.accelf.itc_lms_unofficial.di.SavedCookieJar
import net.accelf.itc_lms_unofficial.models.SelectOption.Companion.selectedValue
import net.accelf.itc_lms_unofficial.models.SelectOption.Companion.toTextStrings
import net.accelf.itc_lms_unofficial.models.SelectOption.Companion.toValueStrings
import net.accelf.itc_lms_unofficial.util.onSuccess
import net.accelf.itc_lms_unofficial.util.restartApp
import javax.inject.Inject

@AndroidEntryPoint
class PreferenceFragment : PreferenceFragmentCompat() {

    @Inject
    lateinit var encryptedDataStore: EncryptedDataStore

    @Inject
    lateinit var cookieJar: SavedCookieJar

    private val viewModel by activityViewModels<PreferenceViewModel>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        makePreferenceScreen {
            preferenceCategory(R.string.pref_category_language) {
                listPreference {
                    key = Prefs.Keys.LMS_LANGUAGE
                    setTitle(R.string.pref_title_lms_language)
                    setSummary(R.string.pref_text_lms_language)
                    isPersistent = false

                    setOnPreferenceChangeListener { _, value ->
                        isEnabled = false
                        viewModel.setLanguage(value as String)
                        false
                    }

                    isEnabled = false
                    viewModel.settings.onSuccess(this@PreferenceFragment) {
                        entries = it.languages.toTextStrings().toTypedArray()
                        entryValues = it.languages.toValueStrings().toTypedArray()
                        value = it.languages.selectedValue()
                        isEnabled = true
                    }
                }
            }

            val automateLoginDependents = mutableListOf<EditTextPreference>()
            preferenceCategory(R.string.pref_category_login) {
                preference {
                    key = Prefs.Keys.LOGOUT
                    setTitle(R.string.pref_title_logout)
                    setSummary(R.string.pref_text_logout)
                    setOnPreferenceClickListener {
                        sharedPreferences?.edit()
                            ?.putStringSet(Prefs.Keys.COOKIE, emptySet())
                            ?.apply()
                        cookieJar.loadCookies()
                        restartApp()
                        true
                    }
                }
                switchPreference {
                    key = Prefs.Keys.AUTOMATE_LOGIN
                    setTitle(R.string.pref_title_automate_login)
                    setSummary(R.string.pref_text_automate_login)
                    setOnPreferenceClickListener {
                        if (!isChecked) {
                            automateLoginDependents.forEach {
                                it.text = ""
                            }
                        }
                        false
                    }
                }
                editTextPreference {
                    key = Prefs.Keys.LOGIN_USERNAME
                    setTitle(R.string.login_hint_user_name)
                    preferenceDataStore = encryptedDataStore
                    automateLoginDependents.add(this)

                    setOnBindEditTextListener {
                        it.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    }
                }
                editTextPreference {
                    key = Prefs.Keys.LOGIN_PASSWORD
                    setTitle(R.string.input_hint_password)
                    preferenceDataStore = encryptedDataStore
                    automateLoginDependents.add(this)

                    setOnBindEditTextListener {
                        it.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                }
            }
            automateLoginDependents.forEach {
                it.dependency = Prefs.Keys.AUTOMATE_LOGIN
            }
        }
    }

    companion object {

        fun newInstance(): PreferenceFragment {
            return PreferenceFragment()
        }
    }
}
