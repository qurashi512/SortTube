package com.grieztech.ytorganizer.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grieztech.ytorganizer.R
import com.grieztech.ytorganizer.ui.theme.AccentPurple
import com.grieztech.ytorganizer.ui.theme.YouTubeRed

private data class OnboardingPage(
    val emoji: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val color: Color,
)

private val pages = listOf(
    OnboardingPage(
        emoji = "📁",
        titleRes = R.string.onboarding_title_1,
        descRes = R.string.onboarding_desc_1,
        color = AccentPurple,
    ),
    OnboardingPage(
        emoji = "🔄",
        titleRes = R.string.onboarding_title_2,
        descRes = R.string.onboarding_desc_2,
        color = Color(0xFF4488FF),
    ),
    OnboardingPage(
        emoji = "👆",
        titleRes = R.string.onboarding_title_3,
        descRes = R.string.onboarding_desc_3,
        color = Color(0xFF44CC88),
    ),
    OnboardingPage(
        emoji = "⬅️",
        titleRes = R.string.onboarding_title_4,
        descRes = R.string.onboarding_desc_4,
        color = YouTubeRed,
    ),
    OnboardingPage(
        emoji = "🎬",
        titleRes = R.string.onboarding_title_5,
        descRes = R.string.onboarding_desc_5,
        color = Color(0xFFFF8800),
    ),
    OnboardingPage(
        emoji = "🚀",
        titleRes = R.string.onboarding_title_6,
        descRes = R.string.onboarding_desc_6,
        color = AccentPurple,
    ),
)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    var currentPage by remember { mutableIntStateOf(0) }
    val page = pages[currentPage]

    // ── لون الخلفية يتغير بسلاسة مع كل صفحة ──
    val bgColor by animateColorAsState(
        targetValue = page.color.copy(alpha = 0.12f),
        animationSpec = tween(600),
        label = "bg"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D0D1A), bgColor, Color(0xFF0A0A14))
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            // ── نقاط التقدم في الأعلى ──
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { i ->
                    val isActive = i == currentPage
                    val width by animateDpAsState(
                        targetValue = if (isActive) 28.dp else 8.dp,
                        animationSpec = spring(dampingRatio = 0.6f),
                        label = "dot_width"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isActive) page.color else Color.White.copy(0.25f),
                        animationSpec = tween(300),
                        label = "dot_color"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // ── المحتوى الرئيسي ──
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    (slideInHorizontally { it / 2 } + fadeIn(tween(300))).togetherWith(
                        slideOutHorizontally { -it / 2 } + fadeOut(tween(200))
                    )
                },
                label = "page_content"
            ) { pageIndex ->
                val p = pages[pageIndex]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    // الإيموجي الكبير
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(p.color.copy(0.3f), p.color.copy(0.05f))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(p.emoji, fontSize = 52.sp)
                    }

                    // العنوان
                    Text(
                        text = stringResource(id = p.titleRes),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                    )

                    // الوصف
                    Text(
                        text = stringResource(id = p.descRes),
                        color = Color.White.copy(0.65f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp,
                    )
                }
            }

            // ── الأزرار في الأسفل ──
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // الزرار الرئيسي
                Button(
                    onClick = {
                        if (currentPage < pages.lastIndex) currentPage++
                        else onDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = page.color),
                ) {
                    Text(
                        text = if (currentPage < pages.lastIndex) stringResource(R.string.next_btn) else stringResource(R.string.start_btn),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }

                // زرار التخطي — يختفي في الصفحة الأخيرة
                AnimatedVisibility(visible = currentPage < pages.lastIndex) {
                    TextButton(onClick = onDone) {
                        Text(
                            text = stringResource(R.string.skip_btn),
                            color = Color.White.copy(0.4f),
                            fontSize = 14.sp,
                        )
                    }
                }
            }
        }
    }
}