package com.grieztech.ytorganizer.data.local

import androidx.room.*
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE accountId = :accountId ORDER BY position ASC")
    fun getFoldersForAccount(accountId: String): Flow<List<Folder>>

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

    @Transaction
    suspend fun updatePositions(folders: List<Folder>) {
        folders.forEachIndexed { index, folder -> updateFolderPosition(folder.id, index) }
    }

    @Query("SELECT COUNT(*) FROM folders WHERE accountId = :accountId")
    suspend fun getFolderCountForAccount(accountId: String): Int

    @Query("SELECT COUNT(*) FROM folders WHERE accountId = :accountId")
    fun getFolderCountForAccountFlow(accountId: String): Flow<Int>

    @Query("DELETE FROM folders WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query("UPDATE folders SET accountId = :newAccountId WHERE accountId = ''")
    suspend fun claimOrphanedFolders(newAccountId: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE folderId = :folderId AND accountId = :accountId ORDER BY position ASC")
    fun getChannelsInFolder(folderId: Long, accountId: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE accountId = :accountId ORDER BY position ASC")
    fun getAllChannelsForAccount(accountId: String): Flow<List<Channel>>

    @Query("SELECT * FROM channels WHERE id = :id AND accountId = :accountId")
    suspend fun getChannelById(id: String, accountId: String): Channel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)

    @Update
    suspend fun updateChannel(channel: Channel)

    @Delete
    suspend fun deleteChannel(channel: Channel)

    @Query("DELETE FROM channels WHERE folderId = :folderId AND accountId = :accountId")
    suspend fun deleteChannelsInFolder(folderId: Long, accountId: String)

    @Query("UPDATE channels SET folderId = :newFolderId, position = :position WHERE id = :channelId AND accountId = :accountId")
    suspend fun moveChannel(channelId: String, newFolderId: Long, position: Int, accountId: String)

    @Query("UPDATE channels SET position = :position WHERE id = :id AND accountId = :accountId")
    suspend fun updateChannelPosition(id: String, position: Int, accountId: String)

    // ✅ الحل الجذري لمشكلة الترتيب المتعدد
    @Transaction
    suspend fun updatePositions(channels: List<Channel>) {
        channels.forEachIndexed { index, channel -> updateChannelPosition(channel.id, index, channel.accountId) }
    }

    @Query("SELECT COUNT(*) FROM channels WHERE folderId = :folderId AND accountId = :accountId")
    suspend fun getChannelCountInFolder(folderId: Long, accountId: String): Int

    @Query("SELECT COUNT(*) FROM channels WHERE accountId = :accountId")
    fun getChannelCountForAccountFlow(accountId: String): Flow<Int>

    @Query("DELETE FROM channels WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query("UPDATE channels SET accountId = :newAccountId WHERE accountId = ''")
    suspend fun claimOrphanedChannels(newAccountId: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE folderId = :folderId AND accountId = :accountId ORDER BY position ASC")
    fun getPlaylistsInFolder(folderId: Long, accountId: String): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE accountId = :accountId ORDER BY position ASC")
    fun getAllPlaylistsForAccount(accountId: String): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Query("UPDATE playlists SET folderId = :newFolderId, position = :position WHERE id = :playlistId AND accountId = :accountId")
    suspend fun movePlaylist(playlistId: String, newFolderId: Long, position: Int, accountId: String)

    @Query("UPDATE playlists SET position = :position WHERE id = :playlistId AND accountId = :accountId")
    suspend fun updatePlaylistPosition(playlistId: String, position: Int, accountId: String)

    @Transaction
    suspend fun updatePositions(playlists: List<Playlist>) {
        playlists.forEachIndexed { index, playlist -> updatePlaylistPosition(playlist.id, index, playlist.accountId) }
    }

    @Query("SELECT COUNT(*) FROM playlists WHERE accountId = :accountId")
    fun getPlaylistCountForAccountFlow(accountId: String): Flow<Int>

    @Query("DELETE FROM playlists WHERE accountId = :accountId")
    suspend fun deleteAllForAccount(accountId: String)

    @Query("UPDATE playlists SET accountId = :newAccountId WHERE accountId = ''")
    suspend fun claimOrphanedPlaylists(newAccountId: String)
}

@Database(
    entities     = [Folder::class, Channel::class, Playlist::class],
    version      = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao()  : FolderDao
    abstract fun channelDao() : ChannelDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val DATABASE_NAME = "grieztech_db"
    }
}