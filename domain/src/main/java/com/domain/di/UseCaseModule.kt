package com.domain.di

import com.domain.repository.ChatRepository
import com.domain.repository.SettingsRepository
import com.domain.usecase.GetGemmaResponseUseCase
import com.domain.usecase.ThemeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideGetGemmaResponseUseCase(repository: ChatRepository): GetGemmaResponseUseCase {
        return GetGemmaResponseUseCase(repository)
    }

    @Provides
    @Singleton
    fun provideThemeUseCase(repository: SettingsRepository): ThemeUseCase {
        return ThemeUseCase(repository)
    }
}