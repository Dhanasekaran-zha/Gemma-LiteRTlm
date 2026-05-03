package com.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val company: String,
    val contactEmail: String
)