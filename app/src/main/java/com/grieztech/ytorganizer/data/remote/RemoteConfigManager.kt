package com.grieztech.ytorganizer.data.remote

import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - Firebase Remote Config Manager
//  تحكم في التطبيق بدون نشر update على Play Store
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    companion object {
        // ── مفاتيح Remote Config (يجب إضافتها في Firebase Console) ──────
        const val KEY_MAX_VIDEO_RESULTS    = "max_video_results"       // عدد الفيديوهات المجلوبة
        const val KEY_SHOW_CHANNEL_VIDEOS  = "show_channel_videos"     // إظهار/إخفاء الفيديوهات
        const val KEY_NOTICE_MESSAGE       = "notice_message"          // رسالة إشعار للمستخدمين
        const val KEY_SHOW_NOTICE          = "show_notice"             // إظهار/إخفاء الرسالة
        const val KEY_QUOTA_SAFE_MODE      = "quota_safe_mode"         // وضع توفير الـ Quota
        const val KEY_APP_MIN_VERSION      = "app_min_version"         // أقل إصدار مدعوم
        const val KEY_SYNC_COOLDOWN_HOURS  = "sync_cooldown_hours"     // الحد الأدنى بين كل sync
    }

    // ── القيم الافتراضية (تعمل بدون إنترنت) ─────────────────────────────
    private val defaults = mapOf(
        KEY_MAX_VIDEO_RESULTS   to 10L,
        KEY_SHOW_CHANNEL_VIDEOS to true,
        KEY_NOTICE_MESSAGE      to "",
        KEY_SHOW_NOTICE         to false,
        KEY_QUOTA_SAFE_MODE     to false,
        KEY_APP_MIN_VERSION     to 1L,
        KEY_SYNC_COOLDOWN_HOURS to 1L,
    )

    init {
        // إعدادات الـ fetch — في Debug كل 30 ثانية، في Release كل 12 ساعة
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (isDebug()) 30L else 43200L
        }
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(defaults)
    }

    // ── جلب وتفعيل القيم من Firebase ─────────────────────────────────────
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            false // يستخدم القيم الافتراضية عند الفشل
        }
    }

    // ── Getters ──────────────────────────────────────────────────────────

    /** عدد الفيديوهات المجلوبة لكل قناة (افتراضي: 10) */
    val maxVideoResults: Int
        get() = remoteConfig.getLong(KEY_MAX_VIDEO_RESULTS).toInt().coerceIn(1, 50)

    /** هل يتم عرض فيديوهات القناة؟ */
    val showChannelVideos: Boolean
        get() = remoteConfig.getBoolean(KEY_SHOW_CHANNEL_VIDEOS)

    /** رسالة إشعار للمستخدمين (فارغة = لا رسالة) */
    val noticeMessage: String
        get() = remoteConfig.getString(KEY_NOTICE_MESSAGE)

    /** هل يتم عرض رسالة الإشعار؟ */
    val showNotice: Boolean
        get() = remoteConfig.getBoolean(KEY_SHOW_NOTICE)

    /**
     * وضع توفير الـ Quota — عند تفعيله:
     * لا يجلب فيديوهات تلقائياً ويقلل الطلبات
     */
    val quotaSafeMode: Boolean
        get() = remoteConfig.getBoolean(KEY_QUOTA_SAFE_MODE)

    /** أقل رقم إصدار مدعوم للتطبيق */
    val appMinVersion: Int
        get() = remoteConfig.getLong(KEY_APP_MIN_VERSION).toInt()

    /** الحد الأدنى بالساعات بين كل sync (افتراضي: ساعة) */
    val syncCooldownHours: Int
        get() = remoteConfig.getLong(KEY_SYNC_COOLDOWN_HOURS).toInt()

    private fun isDebug(): Boolean {
        return try {
            Class.forName("com.grieztech.ytorganizer.BuildConfig")
                .getField("DEBUG").getBoolean(null)
        } catch (e: Exception) { false }
    }
}
