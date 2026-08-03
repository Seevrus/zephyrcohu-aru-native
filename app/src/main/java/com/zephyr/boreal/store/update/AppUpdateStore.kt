package com.zephyr.boreal.store.update

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A **record** per the persistence-durability convention (see
 * `rn-migration-tasks/00-overview.md`): unlike RN's in-memory `useState(0)`, this must survive
 * process death so the once-per-24h Play Store update check actually throttles across app
 * restarts rather than resetting on every launch.
 */
@Singleton
class AppUpdateStore
  @Inject
  constructor(
    private val dataStore: DataStore<Preferences>,
  ) {
    companion object {
      val LAST_CHECKED_AT = stringPreferencesKey("update_last_checked_at")
    }

    val lastCheckedAt: Flow<Instant?> =
      dataStore.data.map { preferences -> preferences[LAST_CHECKED_AT]?.let(Instant::parse) }

    suspend fun setLastCheckedAt(instant: Instant) {
      dataStore.edit { preferences ->
        preferences[LAST_CHECKED_AT] = instant.toString()
      }
    }
  }
