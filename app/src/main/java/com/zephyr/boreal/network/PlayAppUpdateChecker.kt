package com.zephyr.boreal.network

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestUpdateFlow
import kotlinx.coroutines.flow.first

open class PlayAppUpdateChecker(
  private val context: Context,
) : AppUpdateChecker {
  override suspend fun isUpdateAvailable(): Boolean {
    val appUpdateManager = AppUpdateManagerFactory.create(context)
    return appUpdateManager.requestUpdateFlow().first() is AppUpdateResult.Available
  }
}
