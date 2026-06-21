package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealSearchField
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.InfoCard
import com.zephyr.boreal.ui.theme.BorealColors

private const val ANIMATION_DURATION_MS = 300

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
    onFilterQueryChanged = viewModel::onFilterQueryChanged,
    onResultTapped = viewModel::onResultTapped,
    onConfirmSelection = { viewModel.onConfirmSelection(onNavigateToAddPartner) },
    onManualEntry = onNavigateToManualEntry,
    modifier = modifier,
  )
}

@Suppress("LongMethod")
@Composable
fun SearchPartnerNavScreenContent(
  uiState: SearchPartnerNavUiState,
  onTaxNumberChanged: (String) -> Unit,
  onFilterQueryChanged: (String) -> Unit,
  onResultTapped: (Int) -> Unit,
  onConfirmSelection: () -> Unit,
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
      if (uiState.taxNumber.isEmpty()) {
        InfoCard(message = stringResource(R.string.search_partner_nav_info))
      }

      BorealTextInput(
        label = stringResource(R.string.search_partner_nav_label),
        value = uiState.taxNumber,
        onValueChange = onTaxNumberChanged,
        maxLength = TAX_NUMBER_DIGIT_COUNT,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      )

      Spacer(modifier = Modifier.height(16.dp))

      Box(modifier = Modifier.fillMaxWidth()) {
        BorealButton(
          text = stringResource(R.string.search_partner_nav_manual_entry),
          variant = ButtonVariant.NEUTRAL,
          onClick = onManualEntry,
          modifier = Modifier.align(Alignment.Center),
        )
      }

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
        uiState.results.isNotEmpty() -> {
          val displayedResults =
            if (uiState.filterQuery.isBlank()) {
              uiState.results
            } else {
              uiState.results.filter { taxPayer ->
                val location = taxPayer.locations["D"] ?: taxPayer.locations.values.firstOrNull()
                val name = location?.name ?: taxPayer.vatNumber
                val address = location?.let { "${it.postalCode} ${it.city} ${it.address}" } ?: ""
                name.contains(uiState.filterQuery, ignoreCase = true) ||
                  address.contains(uiState.filterQuery, ignoreCase = true)
              }
            }

          LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp),
          ) {
            itemsIndexed(displayedResults) { _, taxPayer ->
              TaxPayerResultItem(
                taxPayer = taxPayer,
                isSelected = taxPayer.id == uiState.selectedResultId,
                onTap = { onResultTapped(taxPayer.id) },
                onConfirm = onConfirmSelection,
              )
            }
          }

          HorizontalDivider(color = BorealColors.White.copy(alpha = 0.3f))

          Spacer(modifier = Modifier.height(8.dp))

          BorealSearchField(
            query = uiState.filterQuery,
            onQueryChange = onFilterQueryChanged,
            placeholderText = stringResource(R.string.search_partner_nav_filter_label),
          )
        }
      }
    }
  }
}

@Suppress("LongMethod")
@Composable
private fun TaxPayerResultItem(
  taxPayer: TaxPayer,
  isSelected: Boolean,
  onTap: () -> Unit,
  onConfirm: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val deliveryLocation = taxPayer.locations["D"] ?: taxPayer.locations.values.firstOrNull()
  val name = deliveryLocation?.name ?: taxPayer.vatNumber
  val address =
    deliveryLocation?.let { "${it.postalCode} ${it.city}, ${it.address}" } ?: ""
  val headerColor: Color = if (isSelected) BorealColors.Ok else BorealColors.Neutral

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerColor)
          .clickable(onClick = onTap)
          .padding(16.dp),
    ) {
      Text(
        text = name,
        style = MaterialTheme.typography.titleLarge,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
      )
    }

    AnimatedVisibility(
      visible = isSelected,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(16.dp),
      ) {
        if (address.isNotBlank()) {
          Text(
            text = address,
            style = MaterialTheme.typography.bodyMedium,
            color = BorealColors.White,
          )
        }

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 8.dp),
          color = BorealColors.White.copy(alpha = 0.5f),
        )

        Button(
          onClick = onConfirm,
          modifier = Modifier.align(Alignment.CenterHorizontally),
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BorealColors.Ok),
        ) {
          Text(
            text = stringResource(R.string.search_partner_nav_select),
            style = MaterialTheme.typography.titleLarge,
            color = BorealColors.White,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
