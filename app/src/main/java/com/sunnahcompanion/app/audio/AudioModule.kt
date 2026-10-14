package com.sunnahcompanion.app.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

/**
 * Recitations stream once and are then served from a persistent disk cache, so a surah played
 * at home is available later in the car without a connection.
 */
@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    private const val CACHE_BYTES = 300L * 1024 * 1024

    @Provides
    @Singleton
    fun providesAudioCache(@ApplicationContext context: Context): SimpleCache = SimpleCache(
        File(context.filesDir, "audio_cache"),
        LeastRecentlyUsedCacheEvictor(CACHE_BYTES),
        StandaloneDatabaseProvider(context),
    )

    @Provides
    @Singleton
    fun providesCacheDataSourceFactory(
        @ApplicationContext context: Context,
        cache: SimpleCache,
    ): CacheDataSource.Factory = CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(
            DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory().setAllowCrossProtocolRedirects(true)),
        )
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}
