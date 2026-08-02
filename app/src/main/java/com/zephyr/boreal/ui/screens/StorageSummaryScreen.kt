package com.zephyr.boreal.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.PrinterConnector
import com.zephyr.boreal.ui.components.SuccessCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun StorageSummaryScreen(
  viewModel: StorageSummaryViewModel,
  onNavigateHome: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler(enabled = true) {
    viewModel.onLeave(onNavigateHome)
  }

  StorageSummaryScreenContent(
    uiState = uiState,
    onReturnClick = { viewModel.onLeave(onNavigateHome) },
  )
}

@Composable
internal fun StorageSummaryScreenContent(
  uiState: StorageSummaryUiState,
  onReturnClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.storage_summary_title))
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator()
      }
      return@Scaffold
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
      SuccessCard(message = stringResource(R.string.storage_summary_success_message))

      if (uiState.textToPrint != null) {
        Text(text = stringResource(R.string.storage_summary_print_intro))
        Spacer(modifier = Modifier.height(8.dp))
        PrinterConnector(
          textToPrint = uiState.textToPrint,
          suggestedFileName = uiState.suggestedFileName,
          onPrintSuccess = {},
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      Text(text = stringResource(R.string.storage_summary_no_more_printing))

      Spacer(modifier = Modifier.height(30.dp))

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
      ) {
        BorealButton(
          text = stringResource(R.string.storage_summary_return_button),
          variant = if (uiState.isInternetReachable) ButtonVariant.WARNING else ButtonVariant.DISABLED,
          onClick = onReturnClick,
          modifier = Modifier.align(Alignment.Center),
        )
      }
    }
  }
}
