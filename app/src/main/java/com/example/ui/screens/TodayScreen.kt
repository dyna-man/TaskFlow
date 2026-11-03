package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.DailyThoughtCard
import com.example.ui.components.DeleteTaskConfirmDialog
import com.example.ui.components.FocusTaskCard
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskOptionsDialog
import com.example.ui.screens.NewTaskScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TaskEntity
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.TertiaryOrange
import com.example.util.DailyThoughtProvider
import com.example.ui.viewmodel.TaskViewModel

import com.example.data.model.isScheduledForDate

@Composable
fun TodayScreen(
    viewModel: TaskViewModel,
    onNavigateToProfile: () -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    var completedExpanded by remember { mutableStateOf(false) }

    var taskForOptions by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    var todayDateStr by remember {
        mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            if (nowStr != todayDateStr) {
                todayDateStr = nowStr
            }
            delay(10000)
        }
    }

    val todayTasks = remember(tasks, todayDateStr) {
        tasks.filter { it.isScheduledForDate(todayDateStr) }
    }

    val morningTasks = todayTasks.filter { it.timeOfDay.equals("Morning", ignoreCase = true) && !it.isCompleted }
    val afternoonTasks = todayTasks.filter { it.timeOfDay.equals("Afternoon", ignoreCase = true) && !it.isCompleted }
    val eveningTasks = todayTasks.filter { it.timeOfDay.equals("Evening", ignoreCase = true) && !it.isCompleted }
    val otherTasks = todayTasks.filter {
        !it.isCompleted &&
        !it.timeOfDay.equals("Morning", ignoreCase = true) &&
        !it.timeOfDay.equals("Afternoon", ignoreCase = true) &&
        !it.timeOfDay.equals("Evening", ignoreCase = true)
    }
    val completedTasks = todayTasks.filter { it.isCompleted }

    val totalCount = todayTasks.size
    val completedCount = completedTasks.size
    val progressPct = if (totalCount > 0) ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryIndigo
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onNavigateToProfile() }
                        .testTag("profile_avatar_today"),
                    contentAlignment = Alignment.Center
                ) {
                    if (!userAvatarUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = userAvatarUri,
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.img_profile_avatar_1784868721178),
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Daily Progress Bar Section
        item {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "Daily Progress",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$progressPct%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryIndigo
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { progressPct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = PrimaryIndigo,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }

        // 365 Daily Thought Card
        item {
            DailyThoughtCard(compactMode = true)
        }

        // Morning Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFFBEB))
                    .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = "Morning",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Morning Focus (${morningTasks.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        items(morningTasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onToggle = { viewModel.toggleTask(task) },
                onToggleStarred = { viewModel.toggleTaskStarred(task) },
                onLongClick = { taskForOptions = task },
                modifier = Modifier.animateItem()
            )
        }

        // Afternoon Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFF7ED))
                    .border(1.dp, Color(0xFFFDBA74), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LightMode,
                        contentDescription = "Afternoon",
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Afternoon Flow (${afternoonTasks.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9A3412)
                    )
                }
            }
        }

        items(afternoonTasks, key = { it.id }) { task ->
            if (task.isFocusTask) {
                FocusTaskCard(
                    task = task,
                    onStartFocus = onStartFocus,
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            } else {
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onToggleStarred = { viewModel.toggleTaskStarred(task) },
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Evening Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F3FF))
                    .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NightsStay,
                        contentDescription = "Evening",
                        tint = Color(0xFF7C3AED),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Evening Wind-Down (${eveningTasks.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF5B21B6)
                    )
                }
            }
        }

        items(eveningTasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onToggle = { viewModel.toggleTask(task) },
                onToggleStarred = { viewModel.toggleTaskStarred(task) },
                onLongClick = { taskForOptions = task },
                modifier = Modifier.animateItem()
            )
        }

        if (otherTasks.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0FDF4))
                        .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = "General",
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Anytime / General Tasks (${otherTasks.size})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
            }

            items(otherTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onToggleStarred = { viewModel.toggleTaskStarred(task) },
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // Completed Section (Collapsible)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clickable { completedExpanded = !completedExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completed (${completedTasks.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (completedExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (completedExpanded) {
            items(completedTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onToggleStarred = { viewModel.toggleTaskStarred(task) },
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Task Options Dialog (on long-press)
    taskForOptions?.let { task ->
        TaskOptionsDialog(
            task = task,
            onDismiss = { taskForOptions = null },
            onEdit = {
                val t = taskForOptions
                taskForOptions = null
                taskToEdit = t
            },
            onDelete = {
                val t = taskForOptions
                taskForOptions = null
                taskToDelete = t
            }
        )
    }

    // Delete Confirmation Dialog
    taskToDelete?.let { task ->
        DeleteTaskConfirmDialog(
            task = task,
            onConfirm = {
                val t = taskToDelete
                taskToDelete = null
                if (t != null) viewModel.deleteTask(t)
            },
            onDismiss = { taskToDelete = null }
        )
    }

    // Edit Task Modal Dialog
    taskToEdit?.let { task ->
        Dialog(
            onDismissRequest = { taskToEdit = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            NewTaskScreen(
                viewModel = viewModel,
                taskToEdit = task,
                onClose = { taskToEdit = null }
            )
        }
    }
}
