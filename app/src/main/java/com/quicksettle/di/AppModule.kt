package com.quicksettle.di

import com.quicksettle.data.local.UserProfile
import com.quicksettle.data.local.UserProfileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindUserProfile(impl: UserProfileStore): UserProfile
}
