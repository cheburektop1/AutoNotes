package com.example.autonotes.di

import android.content.Context
import androidx.room.Room
import com.example.autonotes.data.db.AppDatabase
import com.example.autonotes.data.db.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "autonotes.db")
            // ТОЛЬКО НА ВРЕМЯ РАЗРАБОТКИ: при смене схемы база пересоздаётся (данные теряются).
            // Перед релизом заменим на настоящие миграции.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRecordingDao(db: AppDatabase): RecordingDao = db.recordingDao()

    /** Scope уровня приложения: переживает сервис и экраны (нужен для сохранения записи в БД). */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}