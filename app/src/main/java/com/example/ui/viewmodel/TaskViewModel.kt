package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.TaskFlowDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import com.example.data.repository.TaskRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.ui.theme.AppThemeMode
import com.example.ui.components.NotificationEvent
import com.example.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository = TaskRepository(TaskFlowDatabase.getDatabase(application).taskDao())
    private val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(application)

    private val _notificationEvent = MutableStateFlow<NotificationEvent?>(null)
    val notificationEvent: StateFlow<NotificationEvent?> = _notificationEvent.asStateFlow()

    private var notificationJob: Job? = null

    fun showNotification(title: String, message: String, isSuccess: Boolean = true) {
        try {
            NotificationHelper.showNotification(getApplication(), title, message)
        } catch (e: Exception) {
            android.util.Log.e("TaskViewModel", "Failed to dispatch system notification: ${e.message}", e)
        }

        notificationJob?.cancel()
        _notificationEvent.value = NotificationEvent(title = title, message = message, isSuccess = isSuccess)
        notificationJob = viewModelScope.launch {
            delay(3500)
            _notificationEvent.value = null
        }
    }

    fun dismissNotification() {
        _notificationEvent.value = null
    }

    // All Tasks
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Focus Sessions History
    val focusSessions: StateFlow<List<FocusSessionEntity>> = repository.focusSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User preferences
    val themeMode: StateFlow<AppThemeMode> = preferencesRepository.themeMode
    val focusDurationMinutes: StateFlow<Int> = preferencesRepository.focusDurationMinutes
    val streakDays: StateFlow<Int> = preferencesRepository.streakDays
    val userName: StateFlow<String> = preferencesRepository.userName
    val userAvatarIndex: StateFlow<Int> = preferencesRepository.userAvatarIndex
    val userAvatarUri: StateFlow<String?> = preferencesRepository.userAvatarUri
    val defaultReminders: StateFlow<String> = preferencesRepository.defaultReminders
    val showFocusMode: StateFlow<Boolean> = preferencesRepository.showFocusMode
    val quietHoursEnabled: StateFlow<Boolean> = preferencesRepository.quietHoursEnabled

    init {
        // Check & Reset recurring/daily tasks at 12 AM / new day
        checkAndResetMidnightTasks()
        viewModelScope.launch {
            while (true) {
                delay(30000) // Check every 30s for midnight date rollover
                checkAndResetMidnightTasks()
            }
        }

        // Calculate real dynamic streak from tasks and update preferences
        viewModelScope.launch {
            allTasks.collect { tasks ->
                val streak = calculateStreakFromTasks(tasks)
                preferencesRepository.setStreakDays(streak)
            }
        }
    }

    private fun checkAndResetMidnightTasks() {
        viewModelScope.launch {
            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            if (preferencesRepository.lastResetDate.value != todayStr) {
                repository.resetTasksForNewDay()
                preferencesRepository.setLastResetDate(todayStr)
            }
        }
    }

    private fun calculateStreakFromTasks(tasks: List<TaskEntity>): Int {
        val completedTasks = tasks.filter { it.isCompleted }
        if (completedTasks.isEmpty()) return 0

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val completedDates = completedTasks.mapNotNull { task ->
            if (task.completedTimestamp != null && task.completedTimestamp > 0) {
                sdf.format(java.util.Date(task.completedTimestamp))
            } else if (task.date.isNotBlank()) {
                task.date
            } else null
        }.toSet()

        if (completedDates.isEmpty()) return 0

        val cal = java.util.Calendar.getInstance()
        var streak = 0
        var checkDate = sdf.format(cal.time)

        if (completedDates.contains(checkDate)) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = sdf.format(cal.time)
        } else {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = sdf.format(cal.time)
            if (!completedDates.contains(checkDate)) {
                return 0
            }
        }

        while (completedDates.contains(checkDate)) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            checkDate = sdf.format(cal.time)
        }

        return streak
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllData()
            showNotification("Data Cleared", "All local tasks and history reset successfully.", isSuccess = false)
        }
    }

    fun updateUserName(newName: String) {
        if (newName.isNotBlank()) {
            preferencesRepository.setUserName(newName.trim())
        }
    }

    fun updateUserAvatar(index: Int, uri: String? = null) {
        preferencesRepository.setUserAvatar(index, uri)
    }

    fun updateDefaultReminders(reminder: String) {
        preferencesRepository.setDefaultReminders(reminder)
    }

    fun toggleShowFocusMode() {
        preferencesRepository.setShowFocusMode(!showFocusMode.value)
    }

    // Selection & Search Filters
    private val todayDateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    private val _selectedDate = MutableStateFlow(todayDateStr)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _selectedTimeFilter = MutableStateFlow<String?>(null)
    val selectedTimeFilter: StateFlow<String?> = _selectedTimeFilter.asStateFlow()

    private val _selectedProjectFilter = MutableStateFlow<String?>(null)
    val selectedProjectFilter: StateFlow<String?> = _selectedProjectFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _categoryFilter = MutableStateFlow("All") // "All", "Starred", "High Priority", "Core Work", "Completed"
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun toggleTaskStarred(task: TaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskStarred(task)
        }
    }

    // Focus Timer State
    private val _activeFocusTask = MutableStateFlow<TaskEntity?>(null)
    val activeFocusTask: StateFlow<TaskEntity?> = _activeFocusTask.asStateFlow()

    private val _isFocusRunning = MutableStateFlow(false)
    val isFocusRunning: StateFlow<Boolean> = _isFocusRunning.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(1500)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val willBeCompleted = !task.isCompleted
            repository.toggleTaskCompletion(task)
            if (willBeCompleted) {
                showNotification("Task Completed! 🎉", "\"${task.title}\" is marked as done.")
            } else {
                showNotification("Task Restored", "\"${task.title}\" moved back to pending.")
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        timeOfDay: String,
        scheduledTime: String,
        date: String,
        scheduleType: String = "One day",
        selectedDates: String = "2024-09-09",
        priority: String,
        project: String,
        isFocusTask: Boolean = false,
        attachmentUri: String? = null,
        notificationEnabled: Boolean = true,
        notificationTime: String = "9:00 AM",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanTitle = title.trim().replace("\\s+".toRegex(), " ")
        val cleanDesc = description.trim()
        val cleanProject = project.trim().replace("\\s+".toRegex(), " ").ifBlank { "Core Work" }
        val cleanScheduledTime = scheduledTime.trim().ifBlank { "9:00 AM" }
        val cleanNotificationTime = notificationTime.trim().ifBlank { "9:00 AM" }

        val controlCharRegex = Regex("[\\p{Cntrl}]")

        if (cleanTitle.isEmpty()) {
            val errMsg = "Task title cannot be empty or whitespace only."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (controlCharRegex.containsMatchIn(cleanTitle)) {
            val errMsg = "Task title cannot contain control characters or newlines."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanTitle.length > 100) {
            val errMsg = "Title length must be between 1 and 100 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (controlCharRegex.containsMatchIn(cleanDesc)) {
            val errMsg = "Task description cannot contain control characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanDesc.length > 1000) {
            val errMsg = "Description length cannot exceed 1000 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanProject.length > 50) {
            val errMsg = "Project name cannot exceed 50 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        val isDuplicate = allTasks.value.any { it.title.trim().equals(cleanTitle, ignoreCase = true) }
        if (isDuplicate) {
            val errMsg = "A task with title \"$cleanTitle\" already exists."
            showNotification("Duplicate Task", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        viewModelScope.launch {
            if (repository.taskExists(cleanTitle)) {
                val errMsg = "A task with title \"$cleanTitle\" already exists."
                showNotification("Duplicate Task", errMsg, isSuccess = false)
                onError(errMsg)
                return@launch
            }

            val task = TaskEntity(
                title = cleanTitle,
                description = cleanDesc,
                timeOfDay = timeOfDay,
                scheduledTime = cleanScheduledTime,
                date = date,
                scheduleType = scheduleType,
                selectedDates = selectedDates.ifBlank { date },
                priority = priority,
                project = cleanProject,
                isFocusTask = isFocusTask,
                attachmentUri = attachmentUri,
                notificationEnabled = notificationEnabled,
                notificationTime = cleanNotificationTime
            )
            val newId = repository.insertTask(task)

            if (notificationEnabled) {
                NotificationHelper.scheduleReminder(
                    getApplication(),
                    newId,
                    cleanTitle,
                    date,
                    cleanNotificationTime
                )
                showNotification(
                    "Reminder Set 🔔",
                    "Alert scheduled for $date at $cleanNotificationTime for \"$cleanTitle\"."
                )
            } else {
                showNotification(
                    "Task Created ✅",
                    "\"$cleanTitle\" saved (notifications disabled for this task)."
                )
            }
            onSuccess()
        }
    }

    fun updateTask(
        id: Long,
        title: String,
        description: String,
        timeOfDay: String,
        scheduledTime: String,
        date: String,
        scheduleType: String,
        selectedDates: String,
        priority: String,
        project: String,
        isFocusTask: Boolean = false,
        isCompleted: Boolean = false,
        isStarred: Boolean = false,
        attachmentUri: String? = null,
        notificationEnabled: Boolean = true,
        notificationTime: String = "9:00 AM",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val cleanTitle = title.trim()
        val cleanDesc = description.trim()
        val cleanProject = if (project.trim().isEmpty()) "Core Work" else project.trim()
        val cleanScheduledTime = if (scheduledTime.trim().isEmpty()) "9:00 AM" else scheduledTime.trim()
        val cleanNotificationTime = if (notificationTime.trim().isEmpty()) "9:00 AM" else notificationTime.trim()

        if (cleanTitle.isEmpty()) {
            val errMsg = "Task title cannot be empty or whitespace only."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        val controlCharRegex = Regex("[\\p{Cntrl}&&[^\r\n\t]]")
        if (controlCharRegex.containsMatchIn(cleanTitle)) {
            val errMsg = "Task title cannot contain control characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanTitle.length > 100) {
            val errMsg = "Title length must be between 1 and 100 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanDesc.length > 1000) {
            val errMsg = "Description length cannot exceed 1000 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        if (cleanProject.length > 50) {
            val errMsg = "Project name cannot exceed 50 characters."
            showNotification("Validation Error", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        val duplicateOtherTask = allTasks.value.any { it.id != id && it.title.trim().equals(cleanTitle, ignoreCase = true) }
        if (duplicateOtherTask) {
            val errMsg = "Another task with title \"$cleanTitle\" already exists."
            showNotification("Duplicate Task", errMsg, isSuccess = false)
            onError(errMsg)
            return
        }

        viewModelScope.launch {
            val updatedTask = TaskEntity(
                id = id,
                title = cleanTitle,
                description = cleanDesc,
                timeOfDay = timeOfDay,
                scheduledTime = cleanScheduledTime,
                date = date,
                scheduleType = scheduleType,
                selectedDates = selectedDates.ifBlank { date },
                priority = priority,
                project = cleanProject,
                isCompleted = isCompleted,
                isStarred = isStarred,
                isFocusTask = isFocusTask,
                attachmentUri = attachmentUri,
                notificationEnabled = notificationEnabled,
                notificationTime = cleanNotificationTime
            )
            repository.updateTask(updatedTask)

            NotificationHelper.cancelReminder(getApplication(), id)
            if (notificationEnabled && !isCompleted) {
                NotificationHelper.scheduleReminder(
                    getApplication(),
                    id,
                    cleanTitle,
                    date,
                    cleanNotificationTime
                )
                showNotification(
                    "Task Updated ✏️",
                    "\"$cleanTitle\" updated with reminder at $cleanNotificationTime."
                )
            } else {
                showNotification(
                    "Task Updated ✏️",
                    "\"$cleanTitle\" changes saved successfully."
                )
            }
            onSuccess()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            NotificationHelper.cancelReminder(getApplication(), task.id)
            showNotification(
                "Task Removed 🗑️",
                "\"${task.title}\" and associated reminders were removed."
            )
        }
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun setTimeFilter(filter: String?) {
        _selectedTimeFilter.value = if (_selectedTimeFilter.value == filter) null else filter
    }

    fun setProjectFilter(project: String?) {
        _selectedProjectFilter.value = if (_selectedProjectFilter.value == project) null else project
    }

    fun setThemeMode(mode: AppThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }

    fun updateFocusDuration(minutes: Int) {
        val clamped = minutes.coerceIn(5, 120)
        preferencesRepository.setFocusDuration(clamped)
        if (!_isFocusRunning.value) {
            _remainingSeconds.value = clamped * 60
        }
    }

    fun toggleQuietHours() {
        preferencesRepository.toggleQuietHours()
    }

    // Focus Mode Logic
    fun startFocusSession(task: TaskEntity? = null) {
        val targetTask = task ?: allTasks.value.firstOrNull { it.isFocusTask }
                ?: allTasks.value.firstOrNull { !it.isCompleted }
        
        val durationMins = focusDurationMinutes.value.coerceIn(5, 120)
        
        // Reset timer only if switching tasks or if session has ended/not started
        if (targetTask != _activeFocusTask.value || _remainingSeconds.value <= 0 || _remainingSeconds.value > durationMins * 60) {
            _remainingSeconds.value = durationMins * 60
        }
        
        _activeFocusTask.value = targetTask
        _isFocusRunning.value = true

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0 && _isFocusRunning.value) {
                delay(1000)
                _remainingSeconds.value -= 1
            }
            if (_remainingSeconds.value == 0) {
                _isFocusRunning.value = false
                // Record completed focus session
                val taskTitle = _activeFocusTask.value?.title ?: "Quick Focus Session"
                repository.recordFocusSession(
                    FocusSessionEntity(
                        taskTitle = taskTitle,
                        durationMinutes = durationMins
                    )
                )
            }
        }
    }

    fun pauseFocusSession() {
        _isFocusRunning.value = false
        timerJob?.cancel()
    }

    fun resetFocusSession() {
        _isFocusRunning.value = false
        timerJob?.cancel()
        _remainingSeconds.value = focusDurationMinutes.value * 60
    }
}
