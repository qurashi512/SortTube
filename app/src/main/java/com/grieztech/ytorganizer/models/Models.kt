package com.grieztech.ytorganizer.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

// ═══════════════════════════════════════
//  GriezTech - Data Models
//  نماذج البيانات
// ═══════════════════════════════════════

// ── نموذج المجلد ──
@Parcelize
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true)
    val id       : Long   = 0,
    val name     : String,          // اسم المجلد
    val nameAr   : String = "",     // الاسم بالعربية
    val emoji    : String = "📁",   // رمز تعبيري للمجلد
    val color    : String = "#FF4444", // لون المجلد (Hex)
    val position : Int    = 0,      // ترتيب المجلد في القائمة
    val createdAt: Long   = System.currentTimeMillis(),
    val updatedAt: Long   = System.currentTimeMillis(),
) : Parcelable

// ── نموذج القناة ──
@Parcelize
@Entity(
    tableName = "channels",
    foreignKeys = [
        ForeignKey(
            entity        = Folder::class,
            parentColumns = ["id"],
            childColumns  = ["folderId"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("folderId")]
)
data class Channel(
    @PrimaryKey
    val id           : String,      // معرّف قناة يوتيوب (UCxxx...)
    val folderId     : Long,        // المجلد الذي تنتمي إليه
    val title        : String,      // اسم القناة
    val description  : String = "", // وصف القناة
    val thumbnailUrl : String = "", // رابط الصورة المصغرة
    val subscriberCount: Long = 0,  // عدد المشتركين
    val videoCount   : Long  = 0,   // عدد الفيديوهات
    val position     : Int   = 0,   // الترتيب داخل المجلد
    val lastFetched  : Long  = 0,   // آخر وقت تم جلب البيانات
) : Parcelable

// ── نموذج قائمة التشغيل ──
@Parcelize
@Entity(
    tableName = "playlists",
    foreignKeys = [
        ForeignKey(
            entity        = Folder::class,
            parentColumns = ["id"],
            childColumns  = ["folderId"],
            onDelete      = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("folderId")]
)
data class Playlist(
    @PrimaryKey
    val id          : String,       // معرّف قائمة التشغيل
    val folderId    : Long,
    val title       : String,
    val description : String = "",
    val thumbnailUrl: String = "",
    val itemCount   : Int    = 0,   // عدد الفيديوهات في القائمة
    val channelTitle: String = "",  // اسم القناة المالكة
    val position    : Int    = 0,
) : Parcelable

// ── نموذج الفيديو (للعرض فقط - لا يُحفظ في DB) ──
data class Video(
    val id          : String,
    val title       : String,
    val description : String = "",
    val thumbnailUrl: String = "",
    val channelTitle: String = "",
    val channelId   : String = "",
    val publishedAt : String = "",
    val duration    : String = "",  // مدة الفيديو (ISO 8601)
    val viewCount   : Long   = 0,
    val likeCount   : Long   = 0,
)

// ── حالة العنصر المسحوب في Drag & Drop ──
data class DraggableItem(
    val key     : String,
    val type    : ItemType,
    val folderId: Long? = null,
)

enum class ItemType { CHANNEL, PLAYLIST, FOLDER }
