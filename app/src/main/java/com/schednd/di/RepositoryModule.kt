package com.schednd.di

import com.schednd.data.repository.AuthRepositoryImpl
import com.schednd.data.repository.EventRepositoryImpl
import com.schednd.data.repository.MessagingRepositoryImpl
import com.schednd.data.repository.NoteRepositoryImpl
import com.schednd.data.repository.NotificationRepositoryImpl
import com.schednd.data.repository.PlayerRepositoryImpl
import com.schednd.data.repository.RecentEventsRepositoryImpl
import com.schednd.data.repository.StorageRepositoryImpl
import com.schednd.data.work.SessionReminderSchedulerImpl
import com.schednd.domain.repository.AuthRepository
import com.schednd.domain.repository.EventRepository
import com.schednd.domain.repository.MessagingRepository
import com.schednd.domain.repository.NoteRepository
import com.schednd.domain.repository.NotificationRepository
import com.schednd.domain.repository.PlayerRepository
import com.schednd.domain.repository.RecentEventsRepository
import com.schednd.domain.repository.SessionReminderScheduler
import com.schednd.domain.repository.StorageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Único punto donde el dominio se entera de que detrás hay Firebase: cambiar de backend
 * es reescribir las implementaciones y tocar aquí, sin abrir un ViewModel.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindMessagingRepository(impl: MessagingRepositoryImpl): MessagingRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    @Singleton
    abstract fun bindRecentEventsRepository(impl: RecentEventsRepositoryImpl): RecentEventsRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindSessionReminderScheduler(
        impl: SessionReminderSchedulerImpl
    ): SessionReminderScheduler
}
