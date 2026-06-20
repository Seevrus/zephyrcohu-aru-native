package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.theme.BorealColors

@Composable
fun SearchPartnerNavScreen(
  viewModel: SearchPartnerNavViewModel,
  onNavigateToAddPartner: (taxNumber: String, selectedIndex: Int) -> Unit,
  onNavigateToManualEntry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SearchPartnerNavScreenContent(
    uiState = uiState,
    onTaxNumberChanged = viewModel::onTaxNumberChanged,
    onTaxPayerSelected = { index ->
      viewModel.onTaxPayerSelected(index, onNavigateToAddPartner)
    },
    onManualEntry = onNavigateToManualEntry,
    modifier = modifier,
  )
}

@Suppress("LongMethod")
@Composable
fun SearchPartnerNavScreenContent(
  uiState: SearchPartnerNavUiState,
  onTaxNumberChanged: (String) -> Unit,
  onTaxPayerSelected: (Int) -> Unit,
  onManualEntry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.search_partner_nav_title))
    },
    containerColor = BorealColors.Background,
    modifier = modifier,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues)
          .padding(16.dp),
    ) {
      OutlinedTextField(
        value = uiState.taxNumber,
        onValueChange = onTaxNumberChanged,
        label = { Text(stringResource(R.string.search_partner_nav_hint)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(16.dp))

      when {
        uiState.isSearching -> {
          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BorealColors.Ok)
          }
        }
        uiState.error != null -> {
          Text(
            text = stringResource(R.string.search_partner_nav_error),
            color = BorealColors.Error,
          )
        }
        uiState.taxNumber.length == TAX_NUMBER_DIGIT_COUNT && uiState.results.isEmpty() -> {
          Text(
            text = stringResource(R.string.search_partner_nav_no_results),
            color = BorealColors.White,
          )
        }
        else -> {
          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 16.dp),
          ) {
            itemsIndexed(uiState.results) { index, taxPayer ->
              TaxPayerResultItem(
                taxPayer = taxPayer,
                onSelect = { onTaxPayerSelected(index) },
              )
              if (index < uiState.results.lastIndex) {
                HorizontalDivider(color = BorealColors.White.copy(alpha = 0.2f))
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = onManualEntry,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = BorealColors.Neutral),
      ) {
        Text(
          text = stringResource(R.string.search_partner_nav_manual_entry),
          color = BorealColors.White,
        )
      }
    }
  }
}

@Composable
private fun TaxPayerResultItem(
  taxPayer: TaxPayer,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val deliveryLocation = taxPayer.locations["D"] ?: taxPayer.locations.values.firstOrNull()
  val name = deliveryLocation?.name ?: taxPayer.vatNumber
  val address =
    deliveryLocation?.let { "${it.postalCode} ${it.city}, ${it.address}" } ?: ""

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp),
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.titleMedium,
      color = BorealColors.White,
    )
    if (address.isNotBlank()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = address,
        style = MaterialTheme.typography.bodyMedium,
        color = BorealColors.White.copy(alpha = 0.7f),
      )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Button(
      onClick = onSelect,
      colors = ButtonDefaults.buttonColors(containerColor = BorealColors.Ok),
    ) {
      Text(
        text = stringResource(R.string.search_partner_nav_select),
        color = BorealColors.White,
      )
    }
  }
}
