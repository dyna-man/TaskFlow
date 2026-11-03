package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isStarred DESC, isCompleted ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isStarred DESC, isCompleted ASC, id ASC")
    fun getTasksForDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedTimestamp DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted, completedTimestamp = :timestamp WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean, timestamp: Long?)

    @Query("UPDATE tasks SET isStarred = :isStarred WHERE id = :id")
    suspend fun updateTaskStarred(id: Long, isStarred: Boolean)

    @Query("UPDATE tasks SET isCompleted = 0, completedTimestamp = NULL WHERE isCompleted = 1 AND scheduleType != 'One day'")
    suspend fun resetTasksForNewDay()

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTaskCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM tasks WHERE LOWER(TRIM(title)) = LOWER(TRIM(:title)))")
    suspend fun taskExists(title: String): Boolean

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllFocusSessions()

    // Focus sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusSession(session: FocusSessionEntity)

    @Query("SELECT * FROM focus_sessions ORDER BY timestamp DESC")
    fun getAllFocusSessions(): Flow<List<FocusSessionEntity>>
}
