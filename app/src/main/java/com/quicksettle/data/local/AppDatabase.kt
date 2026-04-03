package com.quicksettle.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quicksettle.domain.model.Friend

@Database(
    entities = [Friend::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun friendDao(): FriendDao
}
