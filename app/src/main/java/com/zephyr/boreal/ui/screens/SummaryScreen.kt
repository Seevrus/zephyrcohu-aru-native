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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.Receipt
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.SuccessCard
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun SummaryScreen(
  viewModel: SummaryViewModel,
  onNavigateHome: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler(enabled = true) {
    viewModel.onLeave(onNavigateHome)
  }

  SummaryScreenContent(
    uiState = uiState,
    onReturnClick = { viewModel.onLeave(onNavigateHome) },
  )
}

@Composable
internal fun SummaryScreenContent(
  uiState: SummaryUiState,
  onReturnClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.summary_title))
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
      if (!uiState.isInternetReachable) {
        InfoCard(message = stringResource(R.string.summary_offline_notice))
      }

      SuccessCard(message = stringResource(R.string.summary_success_message))

      SummaryPrintSectionSlot(receipt = uiState.receipt)

      Spacer(modifier = Modifier.height(30.dp))

      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
      ) {
        BorealButton(
          text = stringResource(R.string.summary_return_button),
          variant = ButtonVariant.OK,
          onClick = onReturnClick,
          modifier = Modifier.align(Alignment.Center),
        )
      }
    }
  }
}

/**
 * Drop-in slot for Task 03's print section. Intentionally empty for now — takes the finalized
 * [receipt] so Task 03 can render printing controls for it without changing this call site.
 */
@Suppress("UnusedParameter", "EmptyFunctionBlock")
@Composable
private fun SummaryPrintSectionSlot(receipt: Receipt?) {
}
