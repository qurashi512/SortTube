package com.grieztech.ytorganizer.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.grieztech.ytorganizer.R
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

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - ViewModels
//  ✅ FIX: كل العمليات تعمل عبر Repository الذي يُقيِّد بـ accountId تلقائياً
// ═══════════════════════════════════════════════════════════════════════════

// ── Home ViewModel ──────────────────────────────────────────────────────────
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _folders       = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _channelCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val channelCounts: StateFlow<Map<Long, Int>> = _channelCounts.asStateFlow()

    private val _isSyncing     = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError     = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _syncSuccess   = MutableStateFlow<String?>(null)
    val syncSuccess: StateFlow<String?> = _syncSuccess.asStateFlow()

    private val _allChannels   = MutableStateFlow<List<Channel>>(emptyList())
    val allChannels: StateFlow<List<Channel>> = _allChannels.asStateFlow()

    private val _allPlaylists  = MutableStateFlow<List<Playlist>>(emptyList())
    val allPlaylists: StateFlow<List<Playlist>> = _allPlaylists.asStateFlow()

    private val _folderCreateError = MutableStateFlow<String?>(null)
    val folderCreateError: StateFlow<String?> = _folderCreateError.asStateFlow()

    private var _localFolders  = mutableListOf<Folder>()

    init {
        observeFolders()
        loadAllChannelsAndPlaylists()
    }

    private fun observeFolders() {
        viewModelScope.launch {
            // ✅ Repository يُرجع فقط مجلدات الحساب الحالي تلقائياً
            repository.getAllFolders().collect { list ->
                _localFolders = list.toMutableList()
                _folders.value = list
                refreshChannelCounts(list)
            }
        }
    }

    private fun loadAllChannelsAndPlaylists() {
        viewModelScope.launch {
            repository.getAllChannels().collect { _allChannels.value = it }
        }
        viewModelScope.launch {
            repository.getAllPlaylists().collect { _allPlaylists.value = it }
        }
    }

    private suspend fun refreshChannelCounts(list: List<Folder>) {
        val counts = mutableMapOf<Long, Int>()
        list.forEach { folder ->
            repository.getChannelsInFolder(folder.id).take(1)
                .collect { counts[folder.id] = it.size }
        }
        _channelCounts.value = counts
    }

    fun clearFolderCreateError() { _folderCreateError.value = null }
    fun clearError()             { _syncError.value   = null }
    fun clearSuccess()           { _syncSuccess.value = null }

    fun createFolder(name: String, emoji: String, color: String) {
        viewModelScope.launch {
            // ✅ Repository يضيف accountId تلقائياً
            repository.createFolder(
                Folder(name=name, emoji=emoji, color=color, position=_localFolders.size)
            )
        }
    }

    fun createFolderWithItems(
        name       : String,
        emoji      : String,
        color      : String,
        channelIds : List<String>,
        playlistIds: List<String>,
        onSuccess  : () -> Unit = {},
        onError    : (String) -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val folderId = repository.createFolder(
                    Folder(name=name, emoji=emoji, color=color, position=_localFolders.size)
                )
                channelIds.forEachIndexed  { i, id -> repository.moveChannel(id, folderId, i) }
                playlistIds.forEachIndexed { i, id -> repository.movePlaylist(id, folderId, i) }
                onSuccess()
            } catch (e: Exception) {
                val msg = "فشل إنشاء المجلد: ${e.message}"
                _folderCreateError.value = msg
                onError(msg)
            }
        }
    }

    fun updateFolder(folder: Folder) = viewModelScope.launch { repository.updateFolder(folder) }
    fun deleteFolder(folder: Folder) = viewModelScope.launch { repository.deleteFolder(folder) }

    fun moveFolder(from: Int, to: Int) {
        val list = _localFolders.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from); list.add(to, item)
            _localFolders = list; _folders.value = list.toList()
        }
    }

    fun saveFolderOrder() = viewModelScope.launch {
        repository.reorderFolders(_localFolders.mapIndexed { i, f -> f.copy(position=i) })
    }

    private suspend fun getOrCreateDefaultFolder(): Long {
        val existing = _localFolders.firstOrNull()
        if (existing != null) return existing.id
        val name = try { context.getString(R.string.all_folder) } catch (e: Exception) { "كل القنوات" }
        return repository.createFolder(Folder(name=name, emoji="⭐", color="#FF4444"))
    }

    fun syncSubscriptions() {
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            when (val r = repository.syncSubscriptions(folderId)) {
                is Result.Success -> _syncSuccess.value =
                    if (r.data.isNotEmpty()) "✓ تم استيراد ${r.data.size} قناة جديدة"
                    else "✓ لا توجد قنوات جديدة"
                is Result.Error   -> _syncError.value = "فشل الاستيراد: ${r.message}"
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncPlaylists() {
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            when (val r = repository.syncPlaylists(folderId)) {
                is Result.Success -> _syncSuccess.value =
                    if (r.data.isNotEmpty()) "✓ تم استيراد ${r.data.size} قائمة"
                    else "✓ لا توجد قوائم جديدة"
                is Result.Error   -> _syncError.value = "فشل الاستيراد: ${r.message}"
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            val r1 = repository.syncSubscriptions(folderId)
            val r2 = repository.syncPlaylists(folderId)
            val ch = if (r1 is Result.Success) r1.data.size else 0
            val pl = if (r2 is Result.Success) r2.data.size else 0
            if (r1 is Result.Error || r2 is Result.Error)
                _syncError.value = "فشل جزئي في الاستيراد، تحقق من الاتصال"
            else
                _syncSuccess.value = "✓ $ch قناة و $pl قائمة"
            _isSyncing.value = false
        }
    }

    // ✅ تسجيل الخروج — البيانات تبقى محفوظة، فقط نسجّل خروج Google
    //    البيانات لا تُمسح أبداً عند الخروج، فقط عند تسجيل دخول حساب مختلف
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            // لا نمسح أي بيانات هنا — البيانات تبقى مرتبطة بـ accountId
            // سيراها المستخدم فور تسجيل الدخول بنفس الحساب مجدداً
            onDone()
        }
    }

    // ✅ يُستدعى فور نجاح تسجيل الدخول
    //    إذا تغيّر الحساب عن السابق → يمسح بيانات الحساب القديم
    //    إذا نفس الحساب → لا يفعل شيئاً والمجلدات تبقى كما هي
    fun onLoginSuccess(newAccountId: String) {
        viewModelScope.launch {
            val previousAccountId = context
                .getSharedPreferences("grieztech_prefs", android.content.Context.MODE_PRIVATE)
                .getString("last_account_id", "") ?: ""

            // إذا تغيّر الحساب، امسح بيانات الحساب القديم
            repository.handleAccountSwitch(previousAccountId, newAccountId)

            // احفظ الحساب الجديد
            context.getSharedPreferences("grieztech_prefs", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("last_account_id", newAccountId)
                .apply()
        }
    }
}

// ── Folder Detail ViewModel ─────────────────────────────────────────────────
@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository: YouTubeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _channels          = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _playlists         = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _latestVideos      = MutableStateFlow<List<Video>>(emptyList())
    val latestVideos = _latestVideos.asStateFlow()

    private val _isLoading         = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _availableChannels = MutableStateFlow<List<Channel>>(emptyList())
    val availableChannels = _availableChannels.asStateFlow()

    private val _availablePlaylists= MutableStateFlow<List<Playlist>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    private var _localChannels     = mutableListOf<Channel>()
    private var currentFolderId    : Long = -1

    fun loadFolder(folderId: Long) {
        currentFolderId = folderId
        viewModelScope.launch {
            // ✅ Repository يُرجع قنوات هذا الحساب فقط
            repository.getChannelsInFolder(folderId).collect { list ->
                _localChannels = list.toMutableList()
                _channels.value = list
            }
        }
        viewModelScope.launch {
            repository.getPlaylistsInFolder(folderId).collect { _playlists.value = it }
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
            channelIds.forEach  { id -> repository.moveChannel(id,  currentFolderId, 0) }
            playlistIds.forEach { id -> repository.movePlaylist(id, currentFolderId, 0) }
            loadFolder(currentFolderId)
        }
    }

    fun removeChannelFromFolder(channel: Channel) = viewModelScope.launch {
        repository.deleteChannel(channel); loadFolder(currentFolderId)
    }

    fun removePlaylistFromFolder(playlist: Playlist) = viewModelScope.launch {
        repository.deletePlaylist(playlist); loadFolder(currentFolderId)
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val all = mutableListOf<Video>()
            _localChannels.take(5).forEach { ch ->
                when (val r = repository.getChannelVideos(ch.id)) {
                    is Result.Success -> all.addAll(r.data.take(2))
                    else -> {}
                }
            }
            _latestVideos.value = all.sortedByDescending { it.publishedAt }
            _isLoading.value = false
        }
    }

    fun openChannel(channel: Channel) {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/channel/${channel.id}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    fun openPlaylist(playlist: Playlist) {
        context.startActivity(Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/playlist?list=${playlist.id}")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK })
    }

    fun moveChannel(from: Int, to: Int) {
        val list = _localChannels.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from); list.add(to, item)
            _localChannels = list; _channels.value = list.toList()
        }
    }

    fun saveChannelOrder() = viewModelScope.launch {
        _localChannels.forEachIndexed { i, ch -> repository.moveChannel(ch.id, currentFolderId, i) }
    }
}

// ── Login ViewModel ─────────────────────────────────────────────────────────
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
            _isLoading.value = true; _error.value = null
            if (account != null) onSuccess()
            else _error.value = "فشل تسجيل الدخول"
            _isLoading.value = false
        }
    }

    fun onSignInError(msg: String) { _error.value = msg; _isLoading.value = false }
}
