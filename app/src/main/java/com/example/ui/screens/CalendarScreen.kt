package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.DeleteTaskConfirmDialog
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskOptionsDialog
import com.example.ui.screens.NewTaskScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.TaskEntity
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun CalendarScreen(
    viewModel: TaskViewModel,
    onNavigateToProfile: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatarUri by viewModel.userAvatarUri.collectAsStateWithLifecycle()

    var taskForOptions by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToEdit by remember { mutableStateOf<TaskEntity?>(null) }
    var taskToDelete by remember { mutableStateOf<TaskEntity?>(null) }

    val calendarInstance = remember { java.util.Calendar.getInstance() }
    val currentHour = remember { calendarInstance.get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingSalutation = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Welcome back"
    }
    val currentYear = calendarInstance.get(java.util.Calendar.YEAR)
    val currentMonth = calendarInstance.get(java.util.Calendar.MONTH)
    val todayDayOfMonth = calendarInstance.get(java.util.Calendar.DAY_OF_MONTH)

    var displayYear by remember { mutableIntStateOf(currentYear) }
    var displayMonth by remember { mutableIntStateOf(currentMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDayOfMonth) }

    val monthTitle = remember(displayYear, displayMonth) {
        val tempCal = java.util.Calendar.getInstance().apply {
            set(displayYear, displayMonth, 1)
        }
        java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(tempCal.time)
    }

    val formattedSelectedDateStr = remember(selectedDay, displayMonth, displayYear) {
        val tempCal = java.util.Calendar.getInstance().apply {
            set(displayYear, displayMonth, selectedDay)
        }
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(tempCal.time)
    }

    LaunchedEffect(formattedSelectedDateStr) {
        viewModel.setSelectedDate(formattedSelectedDateStr)
    }

    val formattedDateHeader = remember(selectedDay, displayMonth, displayYear) {
        val tempCal = java.util.Calendar.getInstance().apply {
            set(displayYear, displayMonth, selectedDay)
        }
        java.text.SimpleDateFormat("EEEE, MMM d", java.util.Locale.getDefault()).format(tempCal.time)
    }

    val maxDaysInMonth = remember(displayYear, displayMonth) {
        val tempCal = java.util.Calendar.getInstance().apply {
            set(displayYear, displayMonth, 1)
        }
        tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(displayYear, displayMonth) {
        val firstCal = java.util.Calendar.getInstance().apply {
            set(displayYear, displayMonth, 1)
        }
        firstCal.get(java.util.Calendar.DAY_OF_WEEK) - 1 // 0-indexed Sunday
    }

    val daysWithTasks = remember(tasks, displayMonth, displayYear) {
        val monthPrefix = String.format(java.util.Locale.getDefault(), "%04d-%02d-", displayYear, displayMonth + 1)
        tasks.mapNotNull { task ->
            if (task.date.startsWith(monthPrefix)) {
                task.date.substringAfterLast("-").toIntOrNull()
            } else null
        }.toSet()
    }

    val agendaTasks = tasks.filter {
        it.date == formattedSelectedDateStr || (displayYear == currentYear && displayMonth == currentMonth && selectedDay == todayDayOfMonth && (it.date == formattedSelectedDateStr || it.date.isBlank()))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$greetingSalutation, ${userName.ifBlank { "User" }}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onNavigateToProfile() }
                        .testTag("profile_avatar_calendar"),
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

        // Calendar Section Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    // Month Selection Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = monthTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(onClick = {
                                if (displayMonth > 0) {
                                    displayMonth--
                                } else {
                                    displayMonth = 11
                                    displayYear--
                                }
                                selectedDay = 1
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronLeft,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = {
                                if (displayMonth < 11) {
                                    displayMonth++
                                } else {
                                    displayMonth = 0
                                    displayYear++
                                }
                                selectedDay = 1
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Month Selector Chips (Jan - Dec)
                    val monthsList = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(monthsList) { index, mName ->
                            val isSelectedMonth = (index == displayMonth)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelectedMonth) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .clickable {
                                        displayMonth = index
                                        selectedDay = 1
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelectedMonth) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelectedMonth) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Weekday Labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach { dayLabel ->
                            Text(
                                text = dayLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Calendar Grid
                    val gridCells = mutableListOf<Int>()
                    for (i in 0 until firstDayOfWeek) {
                        gridCells.add(0) // empty leading slot
                    }
                    for (day in 1..maxDaysInMonth) {
                        gridCells.add(day)
                    }

                    Column {
                        gridCells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                week.forEach { dayVal ->
                                    val isCurrentMonthDay = dayVal > 0
                                    val isSelected = dayVal == selectedDay
                                    val hasDot = daysWithTasks.contains(dayVal)

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) PrimaryIndigo
                                                else Color.Transparent
                                            )
                                            .clickable(enabled = isCurrentMonthDay) {
                                                if (isCurrentMonthDay) selectedDay = dayVal
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCurrentMonthDay) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$dayVal",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (hasDot) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White else PrimaryIndigo)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Agenda Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Agenda",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedDateHeader,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                TextButton(
                    onClick = {
                        viewModel.setSelectedDate(formattedSelectedDateStr)
                        onQuickAdd()
                    },
                    modifier = Modifier.testTag("quick_add_calendar_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Quick Add",
                        tint = PrimaryIndigo,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Quick Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryIndigo
                    )
                }
            }
        }

        // Tasks for selected agenda day
        if (agendaTasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tasks scheduled for $formattedDateHeader",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(agendaTasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onToggleStarred = { viewModel.toggleTaskStarred(task) },
                    onLongClick = { taskForOptions = task },
                    modifier = Modifier.animateItem()
                )
            }
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
