package com.grieztech.ytorganizer.data.api

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.grieztech.ytorganizer.data.local.ChannelDao
import com.grieztech.ytorganizer.data.local.FolderDao
import com.grieztech.ytorganizer.data.local.PlaylistDao
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.models.Video
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - YouTube Repository
//  ✅ FIX: كل عملية مُقيَّدة بـ accountId من Google
// ═══════════════════════════════════════════════════════════════════════════

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

@Singleton
class YouTubeRepository @Inject constructor(
    private val apiService : YouTubeApiService,
    private val authManager: YouTubeAuthManager,
    private val folderDao  : FolderDao,
    private val channelDao : ChannelDao,
    private val playlistDao: PlaylistDao,
    @ApplicationContext private val context: Context,
) {

    // ✅ جلب ID الحساب الحالي دائماً من Google
    private fun currentAccountId(): String {
        return GoogleSignIn.getLastSignedInAccount(context)?.id ?: ""
    }

    private suspend fun bearer(): String {
        val token = authManager.getAccessToken()
            ?: throw Exception("لا يوجد حساب مسجَّل أو انتهت الجلسة")
        return "Bearer $token"
    }

    // ─── Folders ─────────────────────────────────────────────────────────

    // ✅ يُرجع فقط مجلدات الحساب الحالي
    fun getAllFolders(): Flow<List<Folder>> =
        folderDao.getFoldersForAccount(currentAccountId())

    fun getChannelsInFolder(folderId: Long): Flow<List<Channel>> =
        channelDao.getChannelsInFolder(folderId, currentAccountId())

    fun getAllChannels(): Flow<List<Channel>> =
        channelDao.getAllChannelsForAccount(currentAccountId())

    fun getPlaylistsInFolder(folderId: Long): Flow<List<Playlist>> =
        playlistDao.getPlaylistsInFolder(folderId, currentAccountId())

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylistsForAccount(currentAccountId())

    // ✅ المجلد يُنشأ بـ accountId تلقائياً
    suspend fun createFolder(folder: Folder): Long =
        folderDao.insertFolder(folder.copy(accountId = currentAccountId()))

    suspend fun updateFolder(folder: Folder) =
        folderDao.updateFolder(folder.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)

    suspend fun reorderFolders(folders: List<Folder>) = folderDao.updatePositions(folders)

    // ─── Sync Subscriptions ───────────────────────────────────────────────

    suspend fun syncSubscriptions(defaultFolderId: Long): Result<List<Channel>> {
        return try {
            val auth      = bearer()
            val accountId = currentAccountId()          // ✅
            val channels  = mutableListOf<Channel>()
            var pageToken : String? = null
            var position  = channelDao.getChannelCountInFolder(defaultFolderId, accountId)

            do {
                val subsResp = apiService.getSubscriptions(
                    authorization = auth, pageToken = pageToken)

                if (subsResp.items.isEmpty()) break

                val ids      = subsResp.items.joinToString(",") { it.snippet.resourceId.channelId }
                val statsMap = try {
                    apiService.getChannelDetails(authorization = auth, id = ids)
                        .items.associateBy { it.id }
                } catch (e: Exception) { emptyMap() }

                subsResp.items.forEach { sub ->
                    val chId  = sub.snippet.resourceId.channelId
                    // ✅ التحقق مع accountId
                    if (channelDao.getChannelById(chId, accountId) != null) return@forEach
                    val stats = statsMap[chId]?.statistics
                    channels.add(Channel(
                        id              = chId,
                        accountId       = accountId,    // ✅
                        folderId        = defaultFolderId,
                        title           = sub.snippet.title,
                        description     = sub.snippet.description,
                        thumbnailUrl    = sub.snippet.thumbnails.getBestUrl(),
                        subscriberCount = stats?.subscriberCount?.toLongOrNull() ?: 0L,
                        videoCount      = stats?.videoCount?.toLongOrNull()      ?: 0L,
                        position        = position++,
                        lastFetched     = System.currentTimeMillis(),
                    ))
                }
                pageToken = subsResp.nextPageToken
            } while (pageToken != null)

            if (channels.isNotEmpty()) channelDao.insertChannels(channels)
            Result.Success(channels)

        } catch (e: Exception) {
            Result.Error("فشل جزئي في الاستيراد، تحقق من الاتصال", e)
        }
    }

    // ─── Sync Playlists ───────────────────────────────────────────────────

    suspend fun syncPlaylists(defaultFolderId: Long): Result<List<Playlist>> {
        return try {
            val auth      = bearer()
            val accountId = currentAccountId()          // ✅
            val playlists = mutableListOf<Playlist>()
            var pageToken : String? = null
            var position  = 0

            do {
                val resp = apiService.getMyPlaylists(
                    authorization = auth, pageToken = pageToken)
                resp.items.forEach { item ->
                    playlists.add(Playlist(
                        id           = item.id,
                        accountId    = accountId,       // ✅
                        folderId     = defaultFolderId,
                        title        = item.snippet.title,
                        description  = item.snippet.description,
                        thumbnailUrl = item.snippet.thumbnails.getBestUrl(),
                        itemCount    = item.contentDetails?.itemCount ?: 0,
                        channelTitle = item.snippet.channelTitle,
                        position     = position++,
                    ))
                }
                pageToken = resp.nextPageToken
            } while (pageToken != null)

            if (playlists.isNotEmpty()) playlistDao.insertPlaylists(playlists)
            Result.Success(playlists)
        } catch (e: Exception) {
            Result.Error("فشل جلب قوائم التشغيل: ${e.message}", e)
        }
    }

    // ─── Channel Videos ───────────────────────────────────────────────────

    suspend fun getChannelVideos(channelId: String): Result<List<Video>> {
        return try {
            val resp = apiService.getChannelVideos(
                authorization = bearer(), channelId = channelId)
            Result.Success(resp.items.map {
                Video(
                    id           = it.id,
                    title        = it.snippet.title,
                    description  = it.snippet.description,
                    thumbnailUrl = it.snippet.thumbnails.getBestUrl(),
                    channelTitle = it.snippet.channelTitle,
                    channelId    = it.snippet.channelId,
                    publishedAt  = it.snippet.publishedAt,
                )
            })
        } catch (e: Exception) {
            Result.Error("فشل جلب الفيديوهات: ${e.message}", e)
        }
    }

    // ─── Move / Delete ────────────────────────────────────────────────────

    suspend fun moveChannel(channelId: String, targetFolderId: Long, position: Int) =
        channelDao.moveChannel(channelId, targetFolderId, position, currentAccountId())

    suspend fun deleteChannel(channel: Channel) = channelDao.deleteChannel(channel)

    // ✅ السيناريو الوحيد الذي نمسح فيه البيانات:
    //    عندما يسجّل المستخدم دخولاً بـ accountId مختلف عن السابق
    //    (لا نمسح عند تسجيل الخروج العادي — البيانات تبقى للحساب)
    suspend fun clearAccountData(accountId: String) {
        channelDao.deleteAllForAccount(accountId)
        playlistDao.deleteAllForAccount(accountId)
        folderDao.deleteAllForAccount(accountId)
    }

    // ✅ يُستدعى عند تسجيل الدخول — إذا تغيّر الحساب نمسح بيانات الحساب القديم
    //    أما إذا نفس الحساب فلا نفعل شيئاً والبيانات تظهر كما هي
    suspend fun handleAccountSwitch(previousAccountId: String, newAccountId: String) {
        // نفس الحساب → لا تفعل شيئاً، البيانات موجودة
        if (previousAccountId == newAccountId || previousAccountId.isEmpty()) return
        // حساب مختلف → امسح بيانات الحساب القديم من الجهاز
        clearAccountData(previousAccountId)
    }

// ─── دوال مساعدة إضافية ───────────────────────────────────────────────────

    suspend fun isFolderNameTaken(name: String): Boolean {
        // ✅ يتحقق فقط بين مجلدات الحساب الحالي
        var taken = false
        getAllFolders().take(1).collect { list ->
            taken = list.any { it.name.equals(name, ignoreCase = true) }
        }
        return taken
    }

    suspend fun createFolderWithItems(
        folder     : Folder,
        channelIds : List<String>,
        playlistIds: List<String>,
    ): Long {
        val accountId = currentAccountId()
        val folderId  = folderDao.insertFolder(folder.copy(accountId = accountId))
        channelIds.forEachIndexed  { i, id -> channelDao.moveChannel(id,  folderId, i, accountId) }
        playlistIds.forEachIndexed { i, id -> playlistDao.movePlaylist(id, folderId, i, accountId) }
        return folderId
    }

    suspend fun movePlaylist(playlistId: String, targetFolderId: Long, position: Int) =
        playlistDao.movePlaylist(playlistId, targetFolderId, position, currentAccountId())

    suspend fun deletePlaylist(playlist: Playlist) = playlistDao.deletePlaylist(playlist)
}
