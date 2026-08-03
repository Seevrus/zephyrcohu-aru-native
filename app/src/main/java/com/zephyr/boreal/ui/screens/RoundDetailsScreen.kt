package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.Round
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.PrinterConnector
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun RoundDetailsScreen(
  modifier: Modifier = Modifier,
  viewModel: RoundDetailsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  RoundDetailsScreenContent(uiState = uiState, modifier = modifier)
}

@Composable
internal fun RoundDetailsScreenContent(
  uiState: RoundDetailsUiState,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.screen_round_details_title))
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    val round = uiState.round

    if (uiState.isLoading || round == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator(message = stringResource(R.string.round_details_loading))
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
      RoundDetailsInfoSection(round = round)

      if (uiState.textToPrint != null) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
          text = stringResource(R.string.print_end_errand_print_intro),
          color = BorealColors.White,
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrinterConnector(
          textToPrint = uiState.textToPrint,
          suggestedFileName = uiState.suggestedFileName,
          onPrintSuccess = {},
        )
      }
    }
  }
}

@Composable
private fun RoundDetailsInfoSection(round: Round) {
  Column {
    InfoRow(label = stringResource(R.string.round_details_store_label), value = round.store.name)
    InfoRow(label = stringResource(R.string.round_details_started_label), value = round.roundStarted)
    InfoRow(label = stringResource(R.string.round_details_finished_label), value = round.roundFinished ?: "")
    InfoRow(label = stringResource(R.string.round_details_partner_list_label), value = round.partnerList.name)
  }
}

@Composable
private fun InfoRow(
  label: String,
  value: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, color = Color.White)
    Text(text = value, color = Color.White)
  }
}
