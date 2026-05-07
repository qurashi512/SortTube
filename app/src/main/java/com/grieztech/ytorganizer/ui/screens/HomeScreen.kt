package com.grieztech.ytorganizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.ui.components.*
import com.grieztech.ytorganizer.ui.theme.*
import com.grieztech.ytorganizer.ui.viewmodel.HomeViewModel
import org.burnoutcrew.reorderable.*

// كلاس مساعد لحفظ بيانات المجلد المؤقتة قبل التأكيد
data class PendingFolder(
    val name: String, val emoji: String, val color: String,
    val chIds: List<String>, val plIds: List<String>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onFolderClick  : (Folder) -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutSuccess: () -> Unit = {},
    viewModel      : HomeViewModel = hiltViewModel(),
) {
    val context       = LocalContext.current

    val rawFolders    by viewModel.folders.collectAsState()
    val allFolderStr  = stringResource(id = R.string.all_folder)
    val folders       = remember(rawFolders, allFolderStr) {
        rawFolders.map {
            if (it.name.equals("All Channels", ignoreCase = true)) it.copy(name = allFolderStr) else it
        }
    }

    val channelCounts by viewModel.channelCounts.collectAsState()
    val isSyncing     by viewModel.isSyncing.collectAsState()
    val syncError     by viewModel.syncError.collectAsState()
    val syncSuccess   by viewModel.syncSuccess.collectAsState()
    val allChannels   by viewModel.allChannels.collectAsState()
    val allPlaylists  by viewModel.allPlaylists.collectAsState()
    val folderError   by viewModel.folderCreateError.collectAsState()

    val totalFolders   by viewModel.totalFoldersCount.collectAsState()
    val totalChannels  by viewModel.totalChannelsCount.collectAsState()
    val totalPlaylists by viewModel.totalPlaylistsCount.collectAsState()

    // ✅ Remote Config — رسالة الإشعار
    val noticeMessage  by viewModel.noticeMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var mainSearchQuery by remember { mutableStateOf("") }
    val filteredFolders = remember(folders, mainSearchQuery) {
        if (mainSearchQuery.isBlank()) folders
        else folders.filter { it.name.contains(mainSearchQuery, ignoreCase = true) }
    }

    val account     = remember { GoogleSignIn.getLastSignedInAccount(context) }
    val displayName = account?.displayName ?: account?.email ?: "GriezTech"
    val userEmail   = account?.email ?: ""
    val photoUrl    = account?.photoUrl

    val googleSignInClient = remember {
        GoogleSignIn.getClient(context, GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build())
    }

    LaunchedEffect(syncError) {
        syncError?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long); viewModel.clearError() }
    }
    LaunchedEffect(syncSuccess) {
        syncSuccess?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short); viewModel.clearSuccess() }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove    = { from, to -> viewModel.moveFolder(from.index, to.index) },
        onDragEnd = { _, _ -> viewModel.saveFolderOrder() },
    )

    var showAddDialog   by remember { mutableStateOf(false) }
    var showSyncOptions by remember { mutableStateOf(false) }
    var folderToDelete  by remember { mutableStateOf<Folder?>(null) }
    var folderToEdit    by remember { mutableStateOf<Folder?>(null) }
    var showUserMenu    by remember { mutableStateOf(false) }

    // ✅ متغيرات نافذة التحذير الذكية
    var moveWarningMessage by remember { mutableStateOf("") }
    var pendingFolder by remember { mutableStateOf<PendingFolder?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.radialGradient(
                colors = listOf(GradientEnd.copy(alpha = 0.95f), GradientStart, GradientMid),
                center = Offset(size.width * 0.3f, size.height * 0.2f), radius = size.width * 1.2f))
        }

        Scaffold(
            snackbarHost   = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
            floatingActionButton = {
                GlassFAB(onClick = { showAddDialog = true }, icon = Icons.Rounded.CreateNewFolder,
                    label = stringResource(R.string.new_folder), modifier = Modifier.padding(bottom = 16.dp))
            },
            bottomBar = {
                FloatingBottomBar(
                    onHomeClick = { },
                    onProfileClick = { showUserMenu = true },
                    onSettingsClick = onSettingsClick
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 0.dp, glassAlpha = 0.10f) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (photoUrl != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(context).data(photoUrl).crossfade(true).build(),
                                        contentDescription = "صورة الحساب", contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).clip(CircleShape).background(Brush.linearGradient(listOf(AccentPurple, YouTubeRed)))) {
                                        Text(displayName.firstOrNull()?.toString() ?: "G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }

                                Column {
                                    Text(displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold)
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(0.5f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = AccentPurple, strokeWidth = 2.dp)
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { showSyncOptions = true }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Rounded.Sync, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatItem(totalFolders.toString(),  stringResource(R.string.folder_word))
                            StatItem(totalChannels.toString(), stringResource(R.string.channel_word))
                            StatItem(totalPlaylists.toString(),stringResource(R.string.playlist_word))
                        }

                        OutlinedTextField(
                            value = mainSearchQuery, onValueChange = { mainSearchQuery = it },
                            placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onBackground.copy(0.5f), fontSize = 13.sp) },
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onBackground.copy(0.5f), modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth().height(54.dp), shape = CircleShape, singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor   = AccentPurple, unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(0.2f),
                                focusedTextColor     = MaterialTheme.colorScheme.onBackground, unfocusedTextColor   = MaterialTheme.colorScheme.onBackground,
                            )
                        )
                    }
                }

                // ✅ Notice Card من Remote Config
                noticeMessage?.let { msg ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors   = CardDefaults.cardColors(containerColor = AccentPurple.copy(alpha = 0.15f)),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = AccentPurple)
                            Spacer(Modifier.width(8.dp))
                            Text(msg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground)
                            IconButton(onClick = { viewModel.dismissNotice() }) {
                                Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onBackground.copy(0.6f))
                            }
                        }
                    }
                }

                if (folders.isEmpty() && !isSyncing) {
                    EmptyState(onAddFolder = { showAddDialog = true }, onSyncChannels = { showSyncOptions = true })
                } else {
                    LazyColumn(state = reorderState.listState, contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize().reorderable(reorderState)) {
                        items(filteredFolders, key = { it.id }) { folder ->
                            ReorderableItem(reorderState, key = folder.id) { isDragging ->
                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = {
                                        if (it == SwipeToDismissBoxValue.EndToStart) { folderToDelete = folder; false } else false
                                    }
                                )
                                SwipeToDismissBox(
                                    state = dismissState, enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                                        if (isSwiping) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(Color(0xFFAA0000), Color(0xFFEE3333)))).padding(end = 28.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    Icon(Icons.Rounded.Delete, null, tint = Color.White, modifier = Modifier.size(24.dp))
                                                    Text(stringResource(R.string.delete), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    },
                                    content = {
                                        FolderCard(
                                            folder = folder, channelCount = channelCounts[folder.id] ?: 0, isDragging = isDragging,
                                            onClick = { onFolderClick(folder) }, onLongPress = { folderToEdit = folder },
                                            modifier = Modifier.detectReorderAfterLongPress(reorderState),
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showUserMenu) {
        AlertDialog(
            onDismissRequest = { showUserMenu = false }, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(28.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Person, null, tint = AccentPurple)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.account), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(displayName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(userEmail, color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 14.sp)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(0.1f))
                    Button(
                        onClick = {
                            viewModel.logout {
                                googleSignInClient.signOut().addOnCompleteListener {
                                    showUserMenu = false
                                    onLogoutSuccess()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)), shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Logout, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.logout), fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUserMenu = false }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) } }
        )
    }

    if (showAddDialog) {
        AddFolderDialog(
            allChannels = allChannels, allPlaylists = allPlaylists, externalError = folderError,
            onClearError = { viewModel.clearFolderCreateError() },
            onConfirm = { name, emoji, color, chIds, plIds ->
                // ✅ استدعاء التحقق الذكي قبل الحفظ الفعلي
                viewModel.checkItemsBeforeCreatingFolder(chIds, plIds,
                    onWarningNeeded = { msg ->
                        moveWarningMessage = msg
                        pendingFolder = PendingFolder(name, emoji, color, chIds, plIds)
                        showAddDialog = false
                    },
                    onProceed = {
                        viewModel.createFolderWithItems(name, emoji, color, chIds, plIds, onSuccess = { showAddDialog = false })
                    }
                )
            },
            onDismiss = { showAddDialog = false; viewModel.clearFolderCreateError() }
        )
    }

    // ✅ نافذة التنبيه الذكية لنقل القنوات من مجلدات أخرى (في الشاشة الرئيسية)
    pendingFolder?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingFolder = null },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(24.dp),
            title = { Text(stringResource(R.string.confirm_move_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = { Text(moveWarningMessage, color = MaterialTheme.colorScheme.onSurface.copy(0.8f)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createFolderWithItems(pending.name, pending.emoji, pending.color, pending.chIds, pending.plIds, onSuccess = { pendingFolder = null })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) { Text(stringResource(R.string.move_action), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { pendingFolder = null }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) }
            }
        )
    }

    folderToEdit?.let { folder ->
        EditFolderDialog(folder = folder, onConfirm = { n, e -> viewModel.updateFolder(folder.copy(name=n, emoji=e)); folderToEdit = null }, onDismiss = { folderToEdit = null })
    }

    folderToDelete?.let { folder ->
        AlertDialog(onDismissRequest = { folderToDelete = null }, title = { Text(stringResource(R.string.delete_folder_title)) },
            text = { Text(stringResource(R.string.cannot_be_undone)) },
            confirmButton = { Button(onClick = { viewModel.deleteFolder(folder); folderToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text(stringResource(R.string.delete), color = Color.White) } },
            dismissButton = { TextButton(onClick = { folderToDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showSyncOptions) SyncOptionsDialog(onDismiss = { showSyncOptions = false }, onSyncSubscriptions = { viewModel.syncSubscriptions(); showSyncOptions = false }, onSyncPlaylists = { viewModel.syncPlaylists(); showSyncOptions = false }, onSyncAll = { viewModel.syncAll(); showSyncOptions = false })
}

@Composable
private fun FloatingBottomBar(onHomeClick: () -> Unit, onProfileClick: () -> Unit, onSettingsClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).navigationBarsPadding(), contentAlignment = Alignment.Center) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, shadowElevation = 8.dp) {
            Row(modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onHomeClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Home, null, tint = AccentPurple, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = onProfileClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.AccountCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(26.dp)) }
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(40.dp)) { Icon(Icons.Rounded.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(26.dp)) }
            }
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = MaterialTheme.colorScheme.onBackground.copy(0.6f), fontSize = 11.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFolderDialog(
    allChannels: List<Channel>, allPlaylists: List<Playlist>,
    externalError: String?, onClearError: () -> Unit,
    onConfirm: (String, String, String, List<String>, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }
    var selectedColor by remember { mutableStateOf("#9B6FFF") }
    var selectedChannels by remember { mutableStateOf(setOf<String>()) }
    var selectedPlaylists by remember { mutableStateOf(setOf<String>()) }
    var currentTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val emojis = listOf("📁","🎵","🎮","📚","🏋️","🍳","✈️","💻","🎬","🔬","📰","🎨","🌍","📈","📖","⚽","🏀","🍔","🍕","🚗","🚀","🔥","✨","💡")
    val colors = listOf("#FF4444","#FF8800","#FFCC00","#44CC88","#4488FF","#8855FF","#FF44AA","#00CCBB")

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(stringResource(R.string.new_folder), color = MaterialTheme.colorScheme.onSurface)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    TabChip(stringResource(R.string.basic_tab), currentTab == 0) { currentTab = 0; searchQuery = "" }
                    TabChip(stringResource(R.string.channels_tab), currentTab == 1) { currentTab = 1; searchQuery = "" }
                    TabChip(stringResource(R.string.playlists_tab), currentTab == 2) { currentTab = 2; searchQuery = "" }
                }
            }
        },
        text = {
            Box(Modifier.height(400.dp)) {
                when (currentTab) {
                    0 -> Column(Modifier.verticalScroll(rememberScrollState())) {
                        Text(stringResource(R.string.choose_emoji), color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                        FlowRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            emojis.forEach { e -> FilterChip(selected = e == selectedEmoji, onClick = { selectedEmoji = e }, label = { Text(e, fontSize = 18.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPurple.copy(0.35f), containerColor = MaterialTheme.colorScheme.onSurface.copy(0.06f))) }
                        }
                        OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text(stringResource(R.string.folder_name)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface))
                        Text(stringResource(R.string.choose_color), color = MaterialTheme.colorScheme.onSurface.copy(0.6f), modifier = Modifier.padding(top = 10.dp), fontSize = 12.sp)
                        androidx.compose.foundation.lazy.LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(colors) { hex ->
                                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Red }
                                Box(
                                    modifier = Modifier.size(36.dp).background(c, CircleShape)
                                        .border(width = if (hex == selectedColor) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }
                    }
                    1 -> Column {
                        SearchBar(searchQuery) { searchQuery = it }
                        val filtered = allChannels.filter { it.title.contains(searchQuery, true) }
                        LazyColumn(Modifier.weight(1f)) { items(filtered) { ch -> SelectableItem(ch.title, ch.id in selectedChannels) { selectedChannels = if (ch.id in selectedChannels) selectedChannels - ch.id else selectedChannels + ch.id } } }
                    }
                    2 -> Column {
                        SearchBar(searchQuery) { searchQuery = it }
                        val filtered = allPlaylists.filter { it.title.contains(searchQuery, true) }
                        LazyColumn(Modifier.weight(1f)) { items(filtered) { pl -> SelectableItem(pl.title, pl.id in selectedPlaylists) { selectedPlaylists = if (pl.id in selectedPlaylists) selectedPlaylists - pl.id else selectedPlaylists + pl.id } } }
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (externalError != null) {
                    val displayError = if (externalError.contains("already exists", ignoreCase = true)) stringResource(R.string.folder_exists_error) else externalError
                    Text(displayError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
                Button(onClick = { onClearError(); onConfirm(folderName, selectedEmoji, selectedColor, selectedChannels.toList(), selectedPlaylists.toList()) }, enabled = folderName.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) { Text(stringResource(R.string.create), color = Color.White) }
            }
        },
        dismissButton = { TextButton(onClick = { onClearError(); onDismiss() }) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditFolderDialog(folder: Folder, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var folderName by remember { mutableStateOf(folder.name) }
    var selectedEmoji by remember { mutableStateOf(folder.emoji) }
    val emojis = listOf("📁","🎵","🎮","📚","🏋️","🍳","✈️","💻","🎬","🔬","📰","🎨","🌍","📈","📖","⚽","🏀","🍔","🍕","🚗","🚀","🔥","✨","💡")
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.edit_folder), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(value = folderName, onValueChange = { folderName = it }, label = { Text(stringResource(R.string.folder_name)) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface))
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.choose_emoji), color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 12.sp)
                FlowRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    emojis.forEach { e -> FilterChip(selected = e == selectedEmoji, onClick = { selectedEmoji = e }, label = { Text(e, fontSize = 18.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AccentPurple.copy(0.35f), containerColor = MaterialTheme.colorScheme.onSurface.copy(0.06f))) }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(folderName, selectedEmoji) }, enabled = folderName.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) { Text(stringResource(R.string.save), color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) } }
    )
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(value = query, onValueChange = onQueryChange, placeholder = { Text(stringResource(R.string.search_hint), color = MaterialTheme.colorScheme.onSurface.copy(0.4f)) }, leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.4f)) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), singleLine = true, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface, focusedBorderColor = AccentPurple, unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(0.1f)))
}

@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, color = if (selected) AccentPurple else MaterialTheme.colorScheme.onSurface.copy(0.05f), shape = RoundedCornerShape(8.dp), modifier = Modifier.height(35.dp)) { Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) { Text(label, color = if(selected) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) } }
}

@Composable
private fun SelectableItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = selected, onCheckedChange = { onClick() }, colors = CheckboxDefaults.colors(checkedColor = AccentPurple, uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(0.3f)))
        Text(title, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
    }
}

@Composable
private fun SyncOptionsDialog(onDismiss: () -> Unit, onSyncSubscriptions: () -> Unit, onSyncPlaylists: () -> Unit, onSyncAll: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp),
        title = { Text(stringResource(R.string.import_from_youtube), color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onSyncSubscriptions, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.subscribed_channels)) }
                OutlinedButton(onClick = onSyncPlaylists, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.QueueMusic, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.playlists)) }
                Button(onClick = onSyncAll, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) { Icon(Icons.Rounded.Sync, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_all), color = Color.White) }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurface.copy(0.6f)) } }
    )
}

@Composable
private fun EmptyState(onAddFolder: () -> Unit, onSyncChannels: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📺", fontSize = 60.sp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_folders_yet), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onSyncChannels, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) { Icon(Icons.Rounded.Sync, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_my_channels), color = Color.White) }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CreateNewFolder, null, tint = MaterialTheme.colorScheme.onBackground); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.create_new_folder), color = MaterialTheme.colorScheme.onBackground) }
    }
}