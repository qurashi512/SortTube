package com.grieztech.ytorganizer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(folder.id) {
        viewModel.loadFolder(folder.id)
    }

    val headerCount by remember(videos) {
        derivedStateOf { if (videos.isNotEmpty()) 3 else 2 }
    }

    val reorderState = rememberReorderableLazyListState(
        onMove   = { from, to ->
            val fromChannel = from.index - headerCount
            val toChannel   = to.index   - headerCount
            viewModel.moveChannel(fromChannel, toChannel)
        },
        onDragEnd = { _, _ -> viewModel.saveChannelOrder() },
    )

    var showVideoPlayer by remember { mutableStateOf<Video?>(null) }

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

            // ✅ إضافة الخلفية المتدرجة لضمان وضوح العناصر في الوضع الداكن[cite: 7, 9]
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0D0D1A), Color(0xFF12122A), Color(0xFF0A0A14))
                    )
                )
                drawCircle(
                    color = AccentPurple.copy(0.15f),
                    radius = size.width * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.1f)
                )
                drawCircle(
                    color = YouTubeRed.copy(0.08f),
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.1f, size.height * 0.8f)
                )
            }

            LazyColumn(
                state          = reorderState.listState,
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier       = Modifier
                    .fillMaxSize()
                    .reorderable(reorderState),
            ) {
                // ── Header ──
                item {
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
                                    tint = Color.White,
                                )
                            }
                        }
                    )
                }

                // ── أحدث الفيديوهات ──
                if (videos.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Text(
                                text  = stringResource(R.string.latest_videos),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                            )
                            Spacer(Modifier.height(8.dp))
                            videos.take(5).forEach { video ->
                                VideoCard(
                                    video   = video,
                                    onClick = { showVideoPlayer = video },
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                        }
                    }
                }

                // ── قسم القنوات ──
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(
                            text  = "${stringResource(R.string.channels)} (${channels.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text  = stringResource(R.string.long_press_to_reorder),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.5f),
                        )
                    }
                }

                items(channels, key = { it.id }) { channel ->
                    ReorderableItem(reorderState, key = channel.id) { isDragging ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            ChannelCard(
                                channel    = channel,
                                isDragging = isDragging,
                                onClick    = { viewModel.openChannel(channel) },
                                modifier   = Modifier
                                    .weight(1f)
                                    .detectReorderAfterLongPress(reorderState)
                                    .graphicsLayer {
                                        scaleX = if (isDragging) 1.03f else 1f
                                        scaleY = if (isDragging) 1.03f else 1f
                                    },
                            )

                            IconButton(onClick = { viewModel.removeChannelFromFolder(channel) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = YouTubeRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // ── قسم قوائم التشغيل ──
                if (playlists.isNotEmpty()) {
                    item {
                        Text(
                            text     = "${stringResource(R.string.playlists)} (${playlists.size})",
                            style    = MaterialTheme.typography.titleMedium,
                            color    = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(playlists, key = { "pl_${it.id}" }) { playlist ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            PlaylistCard(
                                playlist = playlist,
                                onClick  = { viewModel.openPlaylist(playlist) }, // ✅ تشغيل يوتيوب[cite: 7]
                                modifier = Modifier.weight(1f),
                            )

                            IconButton(onClick = { viewModel.removePlaylistFromFolder(playlist) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = YouTubeRed.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                if (isLoading) {
                    item {
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

        showVideoPlayer?.let { video ->
            VideoOptionsDialog(
                video     = video,
                onDismiss = { showVideoPlayer = null },
                onOpenInApp = { showVideoPlayer = null },
                onOpenInYouTube = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                    context.startActivity(intent)
                    showVideoPlayer = null
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

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                text = stringResource(R.string.add_to_folder),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                TabRow(
                    selectedTabIndex = tabIndex,
                    containerColor = Color.Transparent,
                    contentColor = AccentPurple,
                    divider = {}
                ) {
                    Tab(
                        selected = tabIndex == 0,
                        onClick = { tabIndex = 0 },
                        text = { Text(stringResource(R.string.channels)) }
                    )
                    Tab(
                        selected = tabIndex == 1,
                        onClick = { tabIndex = 1 },
                        text = { Text(stringResource(R.string.playlists)) }
                    )
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    if (tabIndex == 0) {
                        items(availableChannels) { channel ->
                            val isSelected = selectedChannelIds.contains(channel.id)
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(channel.title, color = Color.White) },
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
                                        colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val checked = !isSelected
                                    selectedChannelIds = if (checked) {
                                        selectedChannelIds + channel.id
                                    } else {
                                        selectedChannelIds - channel.id
                                    }
                                }
                            )
                        }
                    } else {
                        items(availablePlaylists) { playlist ->
                            val isSelected = selectedPlaylistIds.contains(playlist.id)
                            ListItem(
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                headlineContent = { Text(playlist.title, color = Color.White) },
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
                                        colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                                    )
                                },
                                modifier = Modifier.clickable {
                                    val checked = !isSelected
                                    selectedPlaylistIds = if (checked) {
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
                Text(stringResource(R.string.add_to_folder), color = AccentPurple)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = Color.White.copy(alpha = 0.6f))
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
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A2E),
        title = {
            Text(
                stringResource(R.string.how_to_watch_video),
                color = Color.White,
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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.watch_in_app))
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onOpenInYouTube,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.open_in_youtube), color = Color.White)
                }
            }
        },
        confirmButton = {}
    )
}