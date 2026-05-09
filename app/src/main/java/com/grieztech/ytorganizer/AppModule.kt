package com.grieztech.ytorganizer

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.grieztech.ytorganizer.data.api.YouTubeApiService
import com.grieztech.ytorganizer.data.api.YouTubeAuthManager
import com.grieztech.ytorganizer.data.remote.RemoteConfigManager
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


    //new migration
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("UPDATE folders SET emoji = '📺' WHERE name = 'All Channels'")
        }
    }

    // ✅ Migration v3 → v4: تغيير ForeignKey من CASCADE إلى NO ACTION
    // بدلاً من حذف القنوات عند حذف المجلد، ننقلها للمجلد الافتراضي
    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // إعادة بناء جدول channels بدون CASCADE
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS channels_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    accountId TEXT NOT NULL DEFAULT '',
                    folderId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    thumbnailUrl TEXT NOT NULL DEFAULT '',
                    subscriberCount INTEGER NOT NULL DEFAULT 0,
                    videoCount INTEGER NOT NULL DEFAULT 0,
                    position INTEGER NOT NULL DEFAULT 0,
                    lastFetched INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE NO ACTION
                )
            """.trimIndent())
            db.execSQL("INSERT INTO channels_new SELECT * FROM channels")
            db.execSQL("DROP TABLE channels")
            db.execSQL("ALTER TABLE channels_new RENAME TO channels")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_folderId  ON channels(folderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_channels_accountId ON channels(accountId)")

            // إعادة بناء جدول playlists بدون CASCADE
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS playlists_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    accountId TEXT NOT NULL DEFAULT '',
                    folderId INTEGER NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    thumbnailUrl TEXT NOT NULL DEFAULT '',
                    itemCount INTEGER NOT NULL DEFAULT 0,
                    channelTitle TEXT NOT NULL DEFAULT '',
                    position INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE NO ACTION
                )
            """.trimIndent())
            db.execSQL("INSERT INTO playlists_new SELECT * FROM playlists")
            db.execSQL("DROP TABLE playlists")
            db.execSQL("ALTER TABLE playlists_new RENAME TO playlists")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_folderId  ON playlists(folderId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_playlists_accountId ON playlists(accountId)")
        }
    }
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
            db.execSQL("UPDATE folders SET emoji = '📺' WHERE name = 'All Channels'")
        }
    }

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)           // ✅ بدل fallbackToDestructiveMigration
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

    @Provides @Singleton
    fun provideRemoteConfig(): RemoteConfigManager = RemoteConfigManager()
}
