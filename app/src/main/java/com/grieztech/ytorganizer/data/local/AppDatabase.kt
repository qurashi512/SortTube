package com.grieztech.ytorganizer.data.local

import androidx.room.*
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import kotlinx.coroutines.flow.Flow

// ═══════════════════════════════════════
//  GriezTech - Local Database (Room)
//  قاعدة البيانات المحلية
// ═══════════════════════════════════════

// ── DAO للمجلدات ──
@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY position ASC")
    fun getAllFolders(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Long): Folder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: Folder): Long

    @Update
    suspend fun updateFolder(folder: Folder)

    @Delete
    suspend fun deleteFolder(folder: Folder)

    @Query("UPDATE folders SET position = :position WHERE id = :id")
    suspend fun updateFolderPosition(id: Long, position: Int)

    // تحديث مواضع متعددة دفعةً واحدة
    @Transaction
    suspend fun updatePositions(folders: List<Folder>) {
        folders.forEachIndexed { index, folder ->
            updateFolderPosition(folder.id, index)
        }
    }

    @Query("SELECT COUNT(*) FROM folders")
    suspend fun getFolderCount(): Int

    // ✅ التحقق من وجود مجلد بنفس الاسم (غير حساس لحالة الأحرف)
    @Query("SELECT * FROM folders WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getFolderByName(name: String): Folder?

    // ✅ إنشاء مجلد ونقل القنوات/القوائم في transaction واحدة ذرية
    @Transaction
    suspend fun createFolderAndMoveItems(
        folder     : Folder,
        channelDao : ChannelDao,
        playlistDao: PlaylistDao,
        channelIds : List<String>,
        playlistIds: List<String>,
    ): Long {
        val folderId = insertFolder(folder)
        channelIds.forEachIndexed  { idx, id -> channelDao.moveChannel(id, folderId, idx) }
        playlistIds.forEachIndexed { idx, id -> playlistDao.movePlaylist(id, folderId, idx) }
        return folderId
    }
}

// ── DAO للقنوات ──
@Dao
interface ChannelDao {

    @Query("SELECT * FROM channels WHERE folderId = :folderId ORDER BY position ASC")
    fun getChannelsInFolder(folderId: Long): Flow<List<Channel>>

    @Query("SELECT * FROM channels ORDER BY position ASC")
    fun getAllChannels(): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id")
    suspend fun getChannelById(id: String): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE folderId = :folderId")
    suspend fun deleteChannelsInFolder(folderId: Long)

    @Query("UPDATE channels SET folderId = :newFolderId, position = :position WHERE id = :channelId")
    suspend fun moveChannel(channelId: String, newFolderId: Long, position: Int)

    @Query("UPDATE channels SET position = :position WHERE id = :id")
    suspend fun updateChannelPosition(id: String, position: Int)

    @Query("SELECT COUNT(*) FROM channels WHERE folderId = :folderId")
    suspend fun getChannelCountInFolder(folderId: Long): Int
}

// ── DAO لقوائم التشغيل ──
@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists WHERE folderId = :folderId ORDER BY position ASC")
    fun getPlaylistsInFolder(folderId: Long): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY position ASC")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Query("UPDATE playlists SET folderId = :newFolderId, position = :position WHERE id = :playlistId")
    suspend fun movePlaylist(playlistId: String, newFolderId: Long, position: Int)
}

// ── قاعدة البيانات الرئيسية ──
@Database(
    entities  = [Folder::class, Channel::class, Playlist::class],
    version   = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "grieztech_db"
    }
}
