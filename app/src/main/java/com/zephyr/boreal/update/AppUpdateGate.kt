package com.zephyr.boreal.update

import com.zephyr.boreal.BuildConfig
import com.zephyr.boreal.network.AppUpdateChecker
import com.zephyr.boreal.store.update.AppUpdateStore
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gates the once-per-24h Play Store update check: prod builds only, online only, and at most
 * once per 24h since the last check attempt (regardless of whether that attempt found an
 * update) — deliberately stricter than RN's `useAppUpdates`, which only stamps its (unpersisted)
 * `lastChecked` timestamp when an update *was* found and so re-checks on every connectivity
 * change once none is found.
 */
@Singleton
open class AppUpdateGate
  @Inject
  constructor(
    private val appUpdateChecker: AppUpdateChecker,
    private val appUpdateStore: AppUpdateStore,
  ) {
    private val clock: Clock = Clock.systemUTC()

    companion object {
      val CHECK_INTERVAL: Duration = Duration.ofHours(24)
    }

    internal open fun isDebugBuild(): Boolean = BuildConfig.DEBUG

    internal open fun now(): Instant = clock.instant()

    open suspend fun checkForUpdate(isOnline: Boolean): Boolean {
      val lastCheckedAt = appUpdateStore.lastCheckedAt.first()
      val isDueForCheck = lastCheckedAt == null || Duration.between(lastCheckedAt, now()) >= CHECK_INTERVAL

      return if (isDebugBuild() || !isOnline || !isDueForCheck) {
        false
      } else {
        val isUpdateAvailable = runCatching { appUpdateChecker.isUpdateAvailable() }.getOrDefault(false)
        appUpdateStore.setLastCheckedAt(now())
        isUpdateAvailable
      }
    }
  }
