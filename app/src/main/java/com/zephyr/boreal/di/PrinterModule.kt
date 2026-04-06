package com.zephyr.boreal.di

import android.content.Context
import com.zephyr.boreal.printer.BluetoothHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {
  @Provides
  @Singleton
  fun provideBluetoothHelper(
    @ApplicationContext context: Context,
  ): BluetoothHelper = BluetoothHelper(context)
}
