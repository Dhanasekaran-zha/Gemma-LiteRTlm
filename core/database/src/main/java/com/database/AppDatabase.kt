package com.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.database.dao.ChatDao
import com.database.entities.ChatMessageEntity
import com.database.entities.ChatSessionEntity

@Database(
        entities = [ChatSessionEntity::class, ChatMessageEntity::class],
        version = 1,
        exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
}