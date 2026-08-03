package com.zephyr.boreal.network

interface AppUpdateChecker {
  suspend fun isUpdateAvailable(): Boolean
}
