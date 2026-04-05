package com.zephyr.boreal.network

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
  val isInternetReachable: StateFlow<Boolean>
}
