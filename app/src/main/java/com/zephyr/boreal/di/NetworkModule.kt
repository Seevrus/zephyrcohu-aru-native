package com.zephyr.boreal.di

import android.content.Context
import com.zephyr.boreal.BuildConfig
import com.zephyr.boreal.api.AuthApiService
import com.zephyr.boreal.api.AuthInterceptor
import com.zephyr.boreal.api.ItemApiService
import com.zephyr.boreal.api.PartnerApiService
import com.zephyr.boreal.api.ReceiptApiService
import com.zephyr.boreal.api.RoundApiService
import com.zephyr.boreal.api.StoreApiService
import com.zephyr.boreal.network.AndroidConnectivityObserver
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.core.ApplicationScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
  @Provides
  @Singleton
  fun provideConnectivityObserver(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
  ): ConnectivityObserver = AndroidConnectivityObserver(context, scope)

  @Provides
  @Singleton
  fun provideJson(): Json =
    Json {
      ignoreUnknownKeys = true
      coerceInputValues = true
    }

  @Provides
  @Singleton
  fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
    val logging =
      HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
      }
    return OkHttpClient
      .Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(logging)
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(
    okHttpClient: OkHttpClient,
    json: Json,
  ): Retrofit {
    val contentType = "application/json".toMediaType()
    return Retrofit
      .Builder()
      .baseUrl(BuildConfig.BASE_URL)
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory(contentType))
      .build()
  }

  @Provides
  @Singleton
  fun provideAuthApiService(retrofit: Retrofit): AuthApiService = retrofit.create(AuthApiService::class.java)

  @Provides
  @Singleton
  fun provideStoreApiService(retrofit: Retrofit): StoreApiService = retrofit.create(StoreApiService::class.java)

  @Provides
  @Singleton
  fun providePartnerApiService(retrofit: Retrofit): PartnerApiService = retrofit.create(PartnerApiService::class.java)

  @Provides
  @Singleton
  fun provideItemApiService(retrofit: Retrofit): ItemApiService = retrofit.create(ItemApiService::class.java)

  @Provides
  @Singleton
  fun provideRoundApiService(retrofit: Retrofit): RoundApiService = retrofit.create(RoundApiService::class.java)

  @Provides
  @Singleton
  fun provideReceiptApiService(retrofit: Retrofit): ReceiptApiService = retrofit.create(ReceiptApiService::class.java)
}
