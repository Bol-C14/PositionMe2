package com.example.positionme2.ui.map.engine.di

import android.content.Context
import com.example.positionme2.data.repository.IndoorMapRepository
import com.example.positionme2.ui.map.engine.GoogleMapEngine
import com.example.positionme2.ui.map.engine.MapEngine
import com.example.positionme2.ui.map.engine.adapter.FusedPositionProvider
import com.example.positionme2.ui.map.engine.adapter.GnssPositionProvider
import com.example.positionme2.ui.map.engine.adapter.PdrPositionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapEngineModule {

    @Provides
    @Singleton
    fun provideMapEngine(
        @ApplicationContext context: Context,
        indoorMapRepository: IndoorMapRepository,
        gnssPositionProvider: GnssPositionProvider,
        pdrPositionProvider: PdrPositionProvider,
        fusedPositionProvider: FusedPositionProvider
    ): MapEngine {
        return GoogleMapEngine(
            context = context,
            indoorMapRepository = indoorMapRepository,
            gnssPositionProvider = gnssPositionProvider,
            pdrPositionProvider = pdrPositionProvider,
            fusedPositionProvider = fusedPositionProvider
        )
    }
}
