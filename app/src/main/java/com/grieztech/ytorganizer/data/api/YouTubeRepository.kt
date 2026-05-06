package com.grieztech.ytorganizer.data.api

import com.grieztech.ytorganizer.data.local.ChannelDao
import com.grieztech.ytorganizer.data.local.FolderDao
import com.grieztech.ytorganizer.data.local.PlaylistDao
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.models.Video
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - YouTube Repository
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
) {

    // ─── helpers ───────────────────────────────────────────────────────────
    private suspend fun bearer(): String {
        val token = authManager.getAccessToken()
            ?: throw Exception("لا يوجد حساب مسجَّل أو انتهت الجلسة")
        return "Bearer $token"
    }

    // ─── Folders ───────────────────────────────────────────────────────────
    fun getAllFolders(): Flow<List<Folder>> = folderDao.getAllFolders()
    fun getChannelsInFolder(folderId: Long): Flow<List<Channel>> = channelDao.getChannelsInFolder(folderId)
    fun getAllChannels(): Flow<List<Channel>> = channelDao.getAllChannels()
    fun getPlaylistsInFolder(folderId: Long): Flow<List<Playlist>> = playlistDao.getPlaylistsInFolder(folderId)

    suspend fun createFolder(folder: Folder): Long = folderDao.insertFolder(folder)
    suspend fun updateFolder(folder: Folder) = folderDao.updateFolder(folder.copy(updatedAt = System.currentTimeMillis()))
    suspend fun deleteFolder(folder: Folder) = folderDao.deleteFolder(folder)
    suspend fun reorderFolders(folders: List<Folder>) = folderDao.updatePositions(folders)

    suspend fun isFolderNameTaken(name: String): Boolean =
        folderDao.getFolderByName(name) != null

    suspend fun createFolderWithItems(
        folder      : Folder,
        channelIds  : List<String>,
        playlistIds : List<String>,
    ): Long = folderDao.createFolderAndMoveItems(folder, channelDao, playlistDao, channelIds, playlistIds)

    fun getAllPlaylists() = playlistDao.getAllPlaylists()

    // ─── Sync Subscriptions (القنوات المشترك بها) ─────────────────────────
    suspend fun syncSubscriptions(defaultFolderId: Long): Result<List<Channel>> {
        return try {
            val auth     = bearer()
            val channels = mutableListOf<Channel>()
            var pageToken: String? = null
            var position = channelDao.getChannelCountInFolder(defaultFolderId)

            do {
                val subsResp = apiService.getSubscriptions(
                    authorization = auth,
                    pageToken     = pageToken,
                )

                if (subsResp.items.isEmpty()) break

                val ids      = subsResp.items.joinToString(",") { it.snippet.resourceId.channelId }
                val statsMap = try {
                    apiService.getChannelDetails(authorization = auth, id = ids).items.associateBy { it.id }
                } catch (e: Exception) { emptyMap() }

                subsResp.items.forEach { sub ->
                    val chId  = sub.snippet.resourceId.channelId
                    if (channelDao.getChannelById(chId) != null) return@forEach
                    val stats = statsMap[chId]?.statistics
                    channels.add(
                        Channel(
                            id              = chId,
                            folderId        = defaultFolderId,
                            title           = sub.snippet.title,
                            description     = sub.snippet.description,
                            thumbnailUrl    = sub.snippet.thumbnails.getBestUrl(),
                            subscriberCount = stats?.subscriberCount?.toLongOrNull() ?: 0L,
                            videoCount      = stats?.videoCount?.toLongOrNull() ?: 0L,
                            position        = position++,
                            lastFetched     = System.currentTimeMillis(),
                        )
                    )
                }
                pageToken = subsResp.nextPageToken
            } while (pageToken != null)

            if (channels.isNotEmpty()) channelDao.insertChannels(channels)
            Result.Success(channels)

        } catch (e: Exception) {
            Result.Error("فشل جزئي في الاستيراد، تحقق من الاتصال", e)
        }
    }

    // ─── Sync Playlists ────────────────────────────────────────────────────
    suspend fun syncPlaylists(defaultFolderId: Long): Result<List<Playlist>> {
        return try {
            val auth      = bearer()
            val playlists = mutableListOf<Playlist>()
            var pageToken : String? = null
            var position  = 0

            do {
                val resp = apiService.getMyPlaylists(authorization = auth, pageToken = pageToken)
                resp.items.forEach { item ->
                    playlists.add(
                        Playlist(
                            id           = item.id,
                            folderId     = defaultFolderId,
                            title        = item.snippet.title,
                            description  = item.snippet.description,
                            thumbnailUrl = item.snippet.thumbnails.getBestUrl(),
                            itemCount    = item.contentDetails?.itemCount ?: 0,
                            channelTitle = item.snippet.channelTitle,
                            position     = position++,
                        )
                    )
                }
                pageToken = resp.nextPageToken
            } while (pageToken != null)

            if (playlists.isNotEmpty()) playlistDao.insertPlaylists(playlists)
            Result.Success(playlists)
        } catch (e: Exception) {
            Result.Error("فشل جلب قوائم التشغيل: ${e.message}", e)
        }
    }

    // ─── Channel Videos ────────────────────────────────────────────────────
    suspend fun getChannelVideos(channelId: String): Result<List<Video>> {
        return try {
            val auth = bearer()
            val resp = apiService.getChannelVideos(authorization = auth, channelId = channelId)
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

    // ─── Move / Delete ──────────────────────────────────────────────────────
    suspend fun moveChannel(channelId: String, targetFolderId: Long, position: Int) =
        channelDao.moveChannel(channelId, targetFolderId, position)

    suspend fun movePlaylist(id: String, folderId: Long, position: Int) =
        playlistDao.movePlaylist(id, folderId, position)

    suspend fun deleteChannel(channel: Channel) =
        channelDao.deleteChannel(channel)

    suspend fun deletePlaylist(playlist: Playlist) =
        playlistDao.deletePlaylist(playlist)
}