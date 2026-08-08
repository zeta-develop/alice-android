package com.alice.app.di

import com.alice.app.data.repository.VoiceRepositoryImpl
import com.alice.app.domain.repository.VoiceRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindVoiceRepository(
        voiceRepositoryImpl: VoiceRepositoryImpl
    ): VoiceRepository
}
