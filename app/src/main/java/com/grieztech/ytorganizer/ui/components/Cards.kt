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
import androidx.compose.ui.text.font.FontWeight
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
    onEdit      : () -> Unit = {},
    onLongPress : () -> Unit = {},
    isDragging  : Boolean   = false,
    isProtected : Boolean   = false,
    modifier    : Modifier  = Modifier,
    dragHandle  : Modifier  = Modifier,
) {
    val folderColor = remember(folder.color) {
        try { Color(folder.color.toColorInt()) } catch (e: Exception) { Color(0xFF9B6FFF) }
    }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val cardGradient = if (isDark) {
        // ✅ الوضع الداكن: تدرج أغمق مع لون المجلد
        Brush.linearGradient(
            colors = listOf(
                folderColor.copy(alpha = if (isDragging) 0.45f else 0.30f),
                folderColor.copy(alpha = 0.10f),
                Color(0xFF0D0D1F).copy(alpha = 0.60f),
            )
        )
    } else {
        // ✅ الوضع الفاتح: تدرج خفيف جداً يبدأ من اليسار وينتهي شفاف
        Brush.linearGradient(
            colors = listOf(
                folderColor.copy(alpha = if (isDragging) 0.18f else 0.10f),
                folderColor.copy(alpha = 0.04f),
                Color.Transparent,
            )
        )
    }

    GlassCard(
        modifier     = modifier.fillMaxWidth().height(88.dp),
        cornerRadius = 20.dp,
        onClick      = onClick,
        onLongClick  = null,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(cardGradient))

        Row(
            modifier  = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {

                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(13.dp))
                        .background(folderColor.copy(alpha = if (isDark) 0.22f else 0.12f))
                        .then(dragHandle)) {
                    Text(text = folder.emoji, style = MaterialTheme.typography.titleLarge)
                }

                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text     = folder.name,
                        style    = MaterialTheme.typography.titleMedium,
                        color    = if (isDark) Color.White else Color(0xFF2E2820),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape)
                            .background(folderColor.copy(alpha = if (isDark) 1f else 0.6f)))
                        Text(
                            text  = "$channelCount ${stringResource(R.string.channel_word)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) Color.White.copy(alpha = 0.55f) else Color(0xFF2E2820).copy(alpha = 0.5f),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isProtected) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF2E2820).copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
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
}

/**
 * بطاقة القناة الزجاجية (تم إزالة أيقونة السحب بناءً على طلبك)
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
                    text  = "${formatCount(channel.subscriberCount)} ${stringResource(R.string.subscriber_word)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
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

private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 1_000     -> String.format("%.1fK", count / 1_000.0)
    else               -> count.toString()
}

/**
 * بطاقة قائمة التشغيل
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
                    text  = "${playlist.itemCount} ${stringResource(R.string.video_word)} · ${playlist.channelTitle}",
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