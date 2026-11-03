package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ScheduleType(val label: String) {
    ONE_DAY("One day"),
    TWO_TO_FIVE_DAYS("2 to 5 days"),
    MULTIPLE_DAYS("Multiple days"),
    FOREVER("Forever");

    companion object {
        fun fromLabel(label: String): ScheduleType {
            return entries.find { it.label.equals(label, ignoreCase = true) } ?: ONE_DAY
        }
    }
}

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["title"]),
        Index(value = ["project"]),
        Index(value = ["date"]),
        Index(value = ["isCompleted"]),
        Index(value = ["priority"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeOfDay: String = "Morning", // "Morning", "Afternoon", "Evening"
    val scheduledTime: String = "9:00 AM",
    val date: String = "2024-09-09", // Format YYYY-MM-DD
    val scheduleType: String = "One day", // "One day", "2 to 5 days", "Multiple days", "Forever"
    val selectedDates: String = "2024-09-09",
    val priority: String = "Medium", // "Low", "Medium", "High"
    val project: String = "Core Work", // "Core Work", "Marketing", "Health & Wellness", etc.
    val isCompleted: Boolean = false,
    val isStarred: Boolean = false,
    val isFocusTask: Boolean = false,
    val completedTimestamp: Long? = null,
    val estimatedDurationMinutes: Int = 25,
    val attachmentUri: String? = null,
    val notificationEnabled: Boolean = true,
    val notificationTime: String = "9:00 AM",
    val syncedWithCloud: Boolean = true
)

fun TaskEntity.isScheduledForDate(targetDateStr: String): Boolean {
    val cleanTarget = targetDateStr.trim()
    val cleanDate = date.trim()

    val type = ScheduleType.fromLabel(scheduleType)
    if (type == ScheduleType.FOREVER) return true

    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

    if (type == ScheduleType.MULTIPLE_DAYS) {
        try {
            val parsedTarget = sdf.parse(cleanTarget)
            if (parsedTarget != null) {
                val dayOfWeek = java.text.SimpleDateFormat("EEE", java.util.Locale.US).format(parsedTarget)
                val tokens = selectedDates.split(",", " ", ";", "|").map { it.trim() }
                if (tokens.any { it.equals(dayOfWeek, ignoreCase = true) || it.equals(cleanTarget, ignoreCase = true) }) {
                    return true
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskEntity", "Error parsing date for multiple days schedule: ${e.message}", e)
        }
        if (selectedDates.contains("All 365 Days", ignoreCase = true)) return true
    }

    if (cleanDate.isBlank()) return false
    if (cleanDate == cleanTarget) return true
    if (selectedDates.split(",", " ", ";").map { it.trim() }.any { it == cleanTarget }) return true

    if (selectedDates.contains(" to ")) {
        try {
            val parts = selectedDates.split(" to ")
            if (parts.size >= 2) {
                val startStr = parts[0].trim()
                val endStr = parts[1].substringBefore(" ").trim()
                val targetDateObj = sdf.parse(cleanTarget)
                val startDateObj = sdf.parse(startStr)
                val endDateObj = sdf.parse(endStr)
                if (targetDateObj != null && startDateObj != null && endDateObj != null) {
                    return !targetDateObj.before(startDateObj) && !targetDateObj.after(endDateObj)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("TaskEntity", "Error parsing date range schedule: ${e.message}", e)
        }
    }

    return false
}
