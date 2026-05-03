package com.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.database.dao.EmployeeDao
import com.database.entities.ClientEntity
import com.database.entities.EmployeeEntity

@Database(
    entities = [EmployeeEntity::class, ClientEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    // ClientDao would be added here similarly
}