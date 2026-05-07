package com.grieztech.ytorganizer.data.api

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - YouTube Auth Manager
//  إدارة الـ Access Token لـ YouTube API
// ═══════════════════════════════════════════════════════════════════════════

@Singleton
class YouTubeAuthManager @Inject constructor(
    private val context: Context,
) {
    private val SCOPE = "oauth2:https://www.googleapis.com/auth/youtube.readonly"

    /**
     * يرجع Access Token صالح ومتجدد دائماً.
     * clearToken أولاً لمسح أي Cache منتهي، ثم getToken لجلب توكن جديد.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            // ✅ اجلب التوكن الحالي أولاً
            val oldToken = GoogleAuthUtil.getToken(context, account.account!!, SCOPE)
            // ✅ امسح الـ Cache باستخدام التوكن نفسه
            GoogleAuthUtil.clearToken(context, oldToken)
            // ✅ اجلب توكن جديد وصالح
            GoogleAuthUtil.getToken(context, account.account!!, SCOPE)
        } catch (e: Exception) {
            null
        }
    }
}
