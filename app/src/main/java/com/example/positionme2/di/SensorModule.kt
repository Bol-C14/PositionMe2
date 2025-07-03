package com.example.positionme2.di

import com.example.positionme2.domain.initialization.PdrInitializationService
import com.example.positionme2.domain.initialization.PdrInitializationStrategy
import com.example.positionme2.domain.initialization.strategies.GpsInitializationStrategy
import com.example.positionme2.domain.initialization.strategies.ManualInitializationStrategy
import com.example.positionme2.domain.location.LocationProvider
import com.example.positionme2.domain.sensor.SensorFusionService
import com.example.positionme2.service.initialization.PdrInitializationServiceImpl
import com.example.positionme2.service.location.GpsLocationProvider
import com.example.positionme2.service.SensorFusionServiceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SensorModule {

    @Binds
    @Singleton
    abstract fun bindSensorFusionService(
        sensorFusionServiceImpl: SensorFusionServiceImpl
    ): SensorFusionService

    @Binds
    @Singleton
    abstract fun bindLocationProvider(
        gpsLocationProvider: GpsLocationProvider
    ): LocationProvider

    @Binds
    @Singleton
    abstract fun bindPdrInitializationService(
        pdrInitializationServiceImpl: PdrInitializationServiceImpl
    ): PdrInitializationService

    companion object {
        @Provides
        @IntoSet
        fun provideGpsInitializationStrategy(
            gpsInitializationStrategy: GpsInitializationStrategy
        ): PdrInitializationStrategy = gpsInitializationStrategy

        @Provides
        @IntoSet
        fun provideManualInitializationStrategy(
            manualInitializationStrategy: ManualInitializationStrategy
        ): PdrInitializationStrategy = manualInitializationStrategy
    }
}
