package com.grieztech.ytorganizer.utils.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

// ═══════════════════════════════════════
//  GriezTech - Locale Helper (Fixed)
//  يقبل String مباشرةً ("ar" / "en")
// ═══════════════════════════════════════

object LocaleHelper {

    enum class AppLanguage(val code: String, val displayName: String) {
        ARABIC("ar", "العربية"),
        ENGLISH("en", "English"),
    }

    // ✓ قبول String مباشرة
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    // قبول Enum (للتوافق مع الكود القديم)
    fun setLocale(context: Context, language: AppLanguage): Context =
        setLocale(context, language.code)

    fun getCurrentLanguage(context: Context): AppLanguage {
        val langCode = context.resources.configuration.locales[0].language
        return AppLanguage.values().firstOrNull { it.code == langCode }
            ?: AppLanguage.ARABIC
    }

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
}
