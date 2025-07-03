package com.example.positionme2.ui.map.features.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object TrajectoryFeatureModule {
    // All feature managers are already annotated with @Singleton and @Inject constructor
    // so Hilt will automatically provide them
}
