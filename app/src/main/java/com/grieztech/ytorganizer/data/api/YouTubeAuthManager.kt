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
    // الـ Scope المطلوب لقراءة قائمة الاشتراكات
    private val SCOPE = "oauth2:https://www.googleapis.com/auth/youtube.readonly"

    /**
     * يرجع Access Token صالح.
     * GoogleAuthUtil يجدد التوكن تلقائياً إذا انتهت صلاحيته.
     */
    suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
            GoogleAuthUtil.getToken(context, account.account!!, SCOPE)
        } catch (e: Exception) {
            null
        }
    }
}
