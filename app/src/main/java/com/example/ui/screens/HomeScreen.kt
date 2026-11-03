package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import coil.compose.AsyncImage
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.Secondary
import com.example.ui.theme.TertiaryOrange
import com.example.util.DailyThoughtProvider
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

import com.example.data.model.isScheduledForDate

@Composable
fun HomeScreen(
    viewModel: TaskViewModel,
    onNavigateToToday: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onStartFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatarIndex by viewModel.userAvatarIndex.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedTimeFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val showFocusMode by viewModel.showFocusMode.collectAsStateWithLifecycle()

    var taskForOptions by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    var todayDateStr by remember {
        mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()))
    }
    var currentHour by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
    }

    LaunchedEffect(Unit) {
        while (true) {
            val nowStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            if (nowStr != todayDateStr) {
                todayDateStr = nowStr
            }
            val hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (hourNow != currentHour) {
                currentHour = hourNow
            }
            delay(10000)
        }
    }

    val todayTasks = remember(tasks, todayDateStr) {
        tasks.filter { it.isScheduledForDate(todayDateStr) }
    }

    val totalCount = todayTasks.size
    val completedCount = todayTasks.count { it.isCompleted }
    val openCount = todayTasks.count { !it.isCompleted }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val progressPercent = (progressFraction * 100).toInt()

    val focusTask = todayTasks.firstOrNull { it.isFocusTask } ?: todayTasks.firstOrNull { !it.isCompleted } ?: tasks.firstOrNull { !it.isCompleted }

    // Time-based dynamic formal greeting according to the current hour
    val greetingSalutation = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }

    // Filter tasks based on search, scheduled date, and category filter
    val filteredTasks = tasks.filter { task ->
        val matchesDate = task.isScheduledForDate(todayDateStr)

        val matchesSearch = searchQuery.isBlank() ||
                task.title.contains(searchQuery, ignoreCase = true) ||
                task.project.contains(searchQuery, ignoreCase = true) ||
                task.description.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when (categoryFilter) {
            "All" -> true
            "Active" -> !task.isCompleted
            "Starred" -> task.isStarred
            "High Priority" -> task.priority.equals("High", ignoreCase = true)
            "Core Work" -> task.project.equals("Core Work", ignoreCase = true)
            "Completed" -> task.isCompleted
            else -> !task.isCompleted
        }

        val matchesTime = when (selectedFilter) {
            "15m" -> task.estimatedDurationMinutes in 1..15
            "30m" -> task.estimatedDurationMinutes in 16..30
            "1h" -> task.estimatedDurationMinutes in 31..60
            "2h+" -> task.estimatedDurationMinutes > 60
            else -> true
        }

        matchesDate && matchesSearch && matchesCategory && matchesTime
    }

    val upcomingTasks = filteredTasks.take(6)

    val heroGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF7C3AED), // Violet
            Color(0xFF06B6D4)  // Cyan
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header with Avatar & Greeting
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val displayName = userName.ifBlank { "User" }
                    Text(
                        text = "$greetingSalutation, $displayName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.3).sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Profile Avatar
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, PrimaryIndigo.copy(alpha = 0.5f), CircleShape)
                            .clickable { onNavigateToProfile() }
                            .testTag("profile_avatar_button"),
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
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search tasks, projects, tags...", fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_search_field"),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = PrimaryIndigo,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                singleLine = true
            )
        }

        // Hero Progress Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), spotColor = PrimaryIndigo.copy(alpha = 0.25f))
                    .clip(RoundedCornerShape(22.dp))
                    .background(heroGradient)
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DAILY PERFORMANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$progressPercent% Completed",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Badge Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (progressPercent >= 80) "🔥 High Output" else "⚡ On Track",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mini Stats Strip Inside Hero
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AssignmentTurnedIn,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$completedCount/$totalCount Tasks",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$openCount Remaining",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Smart Category Filter Chips with Dynamic Counts
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val categoryCounts = listOf(
                    "All" to tasks.size,
                    "Active" to tasks.count { !it.isCompleted },
                    "Starred" to tasks.count { it.isStarred },
                    "High Priority" to tasks.count { it.priority.equals("High", ignoreCase = true) && !it.isCompleted },
                    "Core Work" to tasks.count { it.project.equals("Core Work", ignoreCase = true) && !it.isCompleted },
                    "Completed" to tasks.count { it.isCompleted }
                )

                items(categoryCounts) { (category, count) ->
                    val isSelected = categoryFilter == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerLow)
                            .border(
                                1.dp,
                                if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { viewModel.setCategoryFilter(category) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (category == "Starred") {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else TertiaryOrange,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "$category ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Focus Task Card (Conditionally shown based on showFocusMode preference)
        if (showFocusMode) {
            item {
                FocusTaskCard(
                    task = focusTask,
                    onStartFocus = onStartFocus,
                    onLongClick = {
                        if (focusTask != null) taskForOptions = focusTask
                    }
                )
            }
        }

        // Quick Time Filter ("Have Some Time?")
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Filter by Time Window",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (selectedFilter != null) {
                        TextButton(onClick = { viewModel.setTimeFilter("") }) {
                            Text("Reset", fontSize = 12.sp, color = PrimaryIndigo)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durations = listOf("15m", "30m", "1h", "2h+")
                    durations.forEach { duration ->
                        val isSelected = selectedFilter == duration
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(
                                    1.dp,
                                    if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isSelected) viewModel.setTimeFilter("")
                                    else viewModel.setTimeFilter(duration)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = duration,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Upcoming / Filtered Tasks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (categoryFilter == "All") "Upcoming Tasks" else "$categoryFilter Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onNavigateToToday) {
                    Text(
                        text = "See all",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo
                    )
                }
            }
        }

        if (upcomingTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = PrimaryIndigo,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || categoryFilter != "All") "No tasks match your filter" else "No active tasks for today",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the + button below to add a new task!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(upcomingTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onToggleStarred = { viewModel.toggleTaskStarred(task) },
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            }
        }

        // 365 Daily Thought Card
        item {
            DailyThoughtCard()
            Spacer(modifier = Modifier.height(80.dp)) // Extra space for bottom bar
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


