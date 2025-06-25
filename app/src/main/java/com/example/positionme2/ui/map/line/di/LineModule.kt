package com.example.positionme2.ui.map.line.di

import com.example.positionme2.ui.map.line.*
import com.example.positionme2.ui.map.trajectory.TrajectoryPlaybackProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LineModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideBuildingBoundaryLineProvider(provider: BuildingBoundaryLineProvider): LineProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideWallLineProvider(provider: WallLineProvider): LineProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideTemporaryLineProvider(provider: TemporaryLineProvider): LineProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideUserTrajectoryLineProvider(provider: UserTrajectoryLineProvider): LineProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideTrajectoryPlaybackProvider(provider: TrajectoryPlaybackProvider): LineProvider = provider
}
