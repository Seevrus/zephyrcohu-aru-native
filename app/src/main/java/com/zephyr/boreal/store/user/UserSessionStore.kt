package com.zephyr.boreal.store.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserSessionStore
  @Inject
  constructor(
    private val dataStore: DataStore<Preferences>,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) {
    companion object {
      val DEVICE_ID = stringPreferencesKey("device_id")
      val TOKEN = stringPreferencesKey("token")
      val IS_PASSWORD_EXPIRED = booleanPreferencesKey("is_password_expired")
      val EXPIRES_AT = stringPreferencesKey("expires_at")

      /**
       * A **record** per the persistence-durability convention (see
       * `rn-migration-tasks/00-overview.md`): nothing server-side recreates "we printed the
       * end-errand summary but the app died before the exit/logout sequence finished", so this
       * flag must survive process death for [MainViewModel][com.zephyr.boreal.ui.screens.MainViewModel]'s
       * startup crash-recovery consumer to find it.
       */
      val IS_PENDING_RESET_AFTER_PRINT = booleanPreferencesKey("is_pending_reset_after_print")
    }

    val userState: StateFlow<UserState> =
      dataStore.data
        .map { preferences ->
          val deviceId = preferences[DEVICE_ID]
          val token = preferences[TOKEN]
          val isPasswordExpired = preferences[IS_PASSWORD_EXPIRED]
          val expiresAt = preferences[EXPIRES_AT]

          val storedToken =
            if (token != null && isPasswordExpired != null) {
              StoredToken(
                token = token,
                isPasswordExpired = isPasswordExpired,
                expiresAt = expiresAt,
              )
            } else {
              null
            }

          UserState(
            deviceId = deviceId,
            storedToken = storedToken,
          )
        }.stateIn(
          scope = scope,
          started = SharingStarted.Eagerly,
          initialValue = UserState(),
        )

    suspend fun updateSession(
      deviceId: String?,
      token: StoredToken?,
    ) {
      dataStore.edit { preferences ->
        if (deviceId != null) {
          preferences[DEVICE_ID] = deviceId
        } else {
          preferences.remove(DEVICE_ID)
        }

        if (token != null) {
          preferences[TOKEN] = token.token
          preferences[IS_PASSWORD_EXPIRED] = token.isPasswordExpired
          token.expiresAt?.let {
            preferences[EXPIRES_AT] = it
          } ?: preferences.remove(EXPIRES_AT)
        } else {
          preferences.remove(TOKEN)
          preferences.remove(IS_PASSWORD_EXPIRED)
          preferences.remove(EXPIRES_AT)
        }
      }
    }

    suspend fun clearSession() {
      dataStore.edit { preferences ->
        preferences.remove(DEVICE_ID)
        preferences.remove(TOKEN)
        preferences.remove(IS_PASSWORD_EXPIRED)
        preferences.remove(EXPIRES_AT)
        preferences.remove(IS_PENDING_RESET_AFTER_PRINT)
      }
    }

    val isPendingResetAfterPrint: Flow<Boolean> =
      dataStore.data.map { preferences -> preferences[IS_PENDING_RESET_AFTER_PRINT] == true }

    suspend fun setPendingResetAfterPrint(isPending: Boolean) {
      dataStore.edit { preferences ->
        if (isPending) {
          preferences[IS_PENDING_RESET_AFTER_PRINT] = true
        } else {
          preferences.remove(IS_PENDING_RESET_AFTER_PRINT)
        }
      }
    }
  }
