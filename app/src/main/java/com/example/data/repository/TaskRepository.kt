package com.example.data.repository

import com.example.data.dao.TaskDao
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val focusSessions: Flow<List<FocusSessionEntity>> = taskDao.getAllFocusSessions()

    suspend fun getTaskById(id: Long): TaskEntity? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun taskExists(title: String): Boolean = withContext(Dispatchers.IO) {
        taskDao.taskExists(title)
    }

    suspend fun insertTask(task: TaskEntity): Long = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
    }

    suspend fun toggleTaskCompletion(task: TaskEntity) = withContext(Dispatchers.IO) {
        val newStatus = !task.isCompleted
        val timestamp = if (newStatus) System.currentTimeMillis() else null
        taskDao.updateTaskCompletion(task.id, newStatus, timestamp)
    }

    suspend fun toggleTaskStarred(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.updateTaskStarred(task.id, !task.isStarred)
    }

    suspend fun deleteTask(task: TaskEntity) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
    }

    suspend fun recordFocusSession(session: FocusSessionEntity) = withContext(Dispatchers.IO) {
        taskDao.insertFocusSession(session)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        taskDao.deleteAllTasks()
        taskDao.deleteAllFocusSessions()
    }

    suspend fun resetTasksForNewDay() = withContext(Dispatchers.IO) {
        taskDao.resetTasksForNewDay()
    }

    suspend fun prepopulateInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Clear all dummy tasks and focus sessions so the app starts completely clean
        taskDao.deleteAllTasks()
        taskDao.deleteAllFocusSessions()
    }
}
