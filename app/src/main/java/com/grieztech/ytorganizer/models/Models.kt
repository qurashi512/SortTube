package com.grieztech.ytorganizer.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - Data Models
//  ✅ هذا الملف جاهز وممتاز: تم إضافة accountId بنجاح لكل جدول
// ═══════════════════════════════════════════════════════════════════════════

// ── نموذج المجلد ──────────────────────────────────────────────────────────
@Parcelize
@Entity(
    tableName = "folders",
    indices   = [Index("accountId")],           // ✅ index للبحث السريع
)
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id        : Long   = 0,
    val accountId : String = "",                // ✅ Google account ID
    val name      : String,
    val nameAr    : String = "",
    val emoji     : String = "📁",
    val color     : String = "#9B6FFF",
    val position  : Int    = 0,
    val createdAt : Long   = System.currentTimeMillis(),
    val updatedAt : Long   = System.currentTimeMillis(),
) : Parcelable

// ── نموذج القناة ──────────────────────────────────────────────────────────
@Parcelize
@Entity(
    tableName    = "channels",
    foreignKeys  = [ForeignKey(
        entity        = Folder::class,
        parentColumns = ["id"],
        childColumns  = ["folderId"],
        onDelete      = ForeignKey.NO_ACTION, // ✅ لا تحذف القنوات — ننقلها يدوياً للمجلد الافتراضي
    )],
    indices = [Index("folderId"), Index("accountId")],  // ✅
)
data class Channel(
    @PrimaryKey
    val id             : String,
    val accountId      : String = "",           // ✅
    val folderId       : Long,
    val title          : String,
    val description    : String = "",
    val thumbnailUrl   : String = "",
    val subscriberCount: Long   = 0,
    val videoCount     : Long   = 0,
    val position       : Int    = 0,
    val lastFetched    : Long   = 0,
) : Parcelable

// ── نموذج قائمة التشغيل ───────────────────────────────────────────────────
@Parcelize
@Entity(
    tableName   = "playlists",
    foreignKeys = [ForeignKey(
        entity        = Folder::class,
        parentColumns = ["id"],
        childColumns  = ["folderId"],
        onDelete      = ForeignKey.NO_ACTION, // ✅ لا تحذف البلايلست — ننقلها يدوياً للمجلد الافتراضي
    )],
    indices = [Index("folderId"), Index("accountId")],  // ✅
)
data class Playlist(
    @PrimaryKey
    val id           : String,
    val accountId    : String = "",             // ✅
    val folderId     : Long,
    val title        : String,
    val description  : String = "",
    val thumbnailUrl : String = "",
    val itemCount    : Int    = 0,
    val channelTitle : String = "",
    val position     : Int    = 0,
) : Parcelable

// ── نموذج الفيديو (للعرض فقط) ────────────────────────────────────────────
data class Video(
    val id           : String,
    val title        : String,
    val description  : String = "",
    val thumbnailUrl : String = "",
    val channelTitle : String = "",
    val channelId    : String = "",
    val publishedAt  : String = "",
    val duration     : String = "",
    val viewCount    : Long   = 0,
    val likeCount    : Long   = 0,
)

data class DraggableItem(val key: String, val type: ItemType, val folderId: Long? = null)
enum class ItemType { CHANNEL, PLAYLIST, FOLDER }