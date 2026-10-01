package com.amoledwatchfaces.solarpath.watchfacepush

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideWatchFacePackageRepository(@ApplicationContext context: Context): WatchFacePackageRepository {
        return WatchFacePackageRepository(context)
    }
}
