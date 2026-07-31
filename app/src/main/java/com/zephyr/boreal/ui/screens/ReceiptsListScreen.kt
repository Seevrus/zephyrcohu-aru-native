package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.theme.BorealColors
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsListScreen(
  modifier: Modifier = Modifier,
  onReceiptClick: (Int) -> Unit = {},
  viewModel: ReceiptsListViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  ReceiptsListScreenContent(
    uiState = uiState,
    onReceiptClick = onReceiptClick,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsListScreenContent(
  uiState: ReceiptsListUiState,
  onReceiptClick: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("hu", "HU")) }

  Scaffold(
    modifier = modifier,
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.screen_receipts_list_title))
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    LazyColumn(
      contentPadding = PaddingValues(16.dp),
      modifier =
        Modifier
          .padding(innerPadding)
          .fillMaxSize(),
    ) {
      items(uiState.receipts, key = { it.serialNumber }) { receipt ->
        ReceiptListItem(
          receipt = receipt,
          currencyFormat = currencyFormat,
          onClick = { onReceiptClick(receipt.serialNumber) },
          modifier = Modifier.padding(bottom = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun ReceiptListItem(
  receipt: ReceiptRowUiModel,
  currencyFormat: NumberFormat,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val backgroundColor = if (receipt.isCanceled) BorealColors.Magenta700 else BorealColors.Neutral

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(backgroundColor, RoundedCornerShape(8.dp))
        .clickable(onClick = onClick)
        .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = receipt.deliveryName,
      color = Color.White,
      style = MaterialTheme.typography.bodyLarge,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = "${receipt.serialNumber}/${receipt.yearCode}",
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
      )
      Text(
        text = currencyFormat.format(receipt.grossAmount),
        color = Color.White,
        style = MaterialTheme.typography.bodyMedium,
      )
    }
    if (receipt.isCanceled) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "${receipt.cancelSerialNumber}/${receipt.cancelYearCode}",
          color = Color.White,
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          text = stringResource(R.string.receipt_list_item_canceled_label),
          color = Color.White,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
    }
  }
}
