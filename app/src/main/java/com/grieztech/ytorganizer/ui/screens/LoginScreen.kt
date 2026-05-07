package com.grieztech.ytorganizer.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.grieztech.ytorganizer.ui.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.components.GlassCard
import com.grieztech.ytorganizer.ui.theme.*
import com.grieztech.ytorganizer.ui.viewmodel.LoginViewModel

// ═══════════════════════════════════════
//  GriezTech - Login Screen
//  شاشة تسجيل الدخول
// ═══════════════════════════════════════

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel     : LoginViewModel = hiltViewModel(),
    homeViewModel : HomeViewModel  = hiltViewModel(),
) {
    val context   = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val error     by viewModel.error.collectAsState()

    // تأثير الدوران للأيقونة
    val infiniteTransition = rememberInfiniteTransition(label = "logo_float")
    val logoOffset by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = -12f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_y",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 0.9f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    // Google Sign-In Launcher
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken("647341706002-nmnklnalbqgkggrnkp54nq13k7ckb6o7.apps.googleusercontent.com")
        .requestServerAuthCode("647341706002-nmnklnalbqgkggrnkp54nq13k7ckb6o7.apps.googleusercontent.com")
        .requestScopes(
            com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/youtube.readonly")
        )
        .build()

    val signInClient = GoogleSignIn.getClient(context, gso)

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task    = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            viewModel.onGoogleSignInResult(account) {
                // ✅ أبلغ HomeViewModel بالحساب الجديد
                //    إذا تغيّر الحساب يمسح القديم، وإذا نفس الحساب تظهر المجلدات مباشرة
                // ✅ نرسل email لأنه دائماً موجود وغير null
                //    بخلاف account.id الذي قد يكون null
                val accountEmail = account?.email ?: ""
                homeViewModel.checkAndClearIfAccountChanged(accountEmail)
                onLoginSuccess()
            }
        } catch (e: ApiException) {
            // ✅ استخدام context.getString لأننا خارج واجهة المستخدم (في Callback)
            val errorPrefix = context.getString(R.string.login_failed)
            viewModel.onSignInError("$errorPrefix ${e.statusCode}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── خلفية متحركة ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(GradientEnd, GradientMid, GradientStart),
                    center = Offset(size.width * 0.5f, size.height * 0.3f),
                    radius = size.width,
                )
            )
            drawCircle(
                color  = YouTubeRed.copy(alpha = glowAlpha * 0.2f),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.5f, size.height * 0.25f),
            )
            drawCircle(
                color  = AccentPurple.copy(alpha = 0.12f),
                radius = size.width * 0.5f,
                center = Offset(size.width * 0.8f, size.height * 0.6f),
            )
        }

        Column(
            modifier              = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(32.dp),
            verticalArrangement   = Arrangement.SpaceBetween,
            horizontalAlignment   = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))

            // ── الشعار ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // أيقونة يوتيوب متحركة
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .offset(y = logoOffset.dp),
                ) {
                    // دائرة توهج خلفية
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(1f + glowAlpha * 0.1f)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        YouTubeRed.copy(alpha = glowAlpha * 0.5f),
                                        Color.Transparent,
                                    )
                                )
                            )
                        }
                    }
                    Icon(
                        imageVector        = Icons.Rounded.PlayCircle,
                        contentDescription = "GriezTech",
                        tint               = YouTubeRed,
                        modifier           = Modifier.size(80.dp),
                    )
                }

                Text(
                    text       = "GriezTech", // تُترك كما هي لأنها اسم التطبيق
                    fontSize   = 42.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                )

                Text(
                    text      = stringResource(R.string.app_description), // ✅
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            // ── بطاقة تسجيل الدخول ──
            GlassCard(
                modifier     = Modifier.fillMaxWidth(),
                cornerRadius = 28.dp,
                glassAlpha   = 0.15f,
            ) {
                Column(
                    modifier              = Modifier.padding(28.dp),
                    verticalArrangement   = Arrangement.spacedBy(16.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text  = stringResource(R.string.start_your_journey), // ✅
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                    )
                    Text(
                        text      = stringResource(R.string.login_description), // ✅
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = Color.White.copy(alpha = 0.65f),
                        textAlign = TextAlign.Center,
                    )

                    // رسالة خطأ
                    AnimatedVisibility(visible = error != null) {
                        error?.let {
                            Text(
                                text  = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    // زر تسجيل الدخول
                    Button(
                        onClick  = { launcher.launch(signInClient.signInIntent) },
                        enabled  = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor   = Color(0xFF1A1A2E),
                        ),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                color       = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text(
                                text  = stringResource(R.string.login_with_google), // ✅
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                    }

                    Text(
                        text  = stringResource(R.string.terms_and_privacy_agreement), // ✅
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.40f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}