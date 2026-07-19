package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.OtherItem
import com.zephyr.boreal.domain.model.TempSelection
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.ui.components.BorealSearchField
import com.zephyr.boreal.ui.components.BorealTextInput
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.QuantityStepper
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private const val ANIMATION_DURATION_MS = 300

@Composable
fun SelectOtherItemsScreen(
  viewModel: SelectOtherItemsViewModel,
  onNavigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SelectOtherItemsScreenContent(
    uiState = uiState,
    onSearchQueryChanged = viewModel::onSearchQueryChanged,
    onToggleExpanded = viewModel::onToggleExpanded,
    onQuantityChanged = viewModel::onQuantityChanged,
    onNetPriceChanged = viewModel::onNetPriceChanged,
    onCommentChanged = viewModel::onCommentChanged,
    onConfirm = { viewModel.confirmHandler(onNavigateBack) },
  )
}

@Composable
internal fun SelectOtherItemsScreenContent(
  uiState: SelectOtherItemsUiState,
  onSearchQueryChanged: (String) -> Unit,
  onToggleExpanded: (Int) -> Unit,
  onQuantityChanged: (Int, Int?) -> Unit,
  onNetPriceChanged: (Int, Double?) -> Unit,
  onCommentChanged: (Int, String?) -> Unit,
  onConfirm: () -> Unit,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("hu-HU")) }

  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.select_other_items_title),
        actions = {
          IconButton(onClick = onConfirm, enabled = uiState.canAccept) {
            Icon(
              painter = painterResource(id = R.drawable.arrow_forward),
              contentDescription = stringResource(R.string.select_other_items_accept_description),
              tint = if (uiState.canAccept) BorealColors.White else BorealColors.Disabled,
              modifier = Modifier.size(32.dp),
            )
          }
        },
      )
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        SelectOtherItemsFooter(
          netTotal = uiState.netTotal,
          grossTotal = uiState.grossTotal,
          currencyFormat = currencyFormat,
        )
      }
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    OtherItemsList(
      uiState = uiState,
      currencyFormat = currencyFormat,
      onSearchQueryChanged = onSearchQueryChanged,
      onToggleExpanded = onToggleExpanded,
      onQuantityChanged = onQuantityChanged,
      onNetPriceChanged = onNetPriceChanged,
      onCommentChanged = onCommentChanged,
      modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp),
    )
  }
}

@Composable
private fun OtherItemsList(
  uiState: SelectOtherItemsUiState,
  currencyFormat: NumberFormat,
  onSearchQueryChanged: (String) -> Unit,
  onToggleExpanded: (Int) -> Unit,
  onQuantityChanged: (Int, Int?) -> Unit,
  onNetPriceChanged: (Int, Double?) -> Unit,
  onCommentChanged: (Int, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    BorealSearchField(
      query = uiState.searchQuery,
      onQueryChange = onSearchQueryChanged,
      placeholderText = stringResource(R.string.select_other_items_search_hint),
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyColumn(modifier = Modifier.weight(1f)) {
      items(uiState.items, key = { it.id }) { item ->
        val selection = uiState.selections[item.id]
        val isExpanded = uiState.expandedItemIds.contains(item.id)
        OtherItemSelectionAccordion(
          item = item,
          selection = selection,
          isExpanded = isExpanded,
          currencyFormat = currencyFormat,
          onHeaderClick = { onToggleExpanded(item.id) },
          onQuantityChange = { onQuantityChanged(item.id, it?.toInt()) },
          onNetPriceChange = { onNetPriceChanged(item.id, it) },
          onCommentChange = { onCommentChanged(item.id, it) },
        )
      }
    }
  }
}

@Suppress("MagicNumber")
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OtherItemSelectionAccordion(
  item: OtherItem,
  selection: TempSelection?,
  isExpanded: Boolean,
  currencyFormat: NumberFormat,
  onHeaderClick: () -> Unit,
  onQuantityChange: (Double?) -> Unit,
  onNetPriceChange: (Double?) -> Unit,
  onCommentChange: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val qty = (selection?.quantity ?: 0).toDouble()
  val effectivePrice = selection?.netPrice ?: item.netPrice
  val grossAmount =
    if (qty > 0) AmountCalculator.calculateAmounts(effectivePrice, qty, item.vatRate).grossAmount else 0.0
  val quantityStr = if (qty % 1 == 0.0) qty.toInt().toString() else qty.toString()
  val headerColor = if ((selection?.quantity ?: 0) > 0) BorealColors.Ok else BorealColors.Neutral
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2)
      bringIntoViewRequester.bringIntoView()
    }
  }

  Column(
    modifier =
      modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .fillMaxWidth()
        .padding(vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    OtherItemAccordionHeader(name = item.name, headerColor = headerColor, onHeaderClick = onHeaderClick)
    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
          OtherItemRow(
            label = stringResource(R.string.review_items_quantity_label),
            value = "$quantityStr ${item.unitName}",
          )
          OtherItemRow(
            label = stringResource(R.string.review_items_gross_label),
            value = currencyFormat.format(grossAmount),
          )
        }
        OtherItemExpandedContent(
          item = item,
          qty = qty,
          selection = selection,
          onQuantityChange = onQuantityChange,
          onNetPriceChange = onNetPriceChange,
          onCommentChange = onCommentChange,
        )
      }
    }
  }
}

@Composable
private fun OtherItemAccordionHeader(
  name: String,
  headerColor: Color,
  onHeaderClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .background(headerColor)
        .clickable(onClick = onHeaderClick)
        .padding(16.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = name,
      style = MaterialTheme.typography.titleLarge,
      color = BorealColors.White,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun OtherItemExpandedContent(
  item: OtherItem,
  qty: Double,
  selection: TempSelection?,
  onQuantityChange: (Double?) -> Unit,
  onNetPriceChange: (Double?) -> Unit,
  onCommentChange: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var priceText by remember(item.id, selection?.netPrice) {
    mutableStateOf((selection?.netPrice ?: item.netPrice).toInt().toString())
  }

  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
  ) {
    QuantityStepper(
      label = stringResource(R.string.select_other_items_quantity_label),
      quantity = if (qty > 0) qty else null,
      maxQuantity = null,
      onQuantityChange = onQuantityChange,
    )
    Spacer(modifier = Modifier.height(8.dp))
    BorealTextInput(
      label = stringResource(R.string.select_other_items_net_price_label),
      value = priceText,
      onValueChange = { text ->
        priceText = text
        onNetPriceChange(text.toDoubleOrNull())
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Spacer(modifier = Modifier.height(8.dp))
    BorealTextInput(
      label = stringResource(R.string.select_other_items_comment_label),
      value = selection?.comment ?: "",
      onValueChange = { onCommentChange(it.ifBlank { null }) },
      singleLine = false,
      maxLines = 4,
      maxLength = 100,
    )
  }
}

@Composable
private fun OtherItemRow(
  label: String,
  value: String,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(
      text = label,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = value,
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
    )
  }
}

@Composable
private fun SelectOtherItemsFooter(
  netTotal: Double,
  grossTotal: Double,
  currencyFormat: NumberFormat,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "${stringResource(R.string.select_other_items_net_total_label)} ${currencyFormat.format(netTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Text(
      text = "${stringResource(R.string.select_other_items_gross_total_label)} ${currencyFormat.format(grossTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
    )
  }
}
