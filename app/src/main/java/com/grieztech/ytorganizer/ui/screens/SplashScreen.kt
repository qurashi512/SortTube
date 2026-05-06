package com.grieztech.ytorganizer.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.theme.*
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════
//  GriezTech - Splash Screen
//  شاشة افتتاحية مع أنيميشن Fade-in + Scale
//  → تنتقل تلقائياً لشاشة تسجيل الدخول بعد ~2.5 ثانية
// ═══════════════════════════════════════════════════════════════════

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {

    // ── أنيميشن الظهور التدريجي (Fade-in) والتكبير (Scale) ──
    val animationSpec = tween<Float>(durationMillis = 1000, easing = FastOutSlowInEasing)

    val alpha by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = animationSpec,
        label        = "splash_alpha",
    )
    val scale by animateFloatAsState(
        targetValue  = 1f,
        animationSpec = animationSpec,
        label        = "splash_scale",
    )

    // نقطة بداية: شفاف + صغير
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // إطلاق الأنيميشن
        started = true
        // انتظر انتهاء الأنيميشن (1 ث) + فترة توقف (1.5 ث)
        delay(2_500L)
        onNavigateToLogin()
    }

    // قيم الأنيميشن تبدأ من 0 وتنتهي عند 1
    val alphaValue = if (started) alpha else 0f
    val scaleValue = if (started) scale else 0.85f

    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center,
    ) {

        // ── خلفية متدرجة غنية ──
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GradientEnd.copy(alpha = 0.95f),
                        GradientStart,
                        GradientMid,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.4f),
                    radius = size.width * 1.1f,
                )
            )
            // دوائر زخرفية
            drawCircle(
                color  = AccentPurple.copy(alpha = 0.20f),
                radius = size.width * 0.55f,
                center = Offset(size.width * 0.85f, size.height * 0.12f),
            )
            drawCircle(
                color  = YouTubeRed.copy(alpha = 0.12f),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.1f, size.height * 0.80f),
            )
        }

        // ── المحتوى الرئيسي ──
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 40.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── أيقونة التطبيق + اسمه في المنتصف تماماً ──
            Column(
                modifier            = Modifier
                    .alpha(alphaValue)
                    .scale(scaleValue),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // أيقونة
                Icon(
                    imageVector        = Icons.Rounded.PlayCircle,
                    contentDescription = stringResource(R.string.app_name), // ✅
                    tint               = YouTubeRed,
                    modifier           = Modifier.size(96.dp),
                )

                // اسم التطبيق
                Text(
                    text       = stringResource(R.string.app_name), // ✅
                    fontSize   = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color.White,
                    textAlign  = TextAlign.Center,
                )

                // وصف خفيف
                Text(
                    text      = stringResource(R.string.splash_subtitle), // ✅
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = Color.White.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── نص الصانع في الأسفل ──
        Box(
            modifier          = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 36.dp)
                .alpha(alphaValue)
                .scale(scaleValue),
            contentAlignment  = Alignment.BottomCenter,
        ) {
            Text(
                text      = stringResource(R.string.splash_crafted_by), // ✅
                style     = MaterialTheme.typography.bodyMedium,
                color     = Color.White.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                fontSize  = 14.sp,
            )
        }
    }
}