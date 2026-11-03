package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {
    private const val CHANNEL_ID = "task_flow_reminders"
    private const val CHANNEL_NAME = "Task Flow Reminders"
    private const val CHANNEL_DESC = "Notifications for task reminders, schedule alerts, and productivity milestones."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, title: String, message: String) {
        try {
            createNotificationChannel(context)
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)

            val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
            notificationManager.notify(notificationId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Unable to schedule or display notification: ${e.message}", e)
        }
    }

    fun scheduleReminder(context: Context, taskId: Long, title: String, dateStr: String, timeStr: String) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd h:mm a", java.util.Locale.US)
            val fullDateTimeStr = "$dateStr $timeStr"
            val parsedDate = sdf.parse(fullDateTimeStr) ?: return
            val triggerAtMillis = parsedDate.time

            if (triggerAtMillis <= System.currentTimeMillis()) return

            val intent = android.content.Intent(context, TaskReminderReceiver::class.java).apply {
                putExtra("task_id", taskId)
                putExtra("task_title", title)
            }

            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error scheduling reminder alarm: ${e.message}", e)
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        try {
            val intent = android.content.Intent(context, TaskReminderReceiver::class.java)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                taskId.toInt(),
                intent,
                android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            android.util.Log.e("NotificationHelper", "Error canceling reminder alarm: ${e.message}", e)
        }
    }
}
