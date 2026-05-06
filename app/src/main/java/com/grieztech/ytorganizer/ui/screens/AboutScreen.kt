package com.grieztech.ytorganizer.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.components.GlassCard
import com.grieztech.ytorganizer.ui.components.GlassTopBar
import com.grieztech.ytorganizer.ui.theme.AccentPurple
import com.grieztech.ytorganizer.ui.theme.YouTubeRed

// ═══════════════════════════════════════
//  GriezTech - About / Developer Screen
//  شاشة عن المطوّر
// ═══════════════════════════════════════

@Composable
fun AboutScreen(onBack: () -> Unit) {

    val context = LocalContext.current

    // هذه البيانات تترك ثابتة لأنها لا تتغير بتغير اللغة
    val developerName   = "Gorashe Mohamed"
    val developerNameAr = "قرشي محمد أحمد"
    val email           = "gorashe.suliman@outlook.com"
    val phone           = "01010736525"
    val appVersion      = "SortTube v1.0.0"

    Box(modifier = Modifier.fillMaxSize()) {

        // ── خلفية متدرجة داكنة ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF12122A), Color(0xFF0A0A14))
                )
            )
            drawCircle(
                color  = AccentPurple.copy(0.15f),
                radius = size.width * 0.50f,
                center = Offset(size.width * 0.85f, size.height * 0.10f),
            )
            drawCircle(
                color  = YouTubeRed.copy(0.08f),
                radius = size.width * 0.40f,
                center = Offset(size.width * 0.10f, size.height * 0.80f),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {

            // استخدام النص من القاموس
            GlassTopBar(title = stringResource(R.string.about_developer), onBack = onBack)

            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                Spacer(Modifier.height(8.dp))

                // ── بطاقة هوية المطوّر ──
                GlassCard(
                    modifier     = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp,
                ) {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier         = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary,
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.Rounded.Person,
                                contentDescription = null,
                                tint               = Color.White,
                                modifier           = Modifier.size(44.dp),
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text       = developerName,
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )

                        Text(
                            text  = developerNameAr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.70f),
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        ) {
                            Text(
                                // استخدام النص من القاموس
                                text     = stringResource(R.string.developer_title),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                style    = MaterialTheme.typography.labelMedium,
                                color    = Color.White,
                            )
                        }
                    }
                }

                // ── بطاقة التواصل ──
                GlassCard(
                    modifier     = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                ) {
                    Column(
                        modifier            = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            // استخدام النص من القاموس
                            text  = stringResource(R.string.contact_me),
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.60f),
                        )

                        Spacer(Modifier.height(4.dp))

                        ContactRow(
                            icon    = Icons.Rounded.Email,
                            label   = email,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:$email")
                                }
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color    = Color.White.copy(alpha = 0.12f),
                        )

                        ContactRow(
                            icon    = Icons.Rounded.Phone,
                            label   = phone,
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:$phone")
                                }
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text  = appVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.35f),
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── صف تواصل قابل للنقر ────────────────────────────────────────
@Composable
private fun ContactRow(
    icon    : ImageVector,
    label   : String,
    onClick : () -> Unit,
) {
    TextButton(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector        = Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint               = Color.White.copy(alpha = 0.30f),
            modifier           = Modifier.size(16.dp),
        )
    }
}