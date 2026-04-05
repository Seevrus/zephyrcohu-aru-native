package com.zephyr.boreal.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.zephyr.boreal.store.core.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidConnectivityObserver
  @Inject
  constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
  ) : ConnectivityObserver {
    private val connectivityManager =
      context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetReachable = MutableStateFlow(checkCurrentNetworkState())

    override val isInternetReachable: StateFlow<Boolean> =
      _isInternetReachable.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = _isInternetReachable.value,
      )

    init {
      val networkRequest =
        NetworkRequest
          .Builder()
          .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
          .build()

      connectivityManager.registerNetworkCallback(
        networkRequest,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _isInternetReachable.value = true
          }

          override fun onLost(network: Network) {
            super.onLost(network)
            _isInternetReachable.value = false
          }

          override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
          ) {
            super.onCapabilitiesChanged(network, networkCapabilities)
            val hasInternet =
              networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            _isInternetReachable.value = hasInternet
          }
        },
      )
    }

    private fun checkCurrentNetworkState(): Boolean {
      val activeNetwork = connectivityManager.activeNetwork
      val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
      return capabilities?.let {
        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
          it.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
      } ?: false
    }
  }
