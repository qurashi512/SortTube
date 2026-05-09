package com.grieztech.ytorganizer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.grieztech.ytorganizer.ui.theme.*

// ═══════════════════════════════════════
//  GriezTech - Glass Components
//  العناصر الزجاجية المشتركة
// ═══════════════════════════════════════

/**
 * بطاقة زجاجية شفافة (Glassmorphism Card)
 * تُستخدم لعرض المجلدات والقنوات
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCard(
    modifier       : Modifier = Modifier,
    cornerRadius   : Dp = 20.dp,
    borderWidth    : Dp = 1.dp,
    glassAlpha     : Float = 0.15f,
    onClick        : (() -> Unit)? = null,
    onLongClick    : (() -> Unit)? = null,   // ✅ إصلاح 2: إضافة معامل onLongClick
    content        : @Composable BoxScope.() -> Unit,
) {
    val glassColors = LocalGlassColors.current
    val isDark      = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    val glassBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.12f else 0.55f),
            Color.White.copy(alpha = if (isDark) 0.06f else 0.30f),
        ),
        start = Offset(0f, 0f),
        end   = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )

    val borderBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.40f),
                Color.White.copy(alpha = 0.10f),
            ),
        )
    } else {
        // ✅ الوضع الفاتح: border بلون بيج/رمادي دافي يظهر على كل الجوانب
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFB8A99A).copy(alpha = 0.50f),
                Color(0xFFB8A99A).copy(alpha = 0.30f),
                Color(0xFFB8A99A).copy(alpha = 0.50f),
            ),
        )
    }

    val baseModifier = modifier
        .clip(RoundedCornerShape(cornerRadius))
        .background(glassBrush)
        .border(
            width  = borderWidth,
            brush  = borderBrush,
            shape  = RoundedCornerShape(cornerRadius),
        )

    // ✅ إصلاح 2: استخدام combinedClickable لدعم onClick + onLongClick معاً
    if (onClick != null || onLongClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier = baseModifier.combinedClickable(
                interactionSource = interactionSource,
                indication        = ripple(),
                onClick           = onClick ?: {},
                onLongClick       = onLongClick,
            ),
            content = content,
        )
    } else {
        Box(modifier = baseModifier, content = content)
    }
}

/**
 * زر عائم زجاجي - FAB
 */
@Composable
fun GlassFAB(
    onClick : () -> Unit,
    icon    : ImageVector,
    label   : String? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean  = false,
    tint    : Color    = MaterialTheme.colorScheme.primary,
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue   = 0.3f,
        targetValue    = 0.7f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .shadow(
                elevation    = 16.dp,
                shape        = if (label != null) RoundedCornerShape(16.dp) else CircleShape,
                ambientColor = tint.copy(alpha = glowAlpha),
                spotColor    = tint.copy(alpha = glowAlpha),
            )
            .clip(if (label != null) RoundedCornerShape(16.dp) else CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        tint.copy(alpha = if (isDark) 0.30f else 0.85f),
                        tint.copy(alpha = if (isDark) 0.15f else 0.60f),
                    )
                )
            )
            .border(
                width  = 1.dp,
                brush  = Brush.linearGradient(
                    listOf(Color.White.copy(0.5f), Color.White.copy(0.1f))
                ),
                shape  = if (label != null) RoundedCornerShape(16.dp) else CircleShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (label != null) 20.dp else 0.dp)
            .height(56.dp)
            .then(if (label == null) Modifier.width(56.dp) else Modifier),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
            modifier              = Modifier.padding(horizontal = if (label != null) 4.dp else 0.dp),
        ) {
            Icon(
                imageVector       = icon,
                contentDescription = label,
                tint              = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary,
                modifier          = Modifier.size(24.dp),
            )
            AnimatedVisibility(visible = label != null && expanded) {
                Text(
                    text  = label ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

/**
 * شريط علوي زجاجي (Top App Bar)
 */
@Composable
fun GlassTopBar(
    title     : String,
    subtitle  : String? = null,
    onBack    : (() -> Unit)? = null,
    actions   : @Composable RowScope.() -> Unit = {},
    modifier  : Modifier = Modifier,
) {
    val isDark = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }

    GlassCard(
        modifier     = modifier.fillMaxWidth(),
        cornerRadius = 0.dp,
        glassAlpha   = 0.1f,
    ) {
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment    = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                Column {
                    Text(
                        text  = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    if (subtitle != null) {
                        Text(
                            text  = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        )
                    }
                }
            }
            Row(content = actions)
        }
    }
}

@Composable
fun FloatingBottomBar(onHomeClick: () -> Unit, onProfileClick: () -> Unit, onSettingsClick: () -> Unit, isSettingsActive: Boolean) {
    val isDark      = !MaterialTheme.colorScheme.background.luminance().let { it > 0.5f }
    val barColor    = if (isDark) Color(0xFF2A2A2A) else Color(0xFFFFFFFF)
    val activeColor = if (isDark) Color(0xFF484848) else Color(0xFFE8E8E8)
    val iconActive  = if (isDark) Color.White       else Color(0xFF1A1A1A)
    val iconInactive= if (isDark) Color.White.copy(0.55f) else Color(0xFF1A1A1A).copy(0.45f)

    Box(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = barColor,
            shadowElevation = if (isDark) 12.dp else 8.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // الرئيسية
                Box(
                    modifier = Modifier.height(52.dp).width(64.dp).clip(CircleShape)
                        .background(if (!isSettingsActive) activeColor else Color.Transparent)
                        .clickable(onClick = onHomeClick),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Home, null, tint = if (!isSettingsActive) iconActive else iconInactive, modifier = Modifier.size(26.dp)) }
                // الحساب
                Box(
                    modifier = Modifier.height(52.dp).width(64.dp).clip(CircleShape)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AccountCircle, null, tint = iconInactive, modifier = Modifier.size(26.dp)) }
                // الإعدادات
                Box(
                    modifier = Modifier.height(52.dp).width(64.dp).clip(CircleShape)
                        .background(if (isSettingsActive) activeColor else Color.Transparent)
                        .clickable(onClick = onSettingsClick),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Settings, null, tint = if (isSettingsActive) iconActive else iconInactive, modifier = Modifier.size(26.dp)) }
            }
        }
    }
}
