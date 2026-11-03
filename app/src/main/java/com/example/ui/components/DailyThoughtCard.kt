package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DailyThoughtProvider

// Warm Color Palette for 365 Daily Thoughts System
val WarmAmberGold = Color(0xFFF59E0B)
val WarmCoralSunset = Color(0xFFE11D48)
val WarmTerracotta = Color(0xFFC2410C)
val WarmCreamBg = Color(0xFFFFFBEB)
val WarmHoneyBg = Color(0xFFFEF3C7)
val WarmPeachBg = Color(0xFFFFEDD5)
val WarmTextEspresso = Color(0xFF1F120B)
val WarmTextMuted = Color(0xFF92400E)
val WarmBorderGold = Color(0xFFFCD34D)
val WarmDarkBgStart = Color(0xFF27170F)
val WarmDarkBgMiddle = Color(0xFF331B10)
val WarmDarkBgEnd = Color(0xFF23120B)

@Composable
fun DailyThoughtCard(
    modifier: Modifier = Modifier,
    compactMode: Boolean = false
) {
    val thought = remember { DailyThoughtProvider.getThoughtForToday() }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.3f

    val cardBgGradient = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                WarmDarkBgStart,
                WarmDarkBgMiddle,
                WarmDarkBgEnd
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                WarmCreamBg,
                WarmHoneyBg,
                WarmPeachBg
            )
        )
    }

    val borderColor = if (isDark) WarmCoralSunset.copy(alpha = 0.5f) else WarmBorderGold

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBgGradient)
            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
            .padding(if (compactMode) 12.dp else 16.dp)
            .testTag("daily_thought_card")
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = if (isDark) WarmBorderGold else WarmAmberGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "365 Daily Thought",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) WarmBorderGold else WarmTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Thought Text Content
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.FormatQuote,
                        contentDescription = "Quote",
                        tint = if (isDark) WarmCoralSunset.copy(alpha = 0.7f) else WarmAmberGold,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = thought.quote,
                        fontSize = if (compactMode) 13.sp else 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFFF7ED) else WarmTextEspresso,
                        lineHeight = if (compactMode) 18.sp else 22.sp,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "— ${thought.author.uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) WarmBorderGold else WarmTerracotta,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
