package com.zephyr.boreal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.ReceiptItem
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import com.zephyr.boreal.ui.theme.NunitoSansFamily
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private const val ANIMATION_DURATION_MS = 300

@Composable
fun ReviewItemsScreen(
  viewModel: ReviewItemsViewModel,
  onNavigateHome: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  if (uiState.showCancelConfirmation) {
    BorealAlert(
      title = stringResource(R.string.review_items_cancel_title),
      message = stringResource(R.string.review_items_cancel_message),
      confirmButtonText = stringResource(R.string.review_items_cancel_confirm),
      confirmButtonVariant = ButtonVariant.WARNING,
      cancelButtonText = stringResource(R.string.review_items_cancel_dismiss),
      onConfirmClick = { viewModel.cancelReceipt(onNavigateHome) },
      onCancelClick = { viewModel.dismissCancelDialog() },
      onDismissRequest = { viewModel.dismissCancelDialog() },
    )
  }

  ReviewItemsScreenContent(
    uiState = uiState,
    onToggleExpanded = viewModel::onToggleExpanded,
    onRemoveItem = { id, expirationId -> viewModel.removeItem(id, expirationId, onNavigateHome) },
    onCancelClick = viewModel::showCancelDialog,
  )
}

@Composable
private fun ReviewItemsScreenContent(
  uiState: ReviewItemsUiState,
  onToggleExpanded: (String) -> Unit,
  onRemoveItem: (Int, Int) -> Unit,
  onCancelClick: () -> Unit,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("hu", "HU")) }

  Scaffold(
    topBar = {
      BorealTopAppBar(title = stringResource(R.string.review_items_title))
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        ReviewItemsFooter(
          grossTotal = uiState.grossTotal,
          currencyFormat = currencyFormat,
          onCancelClick = onCancelClick,
        )
      }
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(paddingValues),
    ) {
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
      ) {
        BorealButton(
          text = stringResource(R.string.review_items_extra_items),
          variant = ButtonVariant.OK,
          onClick = {},
        )
      }

      LazyColumn(
        modifier = Modifier.weight(1f),
      ) {
        items(
          items = uiState.items,
          key = { "${it.id}_${it.expirationId}" },
        ) { item ->
          val key = "${item.id}_${item.expirationId}"
          val isExpanded = uiState.expandedItemKeys.contains(key)

          ReviewItemAccordion(
            item = item,
            isExpanded = isExpanded,
            currencyFormat = currencyFormat,
            onHeaderClick = { onToggleExpanded(key) },
            onRemoveClick = { onRemoveItem(item.id, item.expirationId) },
          )
        }
      }
    }
  }
}

@Suppress("MagicNumber")
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ReviewItemAccordion(
  item: ReceiptItem,
  isExpanded: Boolean,
  currencyFormat: NumberFormat,
  onHeaderClick: () -> Unit,
  onRemoveClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val headerColor = if (isExpanded) BorealColors.Ok else BorealColors.Neutral
  val bringIntoViewRequester = remember { BringIntoViewRequester() }
  val quantityStr =
    if (item.quantity % 1 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()

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
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    ReviewItemAccordionHeader(
      name = item.name,
      expiresAt = item.expiresAt,
      headerColor = headerColor,
      onHeaderClick = onHeaderClick,
    )

    HorizontalDivider(color = Color.White, thickness = 1.dp)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
      ReviewItemRow(stringResource(R.string.review_items_quantity_label), "$quantityStr ${item.unitName}")
      ReviewItemRow(stringResource(R.string.review_items_gross_label), currencyFormat.format(item.grossAmount))
    }

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      ReviewItemAccordionExpandedContent(
        articleNumber = item.articleNumber,
        onRemoveClick = onRemoveClick,
      )
    }
  }
}

@Composable
private fun ReviewItemAccordionHeader(
  name: String,
  expiresAt: String,
  headerColor: androidx.compose.ui.graphics.Color,
  onHeaderClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
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
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = expiresAt,
      style = MaterialTheme.typography.titleLarge,
      color = BorealColors.White,
      fontWeight = FontWeight.Bold,
    )
  }
}

@Composable
private fun ReviewItemAccordionExpandedContent(
  articleNumber: String,
  onRemoveClick: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
    ReviewItemRow(
      label = stringResource(R.string.review_items_article_number_label),
      value = articleNumber,
    )
    BorealButton(
      text = stringResource(R.string.review_items_delete),
      variant = ButtonVariant.WARNING,
      onClick = onRemoveClick,
      modifier = Modifier.padding(top = 12.dp),
    )
  }
}

@Composable
private fun ReviewItemRow(
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
private fun ReviewItemsFooter(
  grossTotal: Double,
  currencyFormat: NumberFormat,
  onCancelClick: () -> Unit,
) {
  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = "${stringResource(R.string.review_items_total_label)} ${currencyFormat.format(grossTotal)}",
      color = BorealColors.White,
      fontFamily = NunitoSansFamily,
      fontSize = BorealFontSizes.Input,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 12.dp),
    )
    Row(
      horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      BorealButton(
        text = stringResource(R.string.review_items_cancel),
        variant = ButtonVariant.WARNING,
        onClick = onCancelClick,
      )
      BorealButton(
        text = stringResource(R.string.review_items_finalize),
        variant = ButtonVariant.OK,
        onClick = {},
      )
    }
  }
}
