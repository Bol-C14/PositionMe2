package com.example.positionme2.di

import com.example.positionme2.domain.recording.RecordingService
import com.example.positionme2.service.RecordingServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecordingModule {

    @Binds
    @Singleton
    abstract fun bindRecordingService(impl: RecordingServiceImpl): RecordingService
}
