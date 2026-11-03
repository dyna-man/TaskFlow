package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.TaskDao
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskEntity

@Database(
    entities = [TaskEntity::class, FocusSessionEntity::class],
    version = 5,
    exportSchema = false
)
abstract class TaskFlowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: TaskFlowDatabase? = null

        fun getDatabase(context: Context): TaskFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskFlowDatabase::class.java,
                    "taskflow_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
