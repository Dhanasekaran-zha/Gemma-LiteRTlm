package com.database.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.database.AppDatabase
import com.database.dao.ChatDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Migration from v1 (text-only messages) to v2 (multimodal messages).
     *
     * Adds image fields, message type, generation state, and renames
     * the timestamp column from 'timestamp' to 'createdAt'.
     * Also adds indices for performance.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Rename old table
            db.execSQL("ALTER TABLE chat_messages RENAME TO chat_messages_old")

            // Create new table with full schema
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    messageId INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    sessionId INTEGER NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER',
                    text TEXT NOT NULL DEFAULT '',
                    image_uri TEXT,
                    image_mime_type TEXT,
                    message_type TEXT NOT NULL DEFAULT 'TEXT',
                    generation_state TEXT NOT NULL DEFAULT 'COMPLETE',
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(sessionId) REFERENCES chat_sessions(sessionId) ON DELETE CASCADE
                )
            """.trimIndent())

            // Migrate existing data
            db.execSQL("""
                INSERT INTO chat_messages (messageId, sessionId, role, text, createdAt)
                SELECT messageId, sessionId,
                    CASE WHEN isFromUser = 1 THEN 'USER' ELSE 'MODEL' END,
                    content,
                    timestamp
                FROM chat_messages_old
            """.trimIndent())

            // Drop old table
            db.execSQL("DROP TABLE chat_messages_old")

            // Create indices
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_sessionId ON chat_messages(sessionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_chat_messages_createdAt ON chat_messages(createdAt)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gemma_edge"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }
}