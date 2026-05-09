package com.grieztech.ytorganizer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.models.Video
import com.grieztech.ytorganizer.ui.components.*
import com.grieztech.ytorganizer.ui.theme.*
import com.grieztech.ytorganizer.ui.viewmodel.FolderDetailViewModel
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    folder   : Folder,
    onBack   : () -> Unit,
    viewModel: FolderDetailViewModel = hiltViewModel(),
) {
    val context   = LocalContext.current
    val channels  by viewModel.channels.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val videos    by viewModel.latestVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    val availableChannels by viewModel.availableChannels.collectAsState()
    val availablePlaylists by viewModel.availablePlaylists.collectAsState()

    // ── حالة التبويبات (0 للقنوات، 1 للقوائم) ──
    var currentTab by remember { mutableIntStateOf(0) }

    // ── حالات الحذف (لإظهار نافذة التأكيد) ──
    var channelToRemove by remember { mutableStateOf<Channel?>(null) }
    var playlistToRemove by remember { mutableStateOf<Playlist?>(null) }

    LaunchedEffect(folder.id) {
        viewModel.loadFolder(folder.id)
    }

    val reorderState = rememberReorderableLazyListState(
        onMove = { from, to ->
            val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
            val toKey = to.key as? String ?: return@rememberReorderableLazyListState

            val fromIndex = channels.indexOfFirst { it.id == fromKey }
            val toIndex = channels.indexOfFirst { it.id == toKey }

            if (fromIndex != -1 && toIndex != -1) {
                viewModel.moveChannel(fromIndex, toIndex)
            }
        },
        canDragOver = { draggedOver, _ ->
            val overKey = draggedOver.key as? String
            overKey != null && channels.any { it.id == overKey }
        },
        onDragEnd = { _, _ -> viewModel.saveChannelOrder() },
    )

    var showVideoPlayer by remember { mutableStateOf<Video?>(null) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            GlassFAB(
                onClick = {
                    viewModel.loadAvailableItems()
                    showAddDialog = true
                },
                icon = Icons.Rounded.Add,
                label = stringResource(R.string.add_to_folder),
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Canvas(modifier = Modifier.fillMaxSize()) {
                if (isDark) {
                    drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF0D0D1A), Color(0xFF12122A), Color(0xFF0A0A14))))
                    drawCircle(color = AccentPurple.copy(0.15f), radius = size.width * 0.55f, center = Offset(size.width * 0.85f, size.height * 0.1f))
                    drawCircle(color = YouTubeRed.copy(0.08f), radius = size.width * 0.45f, center = Offset(size.width * 0.1f, size.height * 0.8f))
                } else {
                    drawRect(brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFF5F0E8), Color(0xFFF2EDE5), Color(0xFFEDE8F5)),
                        start  = Offset(0f, 0f),
                        end    = Offset(size.width, size.height),
                    ))
                }
            }

            LazyColumn(
                state          = reorderState.listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier       = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState),
            ) {
                item(key = "header_top") {
                    val channelStr = stringResource(R.string.channel_word)
                    val playlistStr = stringResource(R.string.playlist_word)

                    GlassTopBar(
                        title    = "${folder.emoji} ${folder.name}",
                        subtitle = buildString {
                            append("${channels.size} $channelStr")
                            if (playlists.isNotEmpty()) append(" · ${playlists.size} $playlistStr")
                        },
                        onBack   = onBack,
                        actions  = {
                            IconButton(onClick = { viewModel.refreshVideos() }) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    tint = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                                )
                            }
                        }
                    )
                }

                // ── شريط التبويبات ──
                item(key = "tabs") {
                    TabRow(
                        selectedTabIndex = currentTab,
                        containerColor = Color.Transparent,
                        contentColor = if (isDark) AccentPurple else MaterialTheme.colorScheme.secondary,
                        divider = { HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.onBackground.copy(0.1f)) }
                    ) {
                        Tab(selected = currentTab == 0, onClick = { currentTab = 0 },
                            text = { Text("${stringResource(R.string.channels)} (${channels.size})",
                                color = if (currentTab == 0) (if (isDark) AccentPurple else MaterialTheme.colorScheme.secondary)
                                        else (if (isDark) Color.White.copy(0.6f) else MaterialTheme.colorScheme.onBackground.copy(0.5f))) })
                        Tab(selected = currentTab == 1, onClick = { currentTab = 1 },
                            text = { Text("${stringResource(R.string.playlists)} (${playlists.size})",
                                color = if (currentTab == 1) (if (isDark) AccentPurple else MaterialTheme.colorScheme.secondary)
                                        else (if (isDark) Color.White.copy(0.6f) else MaterialTheme.colorScheme.onBackground.copy(0.5f))) })
                        Tab(selected = currentTab == 2, onClick = { currentTab = 2 },
                            text = { Text(stringResource(R.string.latest_videos), maxLines = 1,
                                color = if (currentTab == 2) (if (isDark) AccentPurple else MaterialTheme.colorScheme.secondary)
                                        else (if (isDark) Color.White.copy(0.6f) else MaterialTheme.colorScheme.onBackground.copy(0.5f))) })
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (currentTab == 0) {
                    // ── محتوى تبويب القنوات (بدون فيديوهات) ──
                    item(key = "header_channels") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Text(
                                text  = stringResource(R.string.channels),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text  = stringResource(R.string.long_press_to_reorder),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onBackground.copy(0.5f),
                            )
                        }
                    }

                    items(channels, key = { it.id }) { channel ->
                        ReorderableItem(reorderState, key = channel.id) { isDragging ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        channelToRemove = channel // إظهار نافذة التأكيد
                                        false // يمنع الحذف المباشر لكي يظهر السؤال أولاً
                                    } else false
                                }
                            )

                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                                        if (isSwiping) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFFAA0000), Color(0xFFEE3333)))).padding(end = 24.dp),
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
                                        ChannelCard(
                                            channel    = channel,
                                            isDragging = isDragging,
                                            onClick    = { viewModel.openChannel(channel) },
                                            modifier   = Modifier
                                                .fillMaxWidth()
                                                .detectReorderAfterLongPress(reorderState)
                                                .graphicsLayer {
                                                    scaleX = if (isDragging) 1.03f else 1f
                                                    scaleY = if (isDragging) 1.03f else 1f
                                                },
                                        )
                                    }
                                )
                            }
                        }
                    }
                } else if (currentTab == 1) {
                    // ── محتوى تبويب القوائم ──
                    if (playlists.isNotEmpty()) {
                        item(key = "header_playlists") {
                            Text(
                                text     = stringResource(R.string.playlists),
                                style    = MaterialTheme.typography.titleMedium,
                                color    = if (isDark) Color.White else MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        items(playlists, key = { "pl_${it.id}" }) { playlist ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        playlistToRemove = playlist // إظهار نافذة التأكيد
                                        false
                                    } else false
                                }
                            )

                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                SwipeToDismissBox(
                                    state = dismissState,
                                    enableDismissFromStartToEnd = false,
                                    backgroundContent = {
                                        val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                                        if (isSwiping) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFFAA0000), Color(0xFFEE3333)))).padding(end = 24.dp),
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
                                        PlaylistCard(
                                            playlist = playlist,
                                            onClick  = { viewModel.openPlaylist(playlist) },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // ── محتوى تبويب آخر الفيديوهات ──
                    if (isLoading) {
                        item(key = "videos_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(color = AccentPurple)
                            }
                        }
                    } else if (videos.isEmpty()) {
                        item(key = "videos_empty") {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("🎬", style = MaterialTheme.typography.displayMedium)
                                Text(
                                    text  = stringResource(R.string.no_videos_yet),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isDark) Color.White.copy(0.5f) else MaterialTheme.colorScheme.onBackground.copy(0.5f),
                                )
                                OutlinedButton(
                                    onClick = { viewModel.refreshVideos() },
                                    border  = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(0.5f))
                                ) {
                                    Icon(Icons.Rounded.Refresh, null, tint = AccentPurple)
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.refresh), color = AccentPurple)
                                }
                            }
                        }
                    } else {
                        items(videos, key = { "vid_${it.id}" }) { video ->
                            VideoCard(
                                video    = video,
                                onClick  = { showVideoPlayer = video },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                if (isLoading && currentTab != 2) {
                    item(key = "loading_indicator") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = AccentPurple)
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AddItemsDialog(
                availableChannels = availableChannels,
                availablePlaylists = availablePlaylists,
                onDismiss = { showAddDialog = false },
                onConfirm = { chIds, plIds ->
                    viewModel.addItemsToFolder(chIds, plIds)
                    showAddDialog = false
                }
            )
        }

        // فتح يوتيوب مباشرة عند الضغط على الفيديو
        showVideoPlayer?.let { video ->
            LaunchedEffect(video.id) {
                try {
                    // حاول فتح تطبيق يوتيوب أولاً
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:${video.id}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // لو مش مثبّت، افتح في المتصفح
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                    context.startActivity(intent)
                }
                showVideoPlayer = null
            }
        }

        // ── نوافذ التأكيد على الحذف ──
        channelToRemove?.let { channel ->
            AlertDialog(
                onDismissRequest = { channelToRemove = null },
                containerColor   = MaterialTheme.colorScheme.surface,
                shape            = RoundedCornerShape(24.dp),
                title  = { Text(stringResource(R.string.delete_channel_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text   = { Text(stringResource(R.string.remove_from_folder_desc), color = MaterialTheme.colorScheme.onSurface.copy(0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeChannelFromFolder(channel)
                            channelToRemove = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4455)),
                    ) { Text(stringResource(R.string.delete), color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { channelToRemove = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }

        playlistToRemove?.let { playlist ->
            AlertDialog(
                onDismissRequest = { playlistToRemove = null },
                containerColor   = MaterialTheme.colorScheme.surface,
                shape            = RoundedCornerShape(24.dp),
                title  = { Text(stringResource(R.string.delete_playlist_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
                text   = { Text(stringResource(R.string.remove_from_folder_desc), color = MaterialTheme.colorScheme.onSurface.copy(0.7f)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removePlaylistFromFolder(playlist)
                            playlistToRemove = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4455)),
                    ) { Text(stringResource(R.string.delete), color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { playlistToRemove = null }) { Text(stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun AddItemsDialog(
    availableChannels: List<Channel>,
    availablePlaylists: List<Playlist>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>, List<String>) -> Unit,
) {
    var selectedChannelIds by remember { mutableStateOf(setOf<String>()) }
    var selectedPlaylistIds by remember { mutableStateOf(setOf<String>()) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    LaunchedEffect(tabIndex) {
        searchQuery = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFFAF6EF),
        title = {
            Text(
                text = stringResource(R.string.add_to_folder),
                color = if (isDark) Color.White else Color(0xFF2E2820),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp)) {
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = if (isDark) AccentPurple else Color(0xFF7B5EA7),
                    divider = {}
                ) {
                    Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 },
                        text = { Text(stringResource(R.string.channels),
                            color = if (tabIndex == 0) (if (isDark) AccentPurple else Color(0xFF7B5EA7))
                                    else (if (isDark) Color.White.copy(0.6f) else Color(0xFF2E2820).copy(0.5f))) })
                    Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 },
                        text = { Text(stringResource(R.string.playlists),
                            color = if (tabIndex == 1) (if (isDark) AccentPurple else Color(0xFF7B5EA7))
                                    else (if (isDark) Color.White.copy(0.6f) else Color(0xFF2E2820).copy(0.5f))) })
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_hint), color = if (isDark) Color.White.copy(0.4f) else Color(0xFF2E2820).copy(0.4f)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = if (isDark) Color.White.copy(0.5f) else Color(0xFF2E2820).copy(0.5f)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, null, tint = if (isDark) Color.White.copy(0.5f) else Color(0xFF2E2820).copy(0.5f))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = if (isDark) AccentPurple else Color(0xFF7B5EA7),
                        unfocusedBorderColor = if (isDark) Color.White.copy(0.1f) else Color(0xFF2E2820).copy(0.15f),
                        focusedTextColor     = if (isDark) Color.White else Color(0xFF2E2820),
                        unfocusedTextColor   = if (isDark) Color.White else Color(0xFF2E2820),
                    )
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (tabIndex == 0) {
                        item {
                            val filteredChannels = if (searchQuery.isBlank()) availableChannels else availableChannels.filter { it.title.contains(searchQuery, ignoreCase = true) }

                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedChannelIds.size}/${availableChannels.size} ${stringResource(R.string.selected_label)}", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.White.copy(0.5f) else Color(0xFF2E2820).copy(0.5f))
                                val filteredIds = filteredChannels.map { it.id }.toSet()
                                val allFilteredSelected = filteredIds.isNotEmpty() && selectedChannelIds.containsAll(filteredIds)

                                TextButton(onClick = {
                                    selectedChannelIds = if (allFilteredSelected) selectedChannelIds - filteredIds else selectedChannelIds + filteredIds
                                }) { Text(if (allFilteredSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all), color = if (isDark) AccentPurple else Color(0xFF7B5EA7), fontSize = 12.sp) }
                            }
                        }

                        val filteredList = if (searchQuery.isBlank()) availableChannels else availableChannels.filter { it.title.contains(searchQuery, ignoreCase = true) }

                        items(filteredList) { channel ->
                            val isSelected = selectedChannelIds.contains(channel.id)
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(channel.title, color = if (isDark) Color.White else Color(0xFF2E2820)) },
                                leadingContent = {
                                    AsyncImage(
                                        model = channel.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(CircleShape)
                                    )
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedChannelIds = if (it) {
                                                selectedChannelIds + channel.id
                                            } else {
                                                selectedChannelIds - channel.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = if (isDark) AccentPurple else Color(0xFF7B5EA7))
                                    )
                                },
                                modifier = Modifier.clickable {
                                    selectedChannelIds = if (!isSelected) {
                                        selectedChannelIds + channel.id
                                    } else {
                                        selectedChannelIds - channel.id
                                    }
                                }
                            )
                        }
                    } else {
                        item {
                            val filteredPlaylists = if (searchQuery.isBlank()) availablePlaylists else availablePlaylists.filter { it.title.contains(searchQuery, ignoreCase = true) }

                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${selectedPlaylistIds.size}/${availablePlaylists.size} ${stringResource(R.string.selected_label)}", style = MaterialTheme.typography.bodySmall, color = if (isDark) Color.White.copy(0.5f) else Color(0xFF2E2820).copy(0.5f))
                                val filteredIds = filteredPlaylists.map { it.id }.toSet()
                                val allFilteredSelected = filteredIds.isNotEmpty() && selectedPlaylistIds.containsAll(filteredIds)

                                TextButton(onClick = {
                                    selectedPlaylistIds = if (allFilteredSelected) selectedPlaylistIds - filteredIds else selectedPlaylistIds + filteredIds
                                }) { Text(if (allFilteredSelected) stringResource(R.string.deselect_all) else stringResource(R.string.select_all), color = if (isDark) AccentPurple else Color(0xFF7B5EA7), fontSize = 12.sp) }
                            }
                        }

                        val filteredList = if (searchQuery.isBlank()) availablePlaylists else availablePlaylists.filter { it.title.contains(searchQuery, ignoreCase = true) }

                        items(filteredList) { playlist ->
                            val isSelected = selectedPlaylistIds.contains(playlist.id)
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(playlist.title, color = if (isDark) Color.White else Color(0xFF2E2820)) },
                                leadingContent = {
                                    AsyncImage(
                                        model = playlist.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedPlaylistIds = if (it) {
                                                selectedPlaylistIds + playlist.id
                                            } else {
                                                selectedPlaylistIds - playlist.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = if (isDark) AccentPurple else Color(0xFF7B5EA7))
                                    )
                                },
                                modifier = Modifier.clickable {
                                    selectedPlaylistIds = if (!isSelected) {
                                        selectedPlaylistIds + playlist.id
                                    } else {
                                        selectedPlaylistIds - playlist.id
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedChannelIds.toList(), selectedPlaylistIds.toList()) },
                enabled = selectedChannelIds.isNotEmpty() || selectedPlaylistIds.isNotEmpty()
            ) {
                Text(stringResource(R.string.add_to_folder), color = if (isDark) AccentPurple else Color(0xFF7B5EA7))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF2E2820).copy(0.5f))
            }
        }
    )
}

@Composable
fun VideoOptionsDialog(
    video: Video,
    onDismiss: () -> Unit,
    onOpenInApp: () -> Unit,
    onOpenInYouTube: () -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = if (isDark) Color(0xFF1A1A2E) else Color(0xFFFAF6EF),
        title = {
            Text(
                stringResource(R.string.how_to_watch_video),
                color = if (isDark) Color.White else Color(0xFF2E2820),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                VideoCard(video = video, onClick = {})
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onOpenInApp,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isDark) AccentPurple else Color(0xFF7B5EA7))
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.watch_in_app))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenInYouTube,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFF2E2820).copy(0.2f))
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = if (isDark) Color.White else Color(0xFF2E2820))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_in_youtube), color = if (isDark) Color.White else Color(0xFF2E2820))
                }
            }
        },
        confirmButton = {}
    )
}