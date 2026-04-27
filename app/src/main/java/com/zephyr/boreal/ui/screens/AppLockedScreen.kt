package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.SettingsButton
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun AppLockedScreen(
  modifier: Modifier = Modifier,
  onNavigateToMain: () -> Unit = {},
  onNavigateToSettings: () -> Unit = {},
  viewModel: MainViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  AppLockedScreenContent(
    userName = uiState.userName ?: "",
    canUseApp = uiState.canUseApp,
    onNavigateToMain = onNavigateToMain,
    onNavigateToSettings = onNavigateToSettings,
    modifier = modifier,
  )
}

@Composable
fun AppLockedScreenContent(
  userName: String,
  canUseApp: Boolean?,
  onNavigateToMain: () -> Unit,
  onNavigateToSettings: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LaunchedEffect(canUseApp) {
    if (canUseApp == true) {
      onNavigateToMain()
    }
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_main_title),
        actions = {
          SettingsButton(onClick = onNavigateToSettings)
        },
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    Box(
      modifier =
        Modifier
          .padding(innerPadding)
          .fillMaxSize(),
      contentAlignment = Alignment.TopCenter,
    ) {
      ErrorCard(
        message = stringResource(R.string.app_locked_message, userName),
      )
    }
  }
}
