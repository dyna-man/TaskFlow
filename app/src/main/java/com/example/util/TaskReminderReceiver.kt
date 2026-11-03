package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("task_title") ?: "Scheduled Task"
        NotificationHelper.showNotification(
            context,
            "Task Reminder ⏰",
            "It's time to work on: \"$title\""
        )
    }
}
