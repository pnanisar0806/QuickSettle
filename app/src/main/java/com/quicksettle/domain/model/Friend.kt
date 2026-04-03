package com.quicksettle.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "friends")
data class Friend(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val upiId: String? = null,
    val lastUsed: Long = System.currentTimeMillis(),
)
