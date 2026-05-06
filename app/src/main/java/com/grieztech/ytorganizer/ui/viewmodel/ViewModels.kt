package com.grieztech.ytorganizer.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.grieztech.ytorganizer.data.api.Result
import com.grieztech.ytorganizer.data.api.YouTubeRepository
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.models.Video
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _channelCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val channelCounts: StateFlow<Map<Long, Int>> = _channelCounts.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncSuccess = MutableStateFlow<String?>(null)
    val syncSuccess: StateFlow<String?> = _syncSuccess.asStateFlow()

    private val _allChannels = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels.asStateFlow()

    private val _allPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val allPlaylists: StateFlow<List<Playlist>> = _allPlaylists.asStateFlow()

    private var _localFolders = mutableListOf<Folder>()

    init {
        observeFolders()
        loadAllChannelsAndPlaylists()
    }

    private fun observeFolders() {
        viewModelScope.launch {
            repository.getAllFolders().collect { folderList ->
                _localFolders = folderList.toMutableList()
                _folders.value = folderList
                refreshChannelCounts(folderList)
            }
        }
    }

    private fun loadAllChannelsAndPlaylists() {
        viewModelScope.launch {
            repository.getAllChannels().collect { channels ->
                _allChannels.value = channels
            }
        }
        viewModelScope.launch {
            repository.getAllPlaylists().collect { playlists ->
                _allPlaylists.value = playlists
            }
        }
    }

    private suspend fun refreshChannelCounts(folderList: List<Folder>) {
        val counts = mutableMapOf<Long, Int>()
        folderList.forEach { folder ->
            repository.getChannelsInFolder(folder.id)
                .take(1)
                .collect { channels -> counts[folder.id] = channels.size }
        }
        _channelCounts.value = counts
    }

    private val _folderCreateError = MutableStateFlow<String?>(null)
    val folderCreateError: StateFlow<String?> = _folderCreateError.asStateFlow()

    fun clearFolderCreateError() { _folderCreateError.value = null }

    fun createFolder(name: String, emoji: String, color: String) {
        viewModelScope.launch {
            if (repository.isFolderNameTaken(name)) {
                _folderCreateError.value = "⚠️ يوجد مجلد بهذا الاسم مسبقاً"
                return@launch
            }
            repository.createFolder(
                Folder(name = name, emoji = emoji, color = color, position = _localFolders.size)
            )
        }
    }

    fun createFolderWithItems(
        name        : String,
        emoji       : String,
        color       : String,
        channelIds  : List<String>,
        playlistIds : List<String>,
        onSuccess   : () -> Unit = {},
        onError     : (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            if (repository.isFolderNameTaken(name)) {
                val msg = "⚠️ يوجد مجلد بهذا الاسم مسبقاً"
                _folderCreateError.value = msg
                onError(msg)
                return@launch
            }
            try {
                repository.createFolderWithItems(
                    folder      = Folder(name = name, emoji = emoji, color = color, position = _localFolders.size),
                    channelIds  = channelIds,
                    playlistIds = playlistIds,
                )
                onSuccess()
            } catch (e: Exception) {
                val msg = "فشل إنشاء المجلد: ${e.message}"
                _folderCreateError.value = msg
                onError(msg)
            }
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch { repository.deleteFolder(folder) }
    }

    fun moveFolder(fromIndex: Int, toIndex: Int) {
        val list = _localFolders.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _localFolders = list
            _folders.value = list.toList()
        }
    }

    fun saveFolderOrder() {
        viewModelScope.launch {
            repository.reorderFolders(
                _localFolders.mapIndexed { i, f -> f.copy(position = i) }
            )
        }
    }

    fun clearError()   { _syncError.value   = null }
    fun clearSuccess() { _syncSuccess.value = null }

    private suspend fun getOrCreateDefaultFolder(): Long {
        val existing = _localFolders.firstOrNull()
        if (existing != null) return existing.id
        return repository.createFolder(Folder(name = "الكل", emoji = "⭐", color = "#FF4444"))
    }

    fun syncSubscriptions() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value  = null
            val folderId = getOrCreateDefaultFolder()
            when (val result = repository.syncSubscriptions(folderId)) {
                is Result.Success -> _syncSuccess.value =
                    if (result.data.isNotEmpty()) "✓ تم استيراد ${result.data.size} قناة جديدة"
                    else "✓ لا توجد قنوات جديدة للاستيراد"
                is Result.Error   -> _syncError.value = "فشل الاستيراد: ${result.message}"
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value  = null
            val folderId = getOrCreateDefaultFolder()
            when (val result = repository.syncPlaylists(folderId)) {
                is Result.Success -> _syncSuccess.value =
                    if (result.data.isNotEmpty()) "✓ تم استيراد ${result.data.size} قائمة تشغيل"
                    else "✓ لا توجد قوائم جديدة"
                is Result.Error   -> _syncError.value = "فشل الاستيراد: ${result.message}"
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value  = null
            val folderId = getOrCreateDefaultFolder()
            val r1 = repository.syncSubscriptions(folderId)
            val r2 = repository.syncPlaylists(folderId)
            val chCount = if (r1 is Result.Success) r1.data.size else 0
            val plCount = if (r2 is Result.Success) r2.data.size else 0
            if (r1 is Result.Error || r2 is Result.Error)
                _syncError.value = "فشل جزئي في الاستيراد، تحقق من الاتصال"
            else
                _syncSuccess.value = "✓ قنوات: $chCount | قوائم: $plCount"
            _isSyncing.value = false
        }
    }
}

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _latestVideos = MutableStateFlow<List<Video>>(emptyList())
    val latestVideos = _latestVideos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<Channel>>(emptyList())
    val availableChannels = _availableChannels.asStateFlow()

    private val _availablePlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    private var _localChannels = mutableListOf<Channel>()
    private var currentFolderId: Long = -1

    fun loadFolder(folderId: Long) {
        currentFolderId = folderId
        viewModelScope.launch {
            repository.getChannelsInFolder(folderId).collect { list ->
                _localChannels = list.toMutableList()
                _channels.value = list
            }
        }
        viewModelScope.launch {
            repository.getPlaylistsInFolder(folderId).collect { list ->
                _playlists.value = list
            }
        }
        refreshVideos()
    }

    fun loadAvailableItems() {
        viewModelScope.launch {
            repository.getAllChannels().collect { all ->
                _availableChannels.value = all.filter { it.folderId != currentFolderId }
            }
        }
        viewModelScope.launch {
            repository.getAllPlaylists().collect { all ->
                _availablePlaylists.value = all.filter { it.folderId != currentFolderId }
            }
        }
    }

    fun addItemsToFolder(channelIds: List<String>, playlistIds: List<String>) {
        viewModelScope.launch {
            channelIds.forEach { id ->
                repository.moveChannel(id, currentFolderId, 0)
            }
            playlistIds.forEach { id ->
                repository.movePlaylist(id, currentFolderId, 0)
            }
            loadFolder(currentFolderId)
        }
    }

    fun removeChannelFromFolder(channel: Channel) {
        viewModelScope.launch {
            repository.deleteChannel(channel)
            loadFolder(currentFolderId)
        }
    }

    fun removePlaylistFromFolder(playlist: Playlist) {
        viewModelScope.launch {
            repository.deletePlaylist(playlist)
            loadFolder(currentFolderId)
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val allVideos = mutableListOf<Video>()
            _localChannels.take(5).forEach { channel ->
                when (val r = repository.getChannelVideos(channel.id)) {
                    is Result.Success -> allVideos.addAll(r.data.take(2))
                    else -> {}
                }
            }
            _latestVideos.value = allVideos.sortedByDescending { it.publishedAt }
            _isLoading.value = false
        }
    }

    fun openChannel(channel: Channel) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/${channel.id}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    // ✅ دالة فتح قائمة التشغيل (إصلاح مشكلة عدم فتح يوتيوب لقوائم التشغيل)[cite: 3]
    fun openPlaylist(playlist: Playlist) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=${playlist.id}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun moveChannel(fromIndex: Int, toIndex: Int) {
        val list = _localChannels.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _localChannels = list
            _channels.value = list.toList()
        }
    }

    fun saveChannelOrder() {
        viewModelScope.launch {
            _localChannels.forEachIndexed { i, ch ->
                repository.moveChannel(ch.id, currentFolderId, i)
            }
        }
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun onGoogleSignInResult(
        account : com.google.android.gms.auth.api.signin.GoogleSignInAccount?,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value     = null
            if (account != null) onSuccess()
            else _error.value = "فشل تسجيل الدخول"
            _isLoading.value = false
        }
    }

    fun onSignInError(message: String) {
        _error.value     = message
        _isLoading.value = false
    }
}