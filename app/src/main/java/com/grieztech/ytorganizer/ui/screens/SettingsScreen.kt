package com.grieztech.ytorganizer.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.components.GlassCard
import com.grieztech.ytorganizer.ui.components.GlassTopBar
import com.grieztech.ytorganizer.ui.components.FloatingBottomBar
import com.grieztech.ytorganizer.ui.theme.*
import com.grieztech.ytorganizer.ui.viewmodel.HomeViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack          : () -> Unit,
    onAboutClick    : () -> Unit,
    onLogout        : () -> Unit,
    homeViewModel   : HomeViewModel = hiltViewModel(),
    onThemeChange   : (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    currentTheme    : String = "system",
    currentLang     : String = "ar",
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showUserMenu by remember { mutableStateOf(false) }

    // استخدام ألوان السمة الديناميكية
    val glassColors = LocalGlassColors.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    val isLightMode = backgroundColor.luminance() > 0.5f
    val warningTextColor = if (isLightMode) Color(0xFFC06000) else Color(0xFFFFAA00)

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {

        // ── خلفية متدرجة ذكية تتكيف مع الوضع الفاتح والداكن ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(glassColors.gradientEnd.copy(alpha = 0.95f), glassColors.gradientStart, glassColors.gradientMid)
                )
            )
            drawCircle(color = AccentPurple.copy(0.18f), radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.08f))
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                FloatingBottomBar(
                    onHomeClick = onBack,
                    onProfileClick = { showUserMenu = true },
                    onSettingsClick = { /* نحن هنا بالفعل */ },
                    isSettingsActive = true
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            ) {
                GlassTopBar(title = stringResource(R.string.settings), onBack = onBack)
                Spacer(Modifier.height(20.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {

                    // ════════════════ المظهر ════════════════
                    SectionTitle("🎨  " + stringResource(R.string.appearance))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BigThemeChip(
                                    label = "🌙  " + stringResource(R.string.dark_mode), selected = currentTheme == "dark",
                                    selectedColor = AccentPurple, modifier = Modifier.weight(1f)
                                ) { onThemeChange("dark") }
                                BigThemeChip(
                                    label = "☀️  " + stringResource(R.string.light_mode), selected = currentTheme == "light",
                                    selectedColor = Color(0xFFFFAA00), modifier = Modifier.weight(1f)
                                ) { onThemeChange("light") }
                                BigThemeChip(
                                    label = "⚙️  " + stringResource(R.string.system_mode), selected = currentTheme == "system",
                                    selectedColor = AccentBlue, modifier = Modifier.weight(1f)
                                ) { onThemeChange("system") }
                            }
                            Text(
                                text = when(currentTheme) {
                                    "dark"  -> stringResource(R.string.dark_mode_active)
                                    "light" -> stringResource(R.string.light_mode_active)
                                    else    -> stringResource(R.string.system_mode_active)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = onBackgroundColor.copy(0.65f), // وضوح أكبر
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                            )
                        }
                    }

                    // ════════════════ اللغة ════════════════
                    SectionTitle("🌐  " + stringResource(R.string.language))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                BigThemeChip(
                                    label = "🇸🇦  " + stringResource(R.string.arabic), selected = currentLang == "ar",
                                    selectedColor = AccentTeal, modifier = Modifier.weight(1f)
                                ) { onLanguageChange("ar") }
                                BigThemeChip(
                                    label = "🇬🇧  " + stringResource(R.string.english), selected = currentLang == "en",
                                    selectedColor = AccentBlue, modifier = Modifier.weight(1f)
                                ) { onLanguageChange("en") }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text  = "⚠️  " + stringResource(R.string.language_change_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = warningTextColor,
                            )
                        }
                    }

                    // ════════════════ الحساب ════════════════
                    SectionTitle("👤  " + stringResource(R.string.account))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val account = remember { GoogleSignIn.getLastSignedInAccount(context) }
                            if (account != null) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                            color = onBackgroundColor, fontWeight = FontWeight.SemiBold)
                                        Text(account.email ?: "", style = MaterialTheme.typography.bodySmall,
                                            color = onBackgroundColor.copy(0.75f))
                                    }
                                }
                                Spacer(Modifier.height(16.dp))
                                HorizontalDivider(color = onBackgroundColor.copy(0.10f))
                                Spacer(Modifier.height(14.dp))
                            }
                            SettingsRow(stringResource(R.string.logout), Icons.Rounded.Logout, Color(0xFFFF4455)) {
                                showLogoutDialog = true
                            }
                        }
                    }

                    // ════════════════ عن التطبيق ════════════════
                    SectionTitle("ℹ️  " + stringResource(R.string.about_app))
                    GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20.dp) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SettingsRow(stringResource(R.string.about_developer), Icons.Rounded.Person, AccentPurple) {
                                onAboutClick()
                            }
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = onBackgroundColor.copy(0.10f))
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(stringResource(R.string.version), style = MaterialTheme.typography.bodySmall,
                                    color = onBackgroundColor.copy(0.65f))
                                Text("SortTube v1.0.0", style = MaterialTheme.typography.bodySmall,
                                    color = onBackgroundColor.copy(0.85f))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.crafted_by_full),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBackgroundColor.copy(0.85f), // تم زيادة الوضوح ليكون ممتازاً في اللون الفاتح
                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        textAlign = TextAlign.Center)
                }
            }
        }
    }

    if (showUserMenu) {
        val account = remember { GoogleSignIn.getLastSignedInAccount(context) }
        AlertDialog(
            onDismissRequest = { showUserMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Person, null, tint = AccentPurple)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.account), color = MaterialTheme.colorScheme.onSurface)
            }},
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${account?.displayName}\n${account?.email}", color = MaterialTheme.colorScheme.onSurface)
                    Button(
                        onClick = { showUserMenu = false; showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(0.8f))
                    ) {
                        Icon(Icons.Rounded.Logout, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.logout), color = Color.White)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showUserMenu = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor   = MaterialTheme.colorScheme.surface,
            shape            = RoundedCornerShape(24.dp),
            title  = { Text(stringResource(R.string.logout_title), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text   = { Text(stringResource(R.string.logout_desc), color = MaterialTheme.colorScheme.onSurface.copy(0.7f)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        homeViewModel.logout {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                            GoogleSignIn.getClient(context, gso).signOut().addOnCompleteListener {
                                onLogout()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4455)),
                ) { Text(stringResource(R.string.logout), color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}



@Composable
private fun BigThemeChip(
    label: String, selected: Boolean, selectedColor: Color,
    modifier: Modifier, onClick: () -> Unit,
) {
    val bg     = if (selected) selectedColor.copy(0.28f) else MaterialTheme.colorScheme.onSurface.copy(0.06f)
    val border = if (selected) selectedColor.copy(0.70f) else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if(selected) Modifier.border(1.dp, border, RoundedCornerShape(14.dp)) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
    ) {
        Text(
            text       = label,
            style      = MaterialTheme.typography.labelMedium,
            color      = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.45f),
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
            color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronLeft, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f),
            modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(0.75f), modifier = Modifier.padding(start = 4.dp))
}