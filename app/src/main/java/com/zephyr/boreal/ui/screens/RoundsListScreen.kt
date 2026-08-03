package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundsListScreen(
  modifier: Modifier = Modifier,
  onRoundClick: (Int) -> Unit = {},
  viewModel: RoundsListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  RoundsListScreenContent(
    uiState = uiState,
    onRoundClick = onRoundClick,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RoundsListScreenContent(
  uiState: RoundsListUiState,
  onRoundClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.screen_rounds_list_title))
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator(message = stringResource(R.string.rounds_list_loading))
      }
      return@Scaffold
    }

    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      modifier =
        Modifier
          .padding(innerPadding)
          .fillMaxSize(),
    ) {
      items(uiState.rounds, key = { it.id }) { round ->
        RoundListItem(
          round = round,
          onClick = { onRoundClick(round.id) },
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun RoundListItem(
  round: RoundListRowUiModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = round.storeName,
      color = Color.White,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(text = stringResource(R.string.rounds_list_item_started), color = Color.White)
      Text(text = round.roundStarted, color = Color.White)
    }
    if (round.roundFinished != null) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(text = stringResource(R.string.rounds_list_item_finished), color = Color.White)
        Text(text = round.roundFinished, color = Color.White)
      }
    }
  }
}
