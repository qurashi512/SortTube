package com.grieztech.ytorganizer.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.data.api.Result
import com.grieztech.ytorganizer.data.api.YouTubeRepository
import com.grieztech.ytorganizer.data.remote.RemoteConfigManager
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
//  ✅ Remote Config مدمج: quota safe mode، notice، cooldown، video toggle
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository   : YouTubeRepository,
    private val remoteConfig : RemoteConfigManager,        // ✅ Remote Config
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _folders       = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    private val _defaultFolderId = MutableStateFlow<Long?>(null)
    val defaultFolderId: StateFlow<Long?> = _defaultFolderId.asStateFlow()

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

    // ✅ رسالة الإشعار من Remote Config
    private val _noticeMessage = MutableStateFlow<String?>(null)
    val noticeMessage: StateFlow<String?> = _noticeMessage.asStateFlow()

    val totalFoldersCount: StateFlow<Int> = repository.getFolderCountForAccountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalChannelsCount: StateFlow<Int> = repository.getChannelCountForAccountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPlaylistsCount: StateFlow<Int> = repository.getPlaylistCountForAccountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _folderCreateError = MutableStateFlow<String?>(null)
    val folderCreateError: StateFlow<String?> = _folderCreateError.asStateFlow()

    private var _localFolders  = mutableListOf<Folder>()

    // ✅ آخر وقت تم فيه Sync (لتطبيق Cooldown)
    private var lastSyncTime   = 0L

    init {
        observeFolders()
        loadAllChannelsAndPlaylists()
        fetchRemoteConfig()               // ✅ جلب Remote Config عند البداية
    }

    // ── Remote Config ────────────────────────────────────────────────────

    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            remoteConfig.fetchAndActivate()
            // ✅ عرض رسالة الإشعار إذا كانت مفعّلة
            if (remoteConfig.showNotice && remoteConfig.noticeMessage.isNotBlank()) {
                _noticeMessage.value = remoteConfig.noticeMessage
            }
        }
    }

    fun dismissNotice() { _noticeMessage.value = null }

    // ── Folders & Channels ───────────────────────────────────────────────

    private fun observeFolders() {
        viewModelScope.launch {
            repository.getAllFolders().collect { list ->
                _localFolders = list.toMutableList()
                _folders.value = list
                _defaultFolderId.value = getDefaultFolderId(list)
                refreshChannelCounts(list)
            }
        }
    }

    private fun loadAllChannelsAndPlaylists() {
        viewModelScope.launch { repository.getAllChannels().collect  { _allChannels.value  = it } }
        viewModelScope.launch { repository.getAllPlaylists().collect { _allPlaylists.value = it } }
    }

    private fun getDefaultFolderId(folders: List<Folder>): Long? {
        val allFolderStr = context.getString(R.string.all_folder)
        return folders.firstOrNull {
            it.name.equals("All Channels", ignoreCase = true) ||
                    it.name.equals(allFolderStr, ignoreCase = true) ||
                    it.name == "كل القنوات"
        }?.id ?: folders.minByOrNull { it.id }?.id
    }

    private suspend fun refreshChannelCounts(list: List<Folder>) {
        val counts    = mutableMapOf<Long, Int>()
        val defaultId = getDefaultFolderId(list)
        list.forEach { folder ->
            if (folder.id == defaultId) {
                repository.getAllChannels().take(1).collect { counts[folder.id] = it.size }
            } else {
                repository.getChannelsInFolder(folder.id).take(1).collect { counts[folder.id] = it.size }
            }
        }
        _channelCounts.value = counts
    }

    fun clearFolderCreateError() { _folderCreateError.value = null }
    fun clearError()             { _syncError.value   = null }
    fun clearSuccess()           { _syncSuccess.value = null }

    private fun isNameTaken(name: String, excludeId: Long? = null): Boolean =
        _localFolders.any { it.name.trim().equals(name.trim(), ignoreCase = true) && it.id != excludeId }

    // ── Sync مع Remote Config ─────────────────────────────────────────────

    /**
     * ✅ تحقق من Quota Safe Mode و Cooldown قبل أي Sync
     * يرجع true إذا يجب إيقاف الـ Sync
     */
    private fun shouldBlockSync(): Boolean {
        // وضع توفير الـ Quota مفعّل من Firebase Console
        if (remoteConfig.quotaSafeMode) {
            _syncError.value = "⚠️ وضع توفير الـ Quota مفعّل مؤقتاً، حاول لاحقاً"
            return true
        }
        // Cooldown — منع الـ Sync المتكرر
        val cooldownMs = remoteConfig.syncCooldownHours * 60 * 60 * 1000L
        if (lastSyncTime > 0 && System.currentTimeMillis() - lastSyncTime < cooldownMs) {
            val remaining = ((cooldownMs - (System.currentTimeMillis() - lastSyncTime)) / 60000).toInt()
            _syncError.value = "⏳ انتظر $remaining دقيقة قبل المزامنة مجدداً"
            return true
        }
        return false
    }

    fun syncSubscriptions() {
        if (shouldBlockSync()) return                          // ✅ فحص Remote Config
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            when (val r = repository.syncSubscriptions(folderId)) {
                is Result.Success -> {
                    lastSyncTime = System.currentTimeMillis()  // ✅ تحديث وقت آخر Sync
                    _syncSuccess.value = if (r.data.isNotEmpty())
                        context.getString(R.string.sync_channels_new, r.data.size)
                    else
                        context.getString(R.string.sync_channels_empty)
                }
                is Result.Error -> _syncError.value = r.message
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncPlaylists() {
        if (shouldBlockSync()) return                          // ✅ فحص Remote Config
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            when (val r = repository.syncPlaylists(folderId)) {
                is Result.Success -> {
                    lastSyncTime = System.currentTimeMillis()  // ✅ تحديث وقت آخر Sync
                    _syncSuccess.value = if (r.data.isNotEmpty())
                        context.getString(R.string.sync_playlists_new, r.data.size)
                    else
                        context.getString(R.string.sync_playlists_empty)
                }
                is Result.Error -> _syncError.value = r.message
                else -> {}
            }
            _isSyncing.value = false
        }
    }

    fun syncAll() {
        if (shouldBlockSync()) return                          // ✅ فحص Remote Config
        viewModelScope.launch {
            _isSyncing.value = true; _syncError.value = null
            val folderId = getOrCreateDefaultFolder()
            val r1 = repository.syncSubscriptions(folderId)
            val r2 = repository.syncPlaylists(folderId)
            val ch = if (r1 is Result.Success) r1.data.size else 0
            val pl = if (r2 is Result.Success) r2.data.size else 0

            if (r1 is Result.Error || r2 is Result.Error) {
                _syncError.value = (r1 as? Result.Error)?.message ?: (r2 as? Result.Error)?.message
            } else {
                lastSyncTime = System.currentTimeMillis()      // ✅ تحديث وقت آخر Sync
                _syncSuccess.value = context.getString(R.string.sync_all_success, ch, pl)
            }
            _isSyncing.value = false
        }
    }

    // ── Folders CRUD ──────────────────────────────────────────────────────

    fun checkItemsBeforeCreatingFolder(
        name       : String,           // ✅ مطلوب للتحقق من الاسم أولاً
        channelIds : List<String>,
        playlistIds: List<String>,
        onWarningNeeded: (String) -> Unit,
        onProceed      : () -> Unit
    ) {
        viewModelScope.launch {
            // ✅ فحص الاسم أولاً — إذا مكرر نوقف هنا قبل أي شيء آخر
            if (isNameTaken(name)) {
                _folderCreateError.value = context.getString(R.string.folder_exists_error)
                return@launch
            }

            val defaultId = getDefaultFolderId(_localFolders)
            val allCh = repository.getAllChannels().first()
            val allPl = repository.getAllPlaylists().first()

            val chToMove = allCh.filter { it.id in channelIds  && it.folderId != defaultId }
            val plToMove = allPl.filter { it.id in playlistIds && it.folderId != defaultId }

            if (chToMove.isNotEmpty() || plToMove.isNotEmpty()) {
                val folderIds = (chToMove.map { it.folderId } + plToMove.map { it.folderId }).distinct()
                val folderNames = _localFolders.filter { it.id in folderIds }.map {
                    if (it.name.equals("All Channels", ignoreCase = true)) context.getString(R.string.all_folder) else it.name
                }.joinToString("، ")
                // ✅ رسالة بالعربية
                val msg = "بعض العناصر المختارة موجودة بالفعل في: $folderNames\nهل تريد نقلها إلى هنا؟"
                onWarningNeeded(msg)
            } else {
                onProceed()
            }
        }
    }

    fun createFolderWithItems(
        name: String, emoji: String, color: String,
        channelIds: List<String>, playlistIds: List<String>,
        onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                if (isNameTaken(name)) {
                    val msg = context.getString(R.string.folder_exists_error)
                    _folderCreateError.value = msg; onError(msg); return@launch
                }
                val folderId = repository.createFolder(Folder(name=name.trim(), emoji=emoji, color=color, position=_localFolders.size))
                channelIds.forEachIndexed  { i, id -> repository.moveChannel(id,  folderId, i) }
                playlistIds.forEachIndexed { i, id -> repository.movePlaylist(id, folderId, i) }
                onSuccess()
            } catch (e: Exception) {
                val msg = context.getString(R.string.folder_create_failed, e.message ?: "")
                _folderCreateError.value = msg; onError(msg)
            }
        }
    }

    fun updateFolder(folder: Folder) = viewModelScope.launch {
        if (isNameTaken(folder.name, excludeId = folder.id)) {
            _syncError.value = context.getString(R.string.folder_exists_error); return@launch
        }
        repository.updateFolder(folder.copy(name = folder.name.trim()))
    }

    fun deleteFolder(folder: Folder)  = viewModelScope.launch { repository.deleteFolder(folder) }

    fun moveFolder(from: Int, to: Int) {
        val list = _localFolders.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from); list.add(to, item)
            _localFolders = list; _folders.value = list.toList()
        }
    }

    fun saveFolderOrder() = viewModelScope.launch {
        repository.reorderFolders(_localFolders.mapIndexed { i, f -> f.copy(position = i) })
    }

    private suspend fun getOrCreateDefaultFolder(): Long {
        val defaultId = getDefaultFolderId(_localFolders)
        if (defaultId != null) return defaultId
        return repository.createFolder(Folder(name="All Channels", emoji="📺", color="#FF4444", position=0))
    }

    // ── Account ───────────────────────────────────────────────────────────

    fun logout(email: String? = null, onDone: () -> Unit) {
        viewModelScope.launch {
            val currentEmail = email ?: GoogleSignIn.getLastSignedInAccount(context)?.email ?: ""
            if (currentEmail.isNotEmpty()) {
                context.getSharedPreferences("grieztech_prefs", Context.MODE_PRIVATE)
                    .edit().putString("last_account_id", currentEmail).apply()
            }
            onDone()
        }
    }

    fun checkAndClearIfAccountChanged(newAccountId: String) {
        viewModelScope.launch {
            if (newAccountId.isEmpty()) return@launch
            repository.claimOrphanedData(newAccountId)
            val prefs = context.getSharedPreferences("grieztech_prefs", Context.MODE_PRIVATE)
            val previousAccountId = prefs.getString("last_account_id", "") ?: ""
            repository.handleAccountSwitch(previousAccountId, newAccountId)
            prefs.edit().putString("last_account_id", newAccountId).apply()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FolderDetailViewModel — مع Remote Config لعرض الفيديوهات
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    private val repository   : YouTubeRepository,
    private val remoteConfig : RemoteConfigManager,        // ✅ Remote Config
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _channels           = MutableStateFlow<List<Channel>>(emptyList())
    val channels = _channels.asStateFlow()

    private val _playlists          = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists = _playlists.asStateFlow()

    private val _latestVideos       = MutableStateFlow<List<Video>>(emptyList())
    val latestVideos = _latestVideos.asStateFlow()

    private val _isLoading          = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _availableChannels  = MutableStateFlow<List<Channel>>(emptyList())
    val availableChannels = _availableChannels.asStateFlow()

    private val _availablePlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val availablePlaylists = _availablePlaylists.asStateFlow()

    private val _isDefaultFolder    = MutableStateFlow(false)
    val isDefaultFolder = _isDefaultFolder.asStateFlow()

    // ✅ هل تعرض الفيديوهات؟ يتحكم فيها Remote Config
    val showVideos: Boolean
        get() = remoteConfig.showChannelVideos

    private var _localChannels      = mutableListOf<Channel>()
    private var currentFolderId     : Long = -1
    private var folderJob: kotlinx.coroutines.Job? = null

    private suspend fun getDefaultFolderId(): Long? {
        val folders      = repository.getAllFolders().first()
        val allFolderStr = context.getString(R.string.all_folder)
        return folders.firstOrNull {
            it.name.equals("All Channels", ignoreCase = true) ||
                    it.name.equals(allFolderStr, ignoreCase = true) ||
                    it.name == "كل القنوات"
        }?.id ?: folders.minByOrNull { it.id }?.id
    }

    fun loadFolder(folderId: Long) {
        currentFolderId = folderId
        folderJob?.cancel()
        folderJob = viewModelScope.launch {
            val defaultId = getDefaultFolderId()
            _isDefaultFolder.value = (folderId == defaultId)

            launch {
                if (folderId == defaultId) {
                    repository.getAllChannels().collect { list ->
                        _localChannels = list.toMutableList()
                        _channels.value = list
                        // ✅ جلب الفيديوهات فقط إذا مفعّلة من Remote Config
                        if (remoteConfig.showChannelVideos) refreshVideosInternal()
                    }
                } else {
                    repository.getChannelsInFolder(folderId).collect { list ->
                        _localChannels = list.toMutableList()
                        _channels.value = list
                        // ✅ جلب الفيديوهات فقط إذا مفعّلة من Remote Config
                        if (remoteConfig.showChannelVideos) refreshVideosInternal()
                    }
                }
            }
            launch {
                if (folderId == defaultId) {
                    repository.getAllPlaylists().collect { _playlists.value = it }
                } else {
                    repository.getPlaylistsInFolder(folderId).collect { _playlists.value = it }
                }
            }
        }
    }

    private fun refreshVideosInternal() {
        // ✅ إذا أطفأ Remote Config الفيديوهات — لا تجلب
        if (!remoteConfig.showChannelVideos) {
            _latestVideos.value = emptyList()
            return
        }
        val all = mutableListOf<Video>()
        viewModelScope.launch {
            _isLoading.value = true
            // ✅ عدد الفيديوهات من Remote Config بدل ما تكون hardcoded
            val maxPerChannel = (remoteConfig.maxVideoResults / 5).coerceAtLeast(1)
            _localChannels.take(5).forEach { ch ->
                when (val r = repository.getChannelVideos(ch.id)) {
                    is Result.Success -> all.addAll(r.data.take(maxPerChannel))
                    else -> {}
                }
            }
            _latestVideos.value = all.sortedByDescending { it.publishedAt }
            _isLoading.value = false
        }
    }

    fun refreshVideos() {
        if (remoteConfig.showChannelVideos) refreshVideosInternal()  // ✅ فحص Remote Config
    }

    fun loadAvailableItems() {
        viewModelScope.launch {
            repository.getAllChannels().collect  { all -> _availableChannels.value  = all.filter { it.folderId != currentFolderId } }
        }
        viewModelScope.launch {
            repository.getAllPlaylists().collect { all -> _availablePlaylists.value = all.filter { it.folderId != currentFolderId } }
        }
    }

    fun checkItemsBeforeAdding(
        channelIds : List<String>,
        playlistIds: List<String>,
        onWarningNeeded: (String) -> Unit,
        onProceed      : () -> Unit
    ) {
        viewModelScope.launch {
            val folders   = repository.getAllFolders().first()
            val defaultId = getDefaultFolderId()
            val allCh     = repository.getAllChannels().first()
            val allPl     = repository.getAllPlaylists().first()

            val chToMove = allCh.filter { it.id in channelIds  && it.folderId != defaultId && it.folderId != currentFolderId }
            val plToMove = allPl.filter { it.id in playlistIds && it.folderId != defaultId && it.folderId != currentFolderId }

            if (chToMove.isNotEmpty() || plToMove.isNotEmpty()) {
                val folderIds   = (chToMove.map { it.folderId } + plToMove.map { it.folderId }).distinct()
                val folderNames = folders.filter { it.id in folderIds }.map {
                    if (it.name.equals("All Channels", ignoreCase = true)) context.getString(R.string.all_folder) else it.name
                }.joinToString("، ")
                onWarningNeeded(context.getString(R.string.items_already_exist_warning, folderNames))
            } else {
                onProceed()
            }
        }
    }

    fun addItemsToFolder(channelIds: List<String>, playlistIds: List<String>) {
        viewModelScope.launch {
            channelIds.forEach  { id -> repository.moveChannel(id,  currentFolderId, 0) }
            playlistIds.forEach { id -> repository.movePlaylist(id, currentFolderId, 0) }
        }
    }

    fun removeChannelFromFolder(channel: Channel) = viewModelScope.launch {
        val defaultId = getDefaultFolderId()
        if (currentFolderId == defaultId) repository.deleteChannel(channel)
        else if (defaultId != null) repository.moveChannel(channel.id, defaultId, 0)
    }

    fun removePlaylistFromFolder(playlist: Playlist) = viewModelScope.launch {
        val defaultId = getDefaultFolderId()
        if (currentFolderId == defaultId) repository.deletePlaylist(playlist)
        else if (defaultId != null) repository.movePlaylist(playlist.id, defaultId, 0)
    }

    fun openChannel(channel: Channel) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/channel/${channel.id}"))
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )
    }

    fun openPlaylist(playlist: Playlist) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/playlist?list=${playlist.id}"))
                .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )
    }

    fun moveChannel(from: Int, to: Int) {
        val list = _localChannels.toMutableList()
        if (from in list.indices && to in list.indices) {
            val item = list.removeAt(from); list.add(to, item)
            _localChannels = list; _channels.value = list.toList()
        }
    }

    fun saveChannelOrder() = viewModelScope.launch {
        repository.reorderChannels(_localChannels.toList())
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LoginViewModel
// ═══════════════════════════════════════════════════════════════════════════

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun onGoogleSignInResult(account: GoogleSignInAccount?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true; _error.value = null
            if (account != null) onSuccess()
            else _error.value = context.getString(R.string.login_failed)
            _isLoading.value = false
        }
    }

    fun onSignInError(msg: String) { _error.value = msg; _isLoading.value = false }
}
