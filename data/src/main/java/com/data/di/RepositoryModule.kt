package com.data.di

import com.data.repository.ChatRepositoryImpl
import com.domain.repository.ChatRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

//    @Binds
//    @Singleton
//    abstract fun bindAuthRepository(
//        authRepositoryImpl: AuthRepositoryImpl
//    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
            impl: ChatRepositoryImpl
    ): ChatRepository

}