package com.example.positionme2.ui.map.marker.di

import com.example.positionme2.ui.map.marker.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MarkerModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideDynamicGnssMarkerProvider(provider: DynamicGnssMarkerProvider): MarkerProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideDynamicPdrMarkerProvider(provider: DynamicPdrMarkerProvider): MarkerProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideDynamicFusedMarkerProvider(provider: DynamicFusedMarkerProvider): MarkerProvider = provider

    @Provides
    @IntoSet
    @Singleton
    fun provideStaticRegionsOfInterestMarkerProvider(provider: StaticRegionsOfInterestMarkerProvider): MarkerProvider = provider
}
