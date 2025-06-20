package com.example.positionme2.di

import com.example.positionme2.data.repository.IndoorMapRepository
import com.example.positionme2.data.repository.StaticIndoorMapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MapModule {

    @Binds
    @Singleton
    abstract fun bindIndoorMapRepository(
        staticIndoorMapRepository: StaticIndoorMapRepository
    ): IndoorMapRepository
}

