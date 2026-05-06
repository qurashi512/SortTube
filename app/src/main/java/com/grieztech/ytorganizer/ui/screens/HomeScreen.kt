package com.grieztech.ytorganizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.ui.components.*
import com.grieztech.ytorganizer.ui.theme.*
import com.grieztech.ytorganizer.ui.viewmodel.HomeViewModel
import org.burnoutcrew.reorderable.*

@Composable
fun HomeScreen(
    onFolderClick  : (Folder) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel      : HomeViewModel = hiltViewModel(),
) {
    val context       = LocalContext.current
    val folders       by viewModel.folders.collectAsState()
    val channelCounts by viewModel.channelCounts.collectAsState()
    val isSyncing     by viewModel.isSyncing.collectAsState()
    val syncError     by viewModel.syncError.collectAsState()
    val syncSuccess   by viewModel.syncSuccess.collectAsState()
    val allChannels   by viewModel.allChannels.collectAsState()
    val allPlaylists  by viewModel.allPlaylists.collectAsState()
    val folderError   by viewModel.folderCreateError.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val account     = remember { GoogleSignIn.getLastSignedInAccount(context) }
    val displayName = account?.displayName ?: account?.email ?: "GriezTech"
    val userEmail   = account?.email ?: ""

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

    Scaffold(
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        floatingActionButton = {
            GlassFAB(onClick = { showAddDialog = true }, icon = Icons.Rounded.CreateNewFolder,
                label = stringResource(R.string.new_folder), modifier = Modifier.navigationBarsPadding())
        }
    ) { _ ->
        Box(modifier = Modifier.fillMaxSize()) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(brush = Brush.radialGradient(
                    colors = listOf(GradientEnd.copy(alpha = 0.95f), GradientStart, GradientMid),
                    center = Offset(size.width * 0.3f, size.height * 0.2f), radius = size.width * 1.2f))
                drawCircle(color = AccentPurple.copy(alpha = 0.20f), radius = size.width * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f))
                drawCircle(color = YouTubeRed.copy(alpha = 0.12f), radius = size.width * 0.45f,
                    center = Offset(size.width * 0.1f, size.height * 0.72f))
            }
            Column(modifier = Modifier.fillMaxSize()) {
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 0.dp, glassAlpha = 0.10f) {
                    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(displayName, style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                                if (userEmail.isNotEmpty() && userEmail != displayName)
                                    Text(userEmail, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                }
                                IconButton(onClick = { showSyncOptions = true }) {
                                    Icon(Icons.Rounded.Sync, stringResource(R.string.import_text), tint = MaterialTheme.colorScheme.onBackground)
                                }
                                IconButton(onClick = onSettingsClick) {
                                    Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = MaterialTheme.colorScheme.onBackground)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.PlayCircle, null, tint = YouTubeRed, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("GriezTech", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                            Text("  ·  ${folders.size} ${stringResource(R.string.folder_word)}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.40f))
                        }
                    }
                }
                if (folders.isEmpty() && !isSyncing) {
                    EmptyState(onAddFolder = { showAddDialog = true }, onSyncChannels = { showSyncOptions = true })
                } else {
                    LazyColumn(state = reorderState.listState,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().reorderable(reorderState)) {
                        items(folders, key = { it.id }) { folder ->
                            ReorderableItem(reorderState, key = folder.id) { isDragging ->
                                val elev by animateFloatAsState(if (isDragging) 8f else 0f, label = "elev")
                                FolderCard(folder = folder, channelCount = channelCounts[folder.id] ?: 0,
                                    isDragging = isDragging, onClick = { onFolderClick(folder) },
                                    onLongPress = { folderToDelete = folder },
                                    modifier = Modifier.detectReorderAfterLongPress(reorderState)
                                        .graphicsLayer { shadowElevation = elev })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSyncOptions) {
        SyncOptionsDialog(onDismiss = { showSyncOptions = false },
            onSyncSubscriptions = { showSyncOptions = false; viewModel.syncSubscriptions() },
            onSyncPlaylists     = { showSyncOptions = false; viewModel.syncPlaylists() },
            onSyncAll           = { showSyncOptions = false; viewModel.syncAll() })
    }

    folderToDelete?.let { folder ->
        val willBeDeleted = stringResource(R.string.will_be_deleted)
        val cannotBeUndone = stringResource(R.string.cannot_be_undone)
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            containerColor   = MaterialTheme.colorScheme.surface,
            title  = { Text(stringResource(R.string.delete_folder_title)) },
            text   = { Text("$willBeDeleted \"${folder.emoji} ${folder.name}\" $cannotBeUndone") },
            confirmButton  = {
                Button(
                    onClick = { viewModel.deleteFolder(folder); folderToDelete = null },
                    colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showAddDialog) {
        AddFolderDialog(
            allChannels   = allChannels,
            allPlaylists  = allPlaylists,
            externalError = folderError,
            onClearError  = { viewModel.clearFolderCreateError() },
            onConfirm = { name, emoji, color, chIds, plIds ->
                viewModel.createFolderWithItems(
                    name        = name,
                    emoji       = emoji,
                    color       = color,
                    channelIds  = chIds,
                    playlistIds = plIds,
                    onSuccess   = { showAddDialog = false },
                    onError     = { /* الخطأ يظهر داخل الحوار */ },
                )
            },
            onDismiss = { showAddDialog = false; viewModel.clearFolderCreateError() },
        )
    }
}

@Composable
private fun SyncOptionsDialog(onDismiss: () -> Unit, onSyncSubscriptions: () -> Unit,
                              onSyncPlaylists: () -> Unit, onSyncAll: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        title = { Text(stringResource(R.string.import_from_youtube), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.choose_what_to_import), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                Spacer(Modifier.height(4.dp))
                OutlinedButton(onClick = onSyncSubscriptions, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.subscribed_channels))
                }
                OutlinedButton(onClick = onSyncPlaylists, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.QueueMusic, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.playlists))
                }
                Button(onClick = onSyncAll, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Sync, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_all))
                }
            }
        }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

@Composable
private fun EmptyState(onAddFolder: () -> Unit, onSyncChannels: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📺", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.no_folders_yet), style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.create_folder_or_import), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), textAlign = TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onSyncChannels, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Sync, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.import_my_channels))
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onAddFolder, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CreateNewFolder, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.create_new_folder))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFolderDialog(
    allChannels   : List<Channel>,
    allPlaylists  : List<Playlist>,
    externalError : String?,
    onClearError  : () -> Unit,
    onConfirm     : (String, String, String, List<String>, List<String>) -> Unit,
    onDismiss     : () -> Unit,
) {
    var folderName        by remember { mutableStateOf("") }
    var selectedEmoji     by remember { mutableStateOf("📁") }
    var selectedColor     by remember { mutableStateOf("#FF4444") }
    var selectedChannels  by remember { mutableStateOf(setOf<String>()) }
    var selectedPlaylists by remember { mutableStateOf(setOf<String>()) }
    var currentTab        by remember { mutableStateOf(0) }
    val errorMsg = externalError

    val emojis = listOf("📁","🎵","🎮","📚","🏋️","🍳","✈️","💻","🎬","🔬","📰","🎨")
    val colors = listOf("#FF4444","#FF8800","#FFCC00","#44CC88","#4488FF","#8855FF","#FF44AA","#00CCBB")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = Color(0xFF1A1A2E),
        shape            = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(stringResource(R.string.new_folder), style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(Triple(stringResource(R.string.basic_tab), Icons.Rounded.Folder, 0),
                        Triple(stringResource(R.string.channels_tab), Icons.Rounded.Person, 1),
                        Triple(stringResource(R.string.playlists_tab), Icons.Rounded.QueueMusic, 2))
                        .forEach { (label, icon, idx) ->
                            FilterChip(selected = currentTab == idx, onClick = { currentTab = idx },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(icon, null, modifier = Modifier.size(13.dp))
                                        Text(label, fontSize = 12.sp)
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPurple.copy(0.35f),
                                    selectedLabelColor     = Color.White,
                                    containerColor         = Color.White.copy(0.06f),
                                    labelColor             = Color.White.copy(0.5f)),
                                modifier = Modifier.weight(1f))
                        }
                }
            }
        },
        text = {
            Box(modifier = Modifier.heightIn(max = 420.dp)) {
                when (currentTab) {
                    0 -> Column(modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.choose_emoji), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            emojis.forEach { emoji ->
                                FilterChip(selected = emoji == selectedEmoji, onClick = { selectedEmoji = emoji },
                                    label = { Text(emoji) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentPurple.copy(0.35f), containerColor = Color.White.copy(0.06f)))
                            }
                        }
                        OutlinedTextField(value = folderName, onValueChange = { folderName = it },
                            label = { Text(stringResource(R.string.folder_name), color = Color.White.copy(0.6f)) },
                            singleLine = true, modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedBorderColor = AccentPurple, unfocusedBorderColor = Color.White.copy(0.2f)))
                        Text(stringResource(R.string.choose_color), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.7f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            colors.forEach { hex ->
                                val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Red }
                                Box(modifier = Modifier.size(32.dp).background(c, CircleShape)
                                    .border(if (hex == selectedColor) 3.dp else 0.dp, Color.White, CircleShape)
                                    .clickable { selectedColor = hex })
                            }
                        }
                        if (selectedChannels.isNotEmpty() || selectedPlaylists.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(0.1f))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (selectedChannels.isNotEmpty())
                                    AssistChip(onClick = { currentTab = 1 },
                                        label = { Text("${selectedChannels.size} ${stringResource(R.string.channel_word)}", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.Person, null, Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = AccentBlue.copy(0.20f), labelColor = Color.White))
                                if (selectedPlaylists.isNotEmpty())
                                    AssistChip(onClick = { currentTab = 2 },
                                        label = { Text("${selectedPlaylists.size} ${stringResource(R.string.playlist_word)}", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Rounded.QueueMusic, null, Modifier.size(14.dp)) },
                                        colors = AssistChipDefaults.assistChipColors(containerColor = AccentTeal.copy(0.20f), labelColor = Color.White))
                            }
                        }
                    }
                    1 -> if (allChannels.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("📡", fontSize = 36.sp)
                                Text(stringResource(R.string.no_imported_channels), style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedChannels.size}/${allChannels.size} ${stringResource(R.string.selected_label)}",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
                                TextButton(onClick = {
                                    selectedChannels = if (selectedChannels.size == allChannels.size) emptySet()
                                    else allChannels.map { it.id }.toSet()
                                }) { Text(if (selectedChannels.size == allChannels.size) stringResource(R.string.deselect_all) else stringResource(R.string.select_all), color = AccentPurple, fontSize = 12.sp) }
                            }
                            LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(allChannels, key = { it.id }) { ch ->
                                    val sel = ch.id in selectedChannels
                                    SelectableItem(ch.title, "${ch.subscriberCount} ${stringResource(R.string.subscriber_word)}", sel, AccentBlue) {
                                        selectedChannels = if (sel) selectedChannels - ch.id else selectedChannels + ch.id
                                    }
                                }
                            }
                        }
                    }
                    2 -> if (allPlaylists.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("🎵", fontSize = 36.sp)
                                Text(stringResource(R.string.no_imported_playlists), style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedPlaylists.size}/${allPlaylists.size} ${stringResource(R.string.selected_label)}",
                                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f))
                                TextButton(onClick = {
                                    selectedPlaylists = if (selectedPlaylists.size == allPlaylists.size) emptySet()
                                    else allPlaylists.map { it.id }.toSet()
                                }) { Text(if (selectedPlaylists.size == allPlaylists.size) stringResource(R.string.deselect_all) else stringResource(R.string.select_all), color = AccentTeal, fontSize = 12.sp) }
                            }
                            LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(allPlaylists, key = { it.id }) { pl ->
                                    val sel = pl.id in selectedPlaylists
                                    SelectableItem(pl.title, "${pl.itemCount} ${stringResource(R.string.video_word)} • ${pl.channelTitle}", sel, AccentTeal) {
                                        selectedPlaylists = if (sel) selectedPlaylists - pl.id else selectedPlaylists + pl.id
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!errorMsg.isNullOrBlank()) {
                    Text(
                        text  = errorMsg,
                        color = Color(0xFFFF6060),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp, end = 4.dp),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = Color.White.copy(0.6f))
                    }
                    Button(
                        onClick = {
                            onClearError()
                            if (folderName.isNotBlank())
                                onConfirm(folderName.trim(), selectedEmoji, selectedColor,
                                    selectedChannels.toList(), selectedPlaylists.toList())
                        },
                        colors  = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        enabled = folderName.isNotBlank(),
                    ) { Text(stringResource(R.string.create)) }
                }
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun SelectableItem(title: String, subtitle: String, isSelected: Boolean, accentColor: Color, onToggle: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(accentColor.copy(if (isSelected) 0.20f else 0.06f), androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        .clickable(onClick = onToggle).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Checkbox(checked = isSelected, onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = accentColor, uncheckedColor = Color.White.copy(0.3f)))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1)
            if (subtitle.isNotEmpty())
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f), maxLines = 1)
        }
    }
}