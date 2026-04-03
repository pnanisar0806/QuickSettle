package com.quicksettle.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for app-wide dependencies.
 *
 * Note: UserProfileStore, FriendRepository, CalculateSplitUseCase, and
 * GenerateUpiLinkUseCase all use @Inject constructor and are discovered
 * by Hilt automatically — no @Provides needed here.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
