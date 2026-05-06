package com.grieztech.ytorganizer

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grieztech.ytorganizer.data.api.YouTubeApiService
import com.grieztech.ytorganizer.data.api.YouTubeAuthManager
import com.grieztech.ytorganizer.data.local.AppDatabase
import com.grieztech.ytorganizer.data.local.ChannelDao
import com.grieztech.ytorganizer.data.local.FolderDao
import com.grieztech.ytorganizer.data.local.PlaylistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ═══════════════════════════════════════════════════════════════════════════
//  GriezTech - Hilt App Module
//  ✅ FIX: Migration من v1 إلى v2 لإضافة عمود accountId
// ═══════════════════════════════════════════════════════════════════════════

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val YOUTUBE_BASE_URL = "https://www.googleapis.com/youtube/v3/"

    // ✅ Migration v1 → v2: إضافة عمود accountId لكل جدول
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // إضافة accountId لجدول folders
            db.execSQL("ALTER TABLE folders   ADD COLUMN accountId TEXT NOT NULL DEFAULT ''")
            // إضافة accountId لجدول channels
            db.execSQL("ALTER TABLE channels  ADD COLUMN accountId TEXT NOT NULL DEFAULT ''")
            // إضافة accountId لجدول playlists
            db.execSQL("ALTER TABLE playlists ADD COLUMN accountId TEXT NOT NULL DEFAULT ''")
            // إضافة index للبحث السريع
            db.execSQL("CREATE INDEX IF NOT EXISTS index_folders_accountId   ON folders(accountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_accountId  ON channels(accountId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_accountId ON playlists(accountId)")
        }
    }

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)           // ✅ بدل fallbackToDestructiveMigration
            .build()

    @Provides fun provideFolderDao(db: AppDatabase):   FolderDao   = db.folderDao()
    @Provides fun provideChannelDao(db: AppDatabase):  ChannelDao  = db.channelDao()
    @Provides fun providePlaylistDao(db: AppDatabase): PlaylistDao = db.playlistDao()

    @Provides @Singleton
    fun provideAuthManager(@ApplicationContext ctx: Context): YouTubeAuthManager =
        YouTubeAuthManager(ctx)

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideRetrofit(okHttp: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(YOUTUBE_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideYouTubeApi(retrofit: Retrofit): YouTubeApiService =
        retrofit.create(YouTubeApiService::class.java)
}
