package com.quicksettle.di

import android.content.Context
import androidx.room.Room
import com.quicksettle.data.local.AppDatabase
import com.quicksettle.data.local.FriendDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context = context,
            klass = AppDatabase::class.java,
            name = "quicksettle.db",
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideFriendDao(database: AppDatabase): FriendDao = database.friendDao()
}
