package com.rakshaai.security.data.preference

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LanguageManager {

    private const val PREF_NAME = "rakshaai_language_prefs"
    private const val KEY_LANGUAGE = "selected_language"
    private const val KEY_FIRST_LAUNCH = "first_launch_done"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isLanguageSelected(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, false)
    }

    fun getLanguage(context: Context): String {
        return getPrefs(context).getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit()
            .putString(KEY_LANGUAGE, languageCode)
            .putBoolean(KEY_FIRST_LAUNCH, true)
            .apply()
    }

    fun getLocalizedContext(context: Context): Context {
        val langCode = getLanguage(context)
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
