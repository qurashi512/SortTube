package com.grieztech.ytorganizer.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - YouTube API Service  (FIXED: Bearer token, no key param)
// ═══════════════════════════════════════════════════════════════════════════

// ── Response models ──────────────────────────────────────────────────────

data class YouTubeSubscriptionsResponse(
    val nextPageToken: String? = null,
    val pageInfo     : PageInfo = PageInfo(0, 0),
    val items        : List<SubscriptionItem> = emptyList(),
)
data class SubscriptionItem(val id: String = "", val snippet: SubscriptionSnippet)
data class SubscriptionSnippet(
    val title      : String = "",
    val description: String = "",
    val publishedAt: String = "",
    val resourceId : ResourceId,
    val thumbnails : Thumbnails = Thumbnails(),
)
data class ResourceId(val kind: String = "", val channelId: String = "")

data class YouTubeChannelsResponse(val items: List<ChannelItem> = emptyList())
data class ChannelItem(
    val id        : String = "",
    val snippet   : ChannelSnippet = ChannelSnippet(),
    val statistics: ChannelStatistics? = null,
)
data class ChannelSnippet(
    val title      : String     = "",
    val description: String     = "",
    val thumbnails : Thumbnails = Thumbnails(),
)
data class ChannelStatistics(
    val subscriberCount: String = "0",
    val videoCount     : String = "0",
)

data class YouTubePlaylistsResponse(
    val nextPageToken: String? = null,
    val items        : List<PlaylistItem> = emptyList(),
)
data class PlaylistItem(
    val id            : String = "",
    val snippet       : PlaylistSnippet = PlaylistSnippet(),
    val contentDetails: PlaylistContentDetails? = null,
)
data class PlaylistSnippet(
    val title       : String     = "",
    val description : String     = "",
    val thumbnails  : Thumbnails = Thumbnails(),
    val channelTitle: String     = "",
)
data class PlaylistContentDetails(val itemCount: Int = 0)

data class YouTubeVideosResponse(
    val nextPageToken: String? = null,
    val items        : List<VideoItem> = emptyList(),
)
data class VideoItem(
    val id            : String                  = "",
    val snippet       : VideoSnippet            = VideoSnippet(),
    val contentDetails: VideoContentDetails?    = null,
    val statistics    : VideoStatistics?        = null,
)
data class VideoSnippet(
    val title      : String     = "",
    val description: String     = "",
    val thumbnails : Thumbnails = Thumbnails(),
    val channelTitle: String    = "",
    val channelId  : String     = "",
    val publishedAt: String     = "",
)
data class VideoContentDetails(val duration: String = "")
data class VideoStatistics(val viewCount: String = "0", val likeCount: String = "0")

data class Thumbnails(
    val default : Thumbnail? = null,
    val medium  : Thumbnail? = null,
    val high    : Thumbnail? = null,
    val standard: Thumbnail? = null,
    val maxres  : Thumbnail? = null,
) {
    fun getBestUrl() = maxres?.url ?: standard?.url ?: high?.url ?: medium?.url ?: default?.url ?: ""
}
data class Thumbnail(val url: String = "")
data class PageInfo(val totalResults: Int = 0, val resultsPerPage: Int = 0)

// ── Retrofit Interface ───────────────────────────────────────────────────

interface YouTubeApiService {

    // الاشتراكات — يستخدم Bearer token وليس API key
    @GET("subscriptions")
    suspend fun getSubscriptions(
        @Header("Authorization") authorization: String,
        @Query("part")       part      : String  = "snippet",
        @Query("mine")       mine      : Boolean = true,
        @Query("maxResults") maxResults: Int     = 50,
        @Query("pageToken")  pageToken : String? = null,
    ): YouTubeSubscriptionsResponse

    // تفاصيل القنوات
    @GET("channels")
    suspend fun getChannelDetails(
        @Header("Authorization") authorization: String,
        @Query("part") part: String = "snippet,statistics",
        @Query("id")   id  : String,
    ): YouTubeChannelsResponse

    // قوائم التشغيل
    @GET("playlists")
    suspend fun getMyPlaylists(
        @Header("Authorization") authorization: String,
        @Query("part")       part      : String  = "snippet,contentDetails",
        @Query("mine")       mine      : Boolean = true,
        @Query("maxResults") maxResults: Int     = 50,
        @Query("pageToken")  pageToken : String? = null,
    ): YouTubePlaylistsResponse

    // فيديوهات قناة
    @GET("search")
    suspend fun getChannelVideos(
        @Header("Authorization") authorization: String,
        @Query("part")       part      : String = "snippet",
        @Query("channelId")  channelId : String,
        @Query("type")       type      : String = "video",
        @Query("order")      order     : String = "date",
        @Query("maxResults") maxResults: Int    = 10,
    ): YouTubeVideosResponse

    // فيديوهات قائمة تشغيل
    @GET("playlistItems")
    suspend fun getPlaylistVideos(
        @Header("Authorization") authorization: String,
        @Query("part")       part      : String = "snippet",
        @Query("playlistId") playlistId: String,
        @Query("maxResults") maxResults: Int    = 20,
        @Query("pageToken")  pageToken : String? = null,
    ): YouTubeVideosResponse
}
