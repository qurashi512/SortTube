package com.grieztech.ytorganizer.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.models.Channel
import com.grieztech.ytorganizer.models.Folder
import com.grieztech.ytorganizer.models.Playlist
import com.grieztech.ytorganizer.models.Video
import androidx.core.graphics.toColorInt

// ═══════════════════════════════════════
//  GriezTech - Channel & Folder Cards
// ═══════════════════════════════════════

/**
 * بطاقة المجلد الزجاجية
 */
@Composable
fun FolderCard(
    folder      : Folder,
    channelCount: Int,
    onClick     : () -> Unit,
    onLongPress : () -> Unit = {},
    isDragging  : Boolean   = false,
    modifier    : Modifier  = Modifier,
) {
    val folderColor = remember(folder.color) {
        try { Color(folder.color.toColorInt()) } catch (e: Exception) { Color(0xFFFF4444) }
    }

    val elevation by animateColorAsState(
        targetValue  = if (isDragging) folderColor.copy(0.5f) else Color.Transparent,
        animationSpec = spring(),
        label        = "drag_elevation",
    )

    GlassCard(
        modifier     = modifier
            .fillMaxWidth()
            .height(100.dp),
        cornerRadius = 24.dp,
        onClick      = onClick,
        onLongClick  = onLongPress.takeIf { true },  // ✅ إصلاح 2b: تمرير long press
    ) {
        // خلفية ملونة خفيفة
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            folderColor.copy(alpha = 0.25f),
                            folderColor.copy(alpha = 0.05f),
                        )
                    )
                )
        )

        Row(
            modifier  = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // أيقونة المجلد
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(folderColor.copy(alpha = 0.25f)),
                ) {
                    Text(text = folder.emoji, style = MaterialTheme.typography.headlineSmall)
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text     = folder.name,
                        style    = MaterialTheme.typography.titleMedium,
                        color    = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = "$channelCount ${stringResource(R.string.channel_word)}", // ✅ الترجمة هنا
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            Icon(
                imageVector        = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
        }
    }
}

/**
 * بطاقة القناة الزجاجية
 */
@Composable
fun ChannelCard(
    channel    : Channel,
    onClick    : () -> Unit,
    onLongPress: () -> Unit = {},
    isDragging : Boolean    = false,
    modifier   : Modifier   = Modifier,
) {
    GlassCard(
        modifier     = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick      = onClick,
    ) {
        Row(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // صورة القناة
            AsyncImage(
                model       = channel.thumbnailUrl,
                contentDescription = channel.title,
                contentScale = ContentScale.Crop,
                modifier    = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text     = channel.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "${formatCount(channel.subscriberCount)} ${stringResource(R.string.subscriber_word)}", // ✅ الترجمة هنا
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }

            // أيقونة السحب
            Icon(
                imageVector        = Icons.Rounded.DragHandle,
                contentDescription = "سحب",
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * بطاقة الفيديو
 */
@Composable
fun VideoCard(
    video   : Video,
    onClick : () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        modifier     = modifier.fillMaxWidth(),
        cornerRadius = 16.dp,
        onClick      = onClick,
    ) {
        Column {
            // صورة مصغرة للفيديو
            AsyncImage(
                model        = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier     = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text     = video.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = video.channelTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}

// ── دالة مساعدة لتنسيق الأرقام الكبيرة ──
private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}

/**
 * ✅ إصلاح 1: بطاقة قائمة التشغيل
 */
@Composable
fun PlaylistCard(
    playlist : Playlist,
    onClick  : (() -> Unit)? = null,
    modifier : Modifier = Modifier,
) {
    GlassCard(
        modifier     = modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
        onClick      = onClick,
    ) {
        Row(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // صورة مصغرة
            AsyncImage(
                model        = playlist.thumbnailUrl,
                contentDescription = playlist.title,
                contentScale = ContentScale.Crop,
                modifier     = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text     = playlist.title,
                    style    = MaterialTheme.typography.titleSmall,
                    color    = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "${playlist.itemCount} ${stringResource(R.string.video_word)} · ${playlist.channelTitle}", // ✅ الترجمة هنا
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Icon(
                imageVector        = Icons.Rounded.PlaylistPlay,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier           = Modifier.size(20.dp),
            )
        }
    }
}