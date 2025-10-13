package com.example.meetsphere.di

import com.example.meetsphere.data.repository.ActivitiesRepositoryImpl
import com.example.meetsphere.data.repository.ChatRepositoryImpl
import com.example.meetsphere.data.repository.LocationRepositoryImpl
import com.example.meetsphere.data.repository.UserRepositoryImpl
import com.example.meetsphere.domain.repository.ActivitiesRepository
import com.example.meetsphere.domain.repository.ChatRepository
import com.example.meetsphere.domain.repository.LocationRepository
import com.example.meetsphere.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindActivitiesRepository(activitiesRepositoryImpl: ActivitiesRepositoryImpl): ActivitiesRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(locationRepositoryImpl: LocationRepositoryImpl): LocationRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(userRepositoryImpl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(chatRepositoryImpl: ChatRepositoryImpl): ChatRepository
}
