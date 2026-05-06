package com.grieztech.ytorganizer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.components.GlassCard
import com.grieztech.ytorganizer.ui.components.GlassTopBar
import com.grieztech.ytorganizer.ui.theme.*

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - Settings Screen  (FIXED – real switching)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(
    onBack          : () -> Unit,
    onAboutClick    : () -> Unit,
    onLogout        : () -> Unit,
    onThemeChange   : (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    currentTheme    : String = "system",
    currentLang     : String = "ar",
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── خلفية متدرجة غنية ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), Color(0xFF12122A), Color(0xFF0A0A14))
                )
            )
            drawCircle(color = AccentPurple.copy(0.18f), radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.08f))
            drawCircle(color = YouTubeRed.copy(0.10f), radius = size.width * 0.40f,
                center = Offset(size.width * 0.1f, size.height * 0.85f))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            GlassTopBar(title = stringResource(R.string.settings), onBack = onBack) // ✅
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                // ════════════════ المظهر ════════════════
                SectionTitle("🎨  " + stringResource(R.string.appearance)) // ✅
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigThemeChip(
                                label = "🌙  " + stringResource(R.string.dark_mode), selected = currentTheme == "dark", // ✅
                                selectedColor = AccentPurple, modifier = Modifier.weight(1f)
                            ) { onThemeChange("dark") }
                            BigThemeChip(
                                label = "☀️  " + stringResource(R.string.light_mode), selected = currentTheme == "light", // ✅
                                selectedColor = Color(0xFFFFAA00), modifier = Modifier.weight(1f)
                            ) { onThemeChange("light") }
                            BigThemeChip(
                                label = "⚙️  " + stringResource(R.string.system_mode), selected = currentTheme == "system", // ✅
                                selectedColor = AccentBlue, modifier = Modifier.weight(1f)
                            ) { onThemeChange("system") }
                        }
                        // مؤشر الوضع النشط
                        val darkActive = stringResource(R.string.dark_mode_active)
                        val lightActive = stringResource(R.string.light_mode_active)
                        val systemActive = stringResource(R.string.system_mode_active)

                        val activeLabel = when(currentTheme) {
                            "dark"  -> darkActive // ✅
                            "light" -> lightActive // ✅
                            else    -> systemActive // ✅
                        }
                        Text(
                            text = activeLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(0.45f),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                        )
                    }
                }

                // ════════════════ اللغة ════════════════
                SectionTitle("🌐  " + stringResource(R.string.language)) // ✅
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            BigThemeChip(
                                label = "🇸🇦  " + stringResource(R.string.arabic), selected = currentLang == "ar", // ✅
                                selectedColor = AccentTeal, modifier = Modifier.weight(1f)
                            ) { onLanguageChange("ar") }
                            BigThemeChip(
                                label = "🇬🇧  " + stringResource(R.string.english), selected = currentLang == "en", // ✅
                                selectedColor = AccentBlue, modifier = Modifier.weight(1f)
                            ) { onLanguageChange("en") }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text  = "⚠️  " + stringResource(R.string.language_change_warning), // ✅
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFAA00).copy(0.8f),
                        )
                    }
                }

                // ════════════════ الحساب ════════════════
                SectionTitle("👤  " + stringResource(R.string.account)) // ✅
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // عرض بيانات المستخدم
                        val account = remember { GoogleSignIn.getLastSignedInAccount(context) }
                        if (account != null) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                // أفاتار بالحرف الأول
                                Box(contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Brush.linearGradient(
                                            listOf(AccentPurple, YouTubeRed)))) {
                                    Text(
                                        text = account.displayName?.firstOrNull()?.toString() ?: "G",
                                        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(account.displayName ?: "", style = MaterialTheme.typography.titleSmall,
                                        color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(account.email ?: "", style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(0.55f))
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(0.10f))
                            Spacer(Modifier.height(14.dp))
                        }
                        SettingsRow(stringResource(R.string.logout), Icons.Rounded.Logout, Color(0xFFFF4455)) { // ✅
                            showLogoutDialog = true
                        }
                    }
                }

                // ════════════════ عن التطبيق ════════════════
                SectionTitle("ℹ️  " + stringResource(R.string.about_app)) // ✅
                GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsRow(stringResource(R.string.about_developer), Icons.Rounded.Person, AccentPurple) { // ✅
                            onAboutClick()
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color.White.copy(0.10f))
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.version), style = MaterialTheme.typography.bodySmall, // ✅
                                color = Color.White.copy(0.45f))
                            Text("SortTube v1.0.0", style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(0.85f))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.crafted_by_full), // ✅
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(0.28f),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 20.dp))
            }
        }
    }

    // ── حوار تأكيد الخروج ──
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = Color(0xFF1A1A2E),
            shape            = RoundedCornerShape(24.dp),
            title  = { Text(stringResource(R.string.logout_title), color = Color.White, fontWeight = FontWeight.Bold) }, // ✅
            text   = { Text(stringResource(R.string.logout_desc), color = Color.White.copy(0.7f)) }, // ✅
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                        GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                            onLogout()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4455)),
                ) { Text(stringResource(R.string.logout)) } // ✅
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showLogoutDialog = false },
                    colors  = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) { Text(stringResource(R.string.cancel)) } // ✅
            }
        )
    }
}

@Composable
private fun BigThemeChip(
    label: String, selected: Boolean, selectedColor: Color,
    modifier: Modifier, onClick: () -> Unit,
) {
    val bg     = if (selected) selectedColor.copy(0.28f) else Color.White.copy(0.06f)
    val border = if (selected) selectedColor.copy(0.70f) else Color.White.copy(0.12f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            color      = if (selected) Color.White else Color.White.copy(0.45f),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SettingsRow(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(color.copy(0.18f))) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = Color.White, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(0.3f),
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge,
        color = Color.White.copy(0.55f), modifier = Modifier.padding(start = 4.dp))
}