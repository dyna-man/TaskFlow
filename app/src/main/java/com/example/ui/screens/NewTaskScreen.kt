package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TaskEntity
import com.example.ui.components.DeleteTaskConfirmDialog
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMediumYellow
import com.example.ui.viewmodel.TaskViewModel
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewTaskScreen(
    viewModel: TaskViewModel,
    taskToEdit: TaskEntity? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    val sdf = remember { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()) }
    val todayStr = remember { sdf.format(Calendar.getInstance().time) }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        sdf.format(cal.time)
    }

    val initialTargetDate = remember(selectedDate, taskToEdit) {
        taskToEdit?.date?.ifBlank { null } ?: if (selectedDate.isNotBlank()) selectedDate else todayStr
    }

    var taskTitle by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var taskDescription by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Enhanced Schedule states
    var scheduleMode by remember(taskToEdit) { mutableStateOf(taskToEdit?.scheduleType ?: "One day") } // "One day", "2 to 5 days", "Multiple days", "Forever"
    var oneDayDate by remember(initialTargetDate, taskToEdit) { mutableStateOf(taskToEdit?.date ?: initialTargetDate) }

    fun getCalculatedDate(baseDate: String, offsetDays: Int): String {
        val cal = Calendar.getInstance()
        return try {
            val d = sdf.parse(baseDate)
            if (d != null) cal.time = d
            cal.add(Calendar.DAY_OF_YEAR, offsetDays)
            sdf.format(cal.time)
        } catch (e: Exception) {
            todayStr
        }
    }

    var selectedDayCount by remember { mutableIntStateOf(2) } // 2, 3, 4, or 5 days
    var rangeDate1 by remember(initialTargetDate) { mutableStateOf(initialTargetDate) }
    var rangeDate2 by remember(initialTargetDate) { mutableStateOf(getCalculatedDate(initialTargetDate, 1)) }
    var rangeDate3 by remember(initialTargetDate) { mutableStateOf(getCalculatedDate(initialTargetDate, 2)) }
    var rangeDate4 by remember(initialTargetDate) { mutableStateOf(getCalculatedDate(initialTargetDate, 3)) }
    var rangeDate5 by remember(initialTargetDate) { mutableStateOf(getCalculatedDate(initialTargetDate, 4)) }

    var multipleDays by remember(taskToEdit) {
        if (taskToEdit != null && taskToEdit.scheduleType.equals("Multiple days", ignoreCase = true)) {
            val days = taskToEdit.selectedDates.split(",", " ", ";", "|").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            if (days.isNotEmpty()) mutableStateOf(days) else mutableStateOf(setOf("Mon", "Wed", "Fri"))
        } else {
            mutableStateOf(setOf("Mon", "Wed", "Fri"))
        }
    }

    var selectedPriority by remember(taskToEdit) { mutableStateOf(taskToEdit?.priority ?: "Medium") }
    var selectedProject by remember(taskToEdit) { mutableStateOf(taskToEdit?.project ?: "Core Work") }
    var selectedTimeOfDay by remember(taskToEdit) { mutableStateOf(taskToEdit?.timeOfDay ?: "Morning") }
    var attachmentUri by remember(taskToEdit) { mutableStateOf(taskToEdit?.attachmentUri) }

    // Notification on Calendar Day States
    var enableNotification by remember(taskToEdit) { mutableStateOf(taskToEdit?.notificationEnabled ?: true) }
    var notificationTime by remember(taskToEdit) { mutableStateOf(taskToEdit?.notificationTime ?: "9:00 AM") }

    val showTimePicker = { onTimeSelected: (String) -> Unit ->
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val hour12 = if (selectedHour == 0) 12 else if (selectedHour > 12) selectedHour - 12 else selectedHour
                val formattedTime = String.format("%d:%02d %s", hour12, selectedMinute, amPm)
                onTimeSelected(formattedTime)
            },
            hour, minute, false
        ).show()
    }

    val showDatePicker = { initialDate: String, onDateSelected: (String) -> Unit ->
        val parts = initialDate.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
        val m = (parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)) - 1
        val d = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onDateSelected(formatted)
            },
            y, m, d
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachmentUri = uri?.toString()
    }

    if (showDeleteConfirmDialog && taskToEdit != null) {
        DeleteTaskConfirmDialog(
            task = taskToEdit,
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteTask(taskToEdit)
                onClose()
            },
            onDismiss = {
                showDeleteConfirmDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (taskToEdit != null) "Edit Task" else "New Task",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (taskToEdit != null) {
                        IconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            modifier = Modifier.testTag("delete_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Task",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_new_task_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Input Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "What do you want to do? *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${taskTitle.length}/100",
                            fontSize = 12.sp,
                            color = if (taskTitle.length > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = {
                            if (it.length <= 100) {
                                taskTitle = it
                                titleError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "E.g., Review quarterly reports...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        isError = titleError != null || taskTitle.length > 100,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .testTag("task_title_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    titleError?.let { err ->
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description Input (Optional)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description (Optional)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${taskDescription.length}/1000",
                            fontSize = 12.sp,
                            color = if (taskDescription.length > 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = taskDescription,
                        onValueChange = {
                            if (it.length <= 1000) {
                                taskDescription = it
                                descriptionError = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Add subtasks, links, notes, or detailed context...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        isError = descriptionError != null || taskDescription.length > 1000,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("task_description_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryIndigo,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    )

                    descriptionError?.let { err ->
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Quick Attribute Selector Row
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Schedule Chip
                AttributeChip(
                    icon = Icons.Filled.CalendarToday,
                    label = "Schedule: $scheduleMode",
                    onClick = { }
                )
                // Priority Chip
                AttributeChip(
                    icon = Icons.Filled.Flag,
                    label = "Priority: $selectedPriority",
                    onClick = { }
                )
                // Project Chip
                AttributeChip(
                    icon = Icons.Filled.Folder,
                    label = "Project: $selectedProject",
                    onClick = { }
                )
                // Attachment Chip
                AttributeChip(
                    icon = Icons.Filled.UploadFile,
                    label = if (attachmentUri != null) "File Attached" else "Upload",
                    onClick = { filePickerLauncher.launch("*/*") }
                )
            }
        }

        // Bento Section: Schedule Selection (One Day, Two Days, Multiple Days, Forever)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Schedule Type",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 4 Main Mode Selector Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("One day", "2 to 5 days", "Multiple days", "Forever").forEach { mode ->
                            val isSelected = scheduleMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .clickable { 
                                        scheduleMode = mode
                                        dateError = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sub-UI based on chosen mode
                    when (scheduleMode) {
                        "One day" -> {
                            Text(
                                text = "Select Date:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val dateChips = remember(initialTargetDate, todayStr, tomorrowStr) {
                                    val list = mutableListOf<Pair<String, String>>()
                                    if (initialTargetDate != todayStr && initialTargetDate != tomorrowStr) {
                                        list.add(initialTargetDate to "Selected ($initialTargetDate)")
                                    }
                                    list.add(todayStr to "Today ($todayStr)")
                                    list.add(tomorrowStr to "Tomorrow ($tomorrowStr)")
                                    list
                                }

                                dateChips.forEach { (dVal, label) ->
                                    val isSelected = oneDayDate == dVal
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLowest)
                                            .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                            .clickable { oneDayDate = dVal }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                // Calendar Pick Button
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                        .border(1.dp, PrimaryIndigo, RoundedCornerShape(20.dp))
                                        .clickable { showDatePicker(oneDayDate) { oneDayDate = it } }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = "Pick Date",
                                        tint = PrimaryIndigo,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Custom: $oneDayDate",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo
                                    )
                                }
                            }
                        }

                        "2 to 5 days", "Two days" -> {
                            Column {
                                Text(
                                    text = "Select Number of Days (2 to 5):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf(2, 3, 4, 5).forEach { count ->
                                        val isSelected = selectedDayCount == count
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerLowest)
                                                .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                                .clickable { selectedDayCount = count }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count Days",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Select Dates for $selectedDayCount Days:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val rangeDates = listOf(
                                        Triple("Date 1", rangeDate1) { d: String -> rangeDate1 = d },
                                        Triple("Date 2", rangeDate2) { d: String -> rangeDate2 = d },
                                        Triple("Date 3", rangeDate3) { d: String -> rangeDate3 = d },
                                        Triple("Date 4", rangeDate4) { d: String -> rangeDate4 = d },
                                        Triple("Date 5", rangeDate5) { d: String -> rangeDate5 = d }
                                    ).take(selectedDayCount)

                                    rangeDates.forEach { (label, dateVal, onSelect) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "$label:", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                                    .clickable { showDatePicker(dateVal, onSelect) }
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Icon(Icons.Filled.CalendarToday, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = dateVal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        "Multiple days" -> {
                            Text(
                                text = "Select Recurring Days:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Day of week chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                                    val isSelected = multipleDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerLowest)
                                            .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                            .clickable {
                                                multipleDays = if (isSelected) {
                                                    multipleDays - day
                                                } else {
                                                    multipleDays + day
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Preset: ",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Weekdays",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.clickable {
                                        multipleDays = setOf("Mon", "Tue", "Wed", "Thu", "Fri")
                                    }
                                )
                                Text(text = "•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "Weekends",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo,
                                    modifier = Modifier.clickable {
                                        multipleDays = setOf("Sat", "Sun")
                                    }
                                )
                            }
                        }

                        "Forever" -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryIndigo.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Repeat,
                                    contentDescription = "Forever 365 Days",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "All 365 Days (Everyday)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryIndigo
                                    )
                                    Text(
                                        text = "This task will repeat every single day for the entire year.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    dateError?.let { err ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = err,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("date_error_text")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Time of Day",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Morning", "Afternoon", "Evening").forEach { tod ->
                            val isSelected = selectedTimeOfDay == tod
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                    .clickable { selectedTimeOfDay = tod }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = tod,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Calendar Day Notification Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = PrimaryIndigo,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Send Calendar Day Notification",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (enableNotification) "Alert enabled on due date" else "Disabled for this task",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = enableNotification,
                            onCheckedChange = { enableNotification = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryIndigo
                            )
                        )
                    }

                    if (enableNotification) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Notification Alert Time",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("8:00 AM", "9:00 AM", "12:00 PM", "3:00 PM", "6:00 PM", "8:00 PM").forEach { timePreset ->
                                val isSelected = notificationTime == timePreset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryIndigo.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainerLowest)
                                        .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                        .clickable { notificationTime = timePreset }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = timePreset,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Custom time picker button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryIndigo)
                                    .clickable {
                                        showTimePicker { customTime ->
                                            notificationTime = customTime
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Custom Time ($notificationTime)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Priority Selection Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Priority",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Low", PriorityLowGreen, "Low"),
                            Triple("Medium", PriorityMediumYellow, "Medium"),
                            Triple("High", PriorityHighRed, "High")
                        ).forEach { (label, flagColor, valName) ->
                            val isSelected = selectedPriority == valName
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainer)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .clickable { selectedPriority = valName }
                                    .padding(vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Flag,
                                    contentDescription = label,
                                    tint = if (isSelected) Color.White else flagColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section / Project Selection
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Section / Project",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Core Work", "Marketing", "Personal", "Study", "Health & Wellness", "Acme Corp").forEach { project ->
                            val isSelected = selectedProject == project
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.surfaceContainerLowest)
                                    .border(1.dp, if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
                                    .clickable { selectedProject = project }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = project,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visual Context Card Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_focus_banner_1784868741046),
                    contentDescription = "Desk setup",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(16.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = "Deep focus mode is recommended for this task.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Create Task Button
        item {
            Button(
                onClick = {
                    titleError = null
                    descriptionError = null
                    dateError = null

                    if (taskTitle.trim().isEmpty()) {
                        titleError = "Task title cannot be empty or whitespace only."
                        return@Button
                    }
                    if (taskTitle.trim().length > 100) {
                        titleError = "Title length must be between 1 and 100 characters."
                        return@Button
                    }
                    if (taskDescription.trim().length > 1000) {
                        descriptionError = "Description length cannot exceed 1000 characters."
                        return@Button
                    }

                    val (datesSummary, primaryDate) = when (scheduleMode) {
                        "One day" -> Pair(oneDayDate, oneDayDate)
                        "2 to 5 days", "Two days" -> {
                            val dates = listOf(rangeDate1, rangeDate2, rangeDate3, rangeDate4, rangeDate5).take(selectedDayCount)
                            Pair("${dates.first()} to ${dates.last()} ($selectedDayCount days)", dates.first())
                        }
                        "Multiple days" -> {
                            val daysStr = if (multipleDays.isEmpty()) "Mon, Wed, Fri" else multipleDays.joinToString(", ")
                            Pair(daysStr, todayStr)
                        }
                        "Forever" -> Pair("All 365 Days", todayStr)
                        else -> Pair(oneDayDate, oneDayDate)
                    }

                    if (primaryDate < todayStr) {
                        dateError = "Scheduled date cannot be in the past."
                        return@Button
                    }

                    if (taskToEdit != null) {
                        viewModel.updateTask(
                            id = taskToEdit.id,
                            title = taskTitle,
                            description = taskDescription,
                            timeOfDay = selectedTimeOfDay,
                            scheduledTime = if (scheduleMode == "One day") notificationTime else "All Day",
                            date = primaryDate,
                            scheduleType = scheduleMode,
                            selectedDates = datesSummary,
                            priority = selectedPriority,
                            project = selectedProject,
                            isFocusTask = taskToEdit.isFocusTask,
                            isCompleted = taskToEdit.isCompleted,
                            isStarred = taskToEdit.isStarred,
                            attachmentUri = attachmentUri,
                            notificationEnabled = enableNotification,
                            notificationTime = notificationTime,
                            onSuccess = {
                                onClose()
                            },
                            onError = { err ->
                                if (err.contains("title", ignoreCase = true) || err.contains("Duplicate", ignoreCase = true)) {
                                    titleError = err
                                } else if (err.contains("description", ignoreCase = true)) {
                                    descriptionError = err
                                } else if (err.contains("date", ignoreCase = true) || err.contains("past", ignoreCase = true)) {
                                    dateError = err
                                }
                            }
                        )
                    } else {
                        viewModel.createTask(
                            title = taskTitle,
                            description = taskDescription,
                            timeOfDay = selectedTimeOfDay,
                            scheduledTime = if (scheduleMode == "One day") notificationTime else "All Day",
                            date = primaryDate,
                            scheduleType = scheduleMode,
                            selectedDates = datesSummary,
                            priority = selectedPriority,
                            project = selectedProject,
                            attachmentUri = attachmentUri,
                            notificationEnabled = enableNotification,
                            notificationTime = notificationTime,
                            onSuccess = {
                                onClose()
                            },
                            onError = { err ->
                                if (err.contains("title", ignoreCase = true) || err.contains("Duplicate", ignoreCase = true)) {
                                    titleError = err
                                } else if (err.contains("description", ignoreCase = true)) {
                                    descriptionError = err
                                } else if (err.contains("date", ignoreCase = true) || err.contains("past", ignoreCase = true)) {
                                    dateError = err
                                }
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("create_task_button")
            ) {
                Text(
                    text = if (taskToEdit != null) "Save Changes" else "Create Task",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AttributeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

