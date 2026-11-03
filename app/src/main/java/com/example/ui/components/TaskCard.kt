package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMediumYellow
import com.example.ui.theme.TaskHighAccent
import com.example.ui.theme.TaskLowAccent
import com.example.ui.theme.TaskMedAccent
import com.example.ui.theme.TertiaryOrange

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    onToggle: () -> Unit,
    onToggleStarred: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (priorityAccent, priorityBgLight, priorityBorder) = when (task.priority.lowercase()) {
        "high" -> Triple(TaskHighAccent, Color(0xFFFEF2F2), Color(0xFFFCA5A5))
        "medium" -> Triple(TaskMedAccent, Color(0xFFFFFBEB), Color(0xFFFCD34D))
        "low" -> Triple(TaskLowAccent, Color(0xFFECFDF5), Color(0xFF6EE7B7))
        else -> Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.outlineVariant)
    }

    // Adapt to dark theme or light theme gracefully
    val isDark = MaterialTheme.colorScheme.surface.red < 0.3f
    val containerColor = if (task.isCompleted) {
        if (isDark) Color(0xFF1A1D26) else Color(0xFFF8FAFC)
    } else {
        if (isDark) {
            when (task.priority.lowercase()) {
                "high" -> Color(0xFF2C191C)
                "medium" -> Color(0xFF2B2213)
                "low" -> Color(0xFF132A20)
                else -> MaterialTheme.colorScheme.surfaceContainerLow
            }
        } else {
            priorityBgLight
        }
    }

    val cardBorderColor = if (task.isCompleted) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    } else {
        if (isDark) priorityAccent.copy(alpha = 0.4f) else priorityBorder
    }

    val projectBadgeColors = when (task.project.lowercase()) {
        "core work" -> Pair(Color(0xFF6366F1), Color(0xFFEEF2FF))
        "personal" -> Pair(Color(0xFFEA580C), Color(0xFFFFF7ED))
        "health", "fitness" -> Pair(Color(0xFF059669), Color(0xFFECFDF5))
        "learning" -> Pair(Color(0xFFD97706), Color(0xFFFEFCE8))
        else -> Pair(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.2.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { onToggle() },
                onLongClick = onLongClick
            )
            .alpha(if (task.isCompleted) 0.65f else 1.0f)
            .testTag("task_card_${task.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Accent Strip indicating task priority/warn color
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .background(if (task.isCompleted) Color.Gray.copy(alpha = 0.4f) else priorityAccent)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = task.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            if (task.project.isNotBlank()) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) projectBadgeColors.first.copy(alpha = 0.2f) else projectBadgeColors.second)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = task.project,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = projectBadgeColors.first
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val scheduleText = when (task.scheduleType) {
                                "Forever" -> "Everyday (365 days)"
                                "Two days", "2 to 5 days" -> task.selectedDates
                                "Multiple days" -> task.selectedDates
                                else -> if (task.selectedDates.isNotBlank() && task.selectedDates != task.date) task.selectedDates else task.scheduledTime
                            }

                            val timeIcon = when (task.timeOfDay.lowercase()) {
                                "morning" -> "🌅 Morning"
                                "afternoon" -> "☀️ Afternoon"
                                "evening" -> "🌙 Evening"
                                else -> "🕒 ${task.timeOfDay}"
                            }

                            Text(
                                text = scheduleText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (task.timeOfDay.isNotBlank()) {
                                Text(
                                    text = " • $timeIcon",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = priorityAccent
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Star Icon
                    if (onToggleStarred != null) {
                        IconButton(
                            onClick = { onToggleStarred() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (task.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star Task",
                                tint = if (task.isStarred) TertiaryOrange else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Checkbox Icon
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) PriorityLowGreen else priorityAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

