package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.ErrorCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily

@Composable
fun EndErrandScreen(
  viewModel: EndErrandViewModel,
  onNavigateBack: () -> Unit,
  onSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsState()

  LaunchedEffect(uiState.isInternetReachable) {
    if (!uiState.isInternetReachable) {
      onNavigateBack()
    }
  }

  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_end_errand_title),
      )
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    EndErrandContent(
      uiState = uiState,
      onConfirm = { viewModel.finishRound(onSuccess) },
      modifier = Modifier.padding(paddingValues),
    )
  }
}

@Composable
private fun EndErrandContent(
  uiState: EndErrandUiState,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    if (uiState.isLoading || uiState.isEndErrandPending || uiState.isUserPending) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LoadingIndicator()
      }
    } else {
      EndErrandForm(
        uiState = uiState,
        onConfirm = onConfirm,
      )
    }
  }
}

@Composable
private fun EndErrandForm(
  uiState: EndErrandUiState,
  onConfirm: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
  ) {
    if (uiState.errorMessage != null) {
      ErrorCard(message = uiState.errorMessage)
      Spacer(modifier = Modifier.height(30.dp))
    }

    Text(
      text = stringResource(R.string.end_errand_description_1),
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Body,
      modifier = Modifier.padding(top = 10.dp),
    )

    Text(
      text = stringResource(R.string.end_errand_description_2),
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Body,
      modifier = Modifier.padding(top = 10.dp),
    )

    Text(
      text = stringResource(R.string.end_errand_description_3),
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Body,
      modifier = Modifier.padding(top = 10.dp),
    )

    uiState.disableReasonResId?.let { resId ->
      Text(
        text = stringResource(resId),
        color = BorealColors.Warning,
        fontFamily = NunitoSansFamily,
        fontSize = BorealFontSizes.Body,
        modifier = Modifier.padding(top = 10.dp),
      )
    }

    Spacer(modifier = Modifier.height(30.dp))

    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 30.dp),
    ) {
      BorealButton(
        text = stringResource(R.string.end_errand_button_label),
        variant = if (uiState.canFinishRound) ButtonVariant.WARNING else ButtonVariant.DISABLED,
        onClick = onConfirm,
        modifier = Modifier.align(Alignment.Center),
      )
    }
  }
}
