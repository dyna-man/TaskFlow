package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppThemeMode

@OptIn(ExperimentalTextApi::class)
@Composable
fun LoadingScreen(
    themeMode: AppThemeMode,
    onLoadingComplete: () -> Unit
) {
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Color definitions based on Light vs Dark Mode screenshots
    val bgColor = if (isDark) Color(0xFF0A0915) else Color(0xFFFDFDFF)
    val titleTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF00D2FF) else Color(0xFF0284C7)
    val trackBgColor = if (isDark) Color(0xFF1E1B38) else Color(0xFFE2E8F0)
    val loadingTextColor = if (isDark) Color(0xFF8E92B0) else Color(0xFF64748B)

    // Vibrant Gradients
    val purpleCyanGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF9333EA), // Vibrant Purple
            Color(0xFF4F46E5), // Indigo
            Color(0xFF06B6D4)  // Cyan
        )
    )

    val logoGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFFA855F7), // Light Violet
            Color(0xFF6366F1), // Indigo
            Color(0xFF0EA5E9)  // Sky Cyan
        )
    )

    // Progress animation state
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2400, easing = FastOutSlowInEasing)
        )
        onLoadingComplete()
    }

    val currentPercentage = (progressAnim.value * 100).toInt()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Background Orbital Circles, Dots & Wave Graphics
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight * 0.38f

            // 1. Orbital Rings
            val radiusInner = canvasWidth * 0.38f
            val radiusOuter = canvasWidth * 0.46f

            drawCircle(
                color = if (isDark) Color(0xFF3B3463).copy(alpha = 0.3f) else Color(0xFFE0E7FF).copy(alpha = 0.6f),
                radius = radiusInner,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )

            drawCircle(
                color = if (isDark) Color(0xFF2A244D).copy(alpha = 0.2f) else Color(0xFFE2E8F0).copy(alpha = 0.4f),
                radius = radiusOuter,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // 2. Orbital Particles / Dots
            val dots = listOf(
                Pair(centerX - radiusInner * 0.7f, centerY - radiusInner * 0.7f) to Color(0xFFA855F7),
                Pair(centerX + radiusInner * 0.85f, centerY - radiusInner * 0.5f) to Color(0xFFEC4899),
                Pair(centerX - radiusInner * 0.95f, centerY + radiusInner * 0.2f) to Color(0xFF0EA5E9),
                Pair(centerX + radiusInner * 0.75f, centerY + radiusInner * 0.65f) to Color(0xFF818CF8),
                Pair(centerX - radiusOuter * 0.3f, centerY - radiusOuter * 0.95f) to Color(0xFF38BDF8),
                Pair(centerX + radiusOuter * 0.2f, centerY + radiusOuter * 0.95f) to Color(0xFFC084FC)
            )

            dots.forEach { (pos, color) ->
                drawCircle(
                    color = color.copy(alpha = if (isDark) 0.85f else 0.7f),
                    radius = 4f,
                    center = Offset(pos.first, pos.second)
                )
            }

            // 3. Bottom Ambient Waves
            val wavePath = Path().apply {
                moveTo(0f, canvasHeight * 0.82f)
                cubicTo(
                    canvasWidth * 0.25f, canvasHeight * 0.76f,
                    canvasWidth * 0.65f, canvasHeight * 0.88f,
                    canvasWidth, canvasHeight * 0.80f
                )
                lineTo(canvasWidth, canvasHeight)
                lineTo(0f, canvasHeight)
                close()
            }

            drawPath(
                path = wavePath,
                brush = Brush.verticalGradient(
                    colors = if (isDark) listOf(
                        Color(0xFF4F46E5).copy(alpha = 0.15f),
                        Color(0xFF0A0915)
                    ) else listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.08f),
                        Color(0xFFFDFDFF)
                    )
                )
            )
        }

        // Main Center Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Taskora Icon / Badge
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .shadow(
                        elevation = if (isDark) 24.dp else 12.dp,
                        shape = RoundedCornerShape(28.dp),
                        spotColor = Color(0xFF818CF8)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(logoGradient),
                contentAlignment = Alignment.Center
            ) {
                // Checkmark symbol inside icon matching Taskora logo in images
                Canvas(modifier = Modifier.size(60.dp)) {
                    val w = size.width
                    val h = size.height

                    val path = Path().apply {
                        moveTo(w * 0.22f, h * 0.52f)
                        lineTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.82f, h * 0.28f)
                    }

                    drawPath(
                        path = path,
                        color = Color.White,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Taskora Title (Task in solid color, ora in gradient)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Task",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = titleTextColor,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "ora",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    style = TextStyle(
                        brush = purpleCyanGradient
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Subtitle: PLAN. PRIORITIZE. PERFORM.
            Text(
                text = "PLAN.  PRIORITIZE.  PERFORM.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = subtitleColor,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(72.dp))

            // Progress Section
            Text(
                text = "LOADING...",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = loadingTextColor,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Gradient Progress Bar with Glow Dot
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.82f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(trackBgColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value)
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(purpleCyanGradient)
                )

                // Glowing Tip Dot
                if (progressAnim.value > 0.05f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = (progressAnim.value * 240).dp) // approximate tracking
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Percentage Text e.g., 72%
            Text(
                text = "$currentPercentage%",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                style = TextStyle(
                    brush = purpleCyanGradient
                )
            )
        }
    }
}
