package com.zephyr.boreal.store.user

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.zephyr.boreal.store.core.ApplicationScope
import kotlinx.coroutines.CoroutineScope
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
    }

    val userState: StateFlow<UserState> =
      dataStore.data
        .map { preferences ->
          val deviceId = preferences[DEVICE_ID]
          val token = preferences[TOKEN]
          val isPasswordExpired = preferences[IS_PASSWORD_EXPIRED]
          val expiresAt = preferences[EXPIRES_AT]

          val storedToken =
            if (token != null && isPasswordExpired != null && expiresAt != null) {
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
          preferences[EXPIRES_AT] = token.expiresAt
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
      }
    }
  }
