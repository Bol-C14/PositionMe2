package com.example.positionme2.di

import android.content.Context
import com.example.positionme2.domain.pdr.PdrProcessor
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.service.SensorFusionServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds
    @Singleton
    abstract fun bindSensorFusionService(impl: SensorFusionServiceImpl): SensorFusionService

    companion object {
        @Provides
        @Singleton
        fun providePdrProcessor(@ApplicationContext context: Context): PdrProcessor {
            return PdrProcessor(context)
        }
    }
}
