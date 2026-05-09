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
        SYSTEM("system", "System"),   // ✅ تلقائي مع لغة الجهاز
    }

    /**
     * يُطبّق اللغة المطلوبة على الـ Context.
     * إذا كان الكود "system" → يستخدم لغة الجهاز الأصلية.
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val resolvedCode = if (languageCode == "system") getSystemLanguageCode() else languageCode
        val locale = Locale(resolvedCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    // قبول Enum (للتوافق مع الكود القديم)
    fun setLocale(context: Context, language: AppLanguage): Context =
        setLocale(context, language.code)

    /**
     * يعيد لغة الجهاز الافتراضية (قبل أي تغيير من التطبيق).
     * مفيد لعرض معاينة اللغة التلقائية في الإعدادات.
     */
    fun getSystemLanguageCode(): String = Locale.getDefault().language

    fun getCurrentLanguage(context: Context): AppLanguage {
        val langCode = context.resources.configuration.locales[0].language
        return AppLanguage.values().firstOrNull { it.code == langCode }
            ?: AppLanguage.SYSTEM
    }

    fun isRtl(context: Context): Boolean =
        context.resources.configuration.layoutDirection == Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
}
