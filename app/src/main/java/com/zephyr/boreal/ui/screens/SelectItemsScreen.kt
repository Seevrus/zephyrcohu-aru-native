package com.zephyr.boreal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BarcodeScanner
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.theme.BorealColors
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private const val ANIMATION_DURATION_MS = 300

@Suppress("LongMethod", "MagicNumber")
@Composable
fun SelectItemsScreen(
  viewModel: SelectItemsViewModel,
  onNavigateHome: () -> Unit,
  onNavigateNext: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var showExitWarning by remember { mutableStateOf(false) }
  var isScanning by remember { mutableStateOf(false) }
  var scannedBarcode by remember { mutableStateOf<String?>(null) }

  BackHandler {
    if (isScanning) {
      isScanning = false
    } else {
      showExitWarning = true
    }
  }

  if (showExitWarning) {
    BorealAlert(
      title = stringResource(R.string.select_items_exit_warning_title),
      message = stringResource(R.string.select_items_exit_warning_message),
      confirmButtonText = stringResource(R.string.dialog_yes),
      cancelButtonText = stringResource(R.string.dialog_cancel),
      onConfirmClick = {
        showExitWarning = false
        viewModel.resetReceipts()
        onNavigateHome()
      },
      onCancelClick = { showExitWarning = false },
      onDismissRequest = { showExitWarning = false },
    )
  }

  if (isScanning) {
    Scaffold(
      topBar = { BorealTopAppBar(title = stringResource(R.string.scanner_title)) },
      containerColor = BorealColors.Background,
    ) { paddingValues ->
      Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(350.dp)
              .padding(horizontal = 24.dp, vertical = 30.dp)
              .clip(
                androidx.compose.foundation.shape
                  .RoundedCornerShape(8.dp),
              ),
        ) {
          BarcodeScanner(
            onBarcodeScanned = { barcode ->
              scannedBarcode = barcode
            },
            modifier = Modifier.fillMaxSize(),
          )
        }

        if (scannedBarcode != null) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text(
              text = stringResource(R.string.scanner_scanned_code),
              color = Color.White,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = scannedBarcode!!,
              color = Color.White,
              style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(20.dp))
            com.zephyr.boreal.ui.components.BorealButton(
              text = stringResource(R.string.scanner_accept_and_back),
              variant = com.zephyr.boreal.ui.components.ButtonVariant.WARNING,
              onClick = {
                viewModel.onBarcodeQueryChanged(scannedBarcode!!)
                isScanning = false
              },
            )
          }
        }
      }
    }
    return
  }

  SelectItemsScreenContent(
    uiState = uiState,
    onSearchQueryChanged = viewModel::onSearchQueryChanged,
    onBarcodeIconClick = {
      scannedBarcode = null
      isScanning = true
    },
    onClearBarcodeClick = { viewModel.onBarcodeQueryChanged("") },
    onToggleItemExpanded = viewModel::onToggleItemExpanded,
    onUpsertSelectedItem = viewModel::upsertSelectedItem,
    onUpsertOrderItem = viewModel::upsertOrderItem,
    onConfirmItems = {
      viewModel.confirmItemsHandler(onSuccess = onNavigateNext)
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SelectItemsScreenContent(
  uiState: SelectItemsUiState,
  onSearchQueryChanged: (String) -> Unit,
  onBarcodeIconClick: () -> Unit,
  onClearBarcodeClick: () -> Unit,
  onToggleItemExpanded: (Int) -> Unit,
  onUpsertSelectedItem: (Int, Int, Double?) -> Unit,
  onUpsertOrderItem: (Int, Double?) -> Unit,
  onConfirmItems: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.select_items_title),
        actions = {
          if (uiState.canConfirmItems) {
            IconButton(onClick = onConfirmItems) {
              Icon(
                painter = painterResource(id = R.drawable.arrow_forward),
                contentDescription = "Tovább",
                tint = BorealColors.White,
                modifier = Modifier.size(32.dp),
              )
            }
          }
        },
      )
    },
    bottomBar = {
      Column {
        HorizontalDivider(color = Color.White, thickness = 1.dp)
        SelectItemsFooter(
          netTotal = uiState.netTotal,
          grossTotal = uiState.grossTotal,
          totalQuantity = uiState.totalQuantity,
          netOrderTotal = uiState.netOrderTotal,
          grossOrderTotal = uiState.grossOrderTotal,
          totalOrderQuantity = uiState.totalOrderQuantity,
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
      SelectItemsSearchHeader(
        searchQuery = uiState.searchQuery,
        barcodeQuery = uiState.barcodeQuery,
        onSearchQueryChanged = onSearchQueryChanged,
        onBarcodeIconClick = onBarcodeIconClick,
        onClearBarcodeClick = onClearBarcodeClick,
      )

      LazyColumn(
        modifier = Modifier.weight(1f),
      ) {
        items(
          items = uiState.items,
          key = { it.id },
        ) { item ->
          val isExpanded = uiState.expandedItemIds.contains(item.id)
          val itemSelected = uiState.selectedItems.containsKey(item.id)
          val itemOrdered = uiState.selectedOrderItems.containsKey(item.id)

          val headerColor =
            if (itemSelected || itemOrdered) {
              BorealColors.Ok
            } else {
              BorealColors.Neutral
            }

          ItemAccordion(
            item = item,
            isExpanded = isExpanded,
            headerColor = headerColor,
            selectedQuantities = uiState.selectedItems[item.id] ?: emptyMap(),
            orderQuantity = uiState.selectedOrderItems[item.id],
            onToggleExpanded = { onToggleItemExpanded(item.id) },
            onUpsertSelectedItem = { expId, qty -> onUpsertSelectedItem(item.id, expId, qty) },
            onUpsertOrderItem = { qty -> onUpsertOrderItem(item.id, qty) },
          )
        }
      }
    }
  }
}

@Composable
private fun SelectItemsSearchHeader(
  searchQuery: String,
  barcodeQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  onBarcodeIconClick: () -> Unit,
  onClearBarcodeClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    com.zephyr.boreal.ui.components.BorealSearchField(
      query = searchQuery,
      onQueryChange = onSearchQueryChanged,
      modifier = Modifier.weight(1f),
      placeholderText = stringResource(R.string.select_items_search_placeholder),
    )
    Spacer(modifier = Modifier.width(8.dp))
    if (barcodeQuery.isNotEmpty()) {
      IconButton(onClick = onClearBarcodeClick) {
        Icon(
          painter = painterResource(id = R.drawable.error_exclamation),
          contentDescription = stringResource(R.string.select_items_delete),
          tint = Color.White,
          modifier = Modifier.size(32.dp),
        )
      }
    } else {
      IconButton(onClick = onBarcodeIconClick) {
        Icon(
          painter = painterResource(id = R.drawable.barcode_scanne),
          contentDescription = stringResource(R.string.select_items_barcode),
          tint = Color.White,
          modifier = Modifier.size(32.dp),
        )
      }
    }
  }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber")
@Composable
private fun ItemAccordion(
  item: SellItem,
  isExpanded: Boolean,
  headerColor: Color,
  selectedQuantities: Map<Int, Double>,
  orderQuantity: Double?,
  onToggleExpanded: () -> Unit,
  onUpsertSelectedItem: (Int, Double?) -> Unit,
  onUpsertOrderItem: (Double?) -> Unit,
) {
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2)
      bringIntoViewRequester.bringIntoView()
    }
  }

  Column(
    modifier =
      Modifier
        .bringIntoViewRequester(bringIntoViewRequester)
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(BorealColors.Neutral),
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerColor)
          .clickable(onClick = onToggleExpanded)
          .padding(16.dp),
    ) {
      Text(
        text = item.name,
        style = MaterialTheme.typography.titleLarge,
        color = BorealColors.White,
        fontWeight = FontWeight.Bold,
      )
    }

    AnimatedVisibility(
      visible = isExpanded,
      enter = expandVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
      exit = shrinkVertically(animationSpec = tween(ANIMATION_DURATION_MS)),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(8.dp),
      ) {
        item.expirations.forEach { exp ->
          val currentQty = selectedQuantities[exp.expirationId]
          SelectionRow(
            label = "${exp.expiresAt.take(6)} (${exp.quantity.toInt()})",
            quantity = currentQty,
            maxQuantity = exp.quantity,
            onQuantityChange = { onUpsertSelectedItem(exp.expirationId, it) },
          )
        }

        SelectionRow(
          label = stringResource(R.string.select_items_order_row),
          quantity = orderQuantity,
          maxQuantity = null,
          onQuantityChange = { onUpsertOrderItem(it) },
        )
      }
    }
  }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun SelectionRow(
  label: String,
  quantity: Double?,
  maxQuantity: Double?,
  onQuantityChange: (Double?) -> Unit,
) {
  val currentQty = quantity ?: 0.0

  val handleIncrease = {
    val next = currentQty + 1.0
    if (maxQuantity == null || next <= maxQuantity) {
      onQuantityChange(next)
    } else {
      // Cannot exceed available quantity
      onQuantityChange(maxQuantity)
    }
  }

  val handleDecrease = {
    val next = currentQty - 1.0
    if (next <= 0) {
      onQuantityChange(null)
    } else {
      onQuantityChange(next)
    }
  }

  val handleTextChange = { text: String ->
    val cleanText = text.trim().replace(",", ".")
    val num = cleanText.toDoubleOrNull()
    if (num == null || num <= 0) {
      onQuantityChange(null)
    } else if (maxQuantity != null && num > maxQuantity) {
      onQuantityChange(maxQuantity)
    } else {
      onQuantityChange(num)
    }
  }

  Column(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      color = Color.White,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.padding(bottom = 4.dp),
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxWidth(),
    ) {
      IconButton(onClick = handleDecrease) {
        Icon(
          painter = painterResource(id = R.drawable.remove_circle),
          contentDescription = stringResource(R.string.select_items_decrease),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }

      Spacer(modifier = Modifier.width(16.dp))

      TextField(
        value =
          if (quantity !=
            null
          ) {
            (if (quantity % 1 == 0.0) quantity.toInt().toString() else quantity.toString())
          } else {
            ""
          },
        onValueChange = handleTextChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(140.dp),
        colors =
          TextFieldDefaults.colors(
            focusedContainerColor = BorealColors.Input,
            unfocusedContainerColor = BorealColors.Input,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
          ),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
        singleLine = true,
      )

      Spacer(modifier = Modifier.width(16.dp))

      IconButton(onClick = handleIncrease) {
        Icon(
          painter = painterResource(id = R.drawable.add_circle),
          contentDescription = stringResource(R.string.select_items_increase),
          tint = Color.White,
          modifier = Modifier.size(40.dp),
        )
      }
    }
  }
}

@Composable
private fun SelectItemsFooter(
  netTotal: Double,
  grossTotal: Double,
  totalQuantity: Int,
  netOrderTotal: Double,
  grossOrderTotal: Double,
  totalOrderQuantity: Int,
) {
  val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("hu", "HU")) }
  val netTotalStr = currencyFormat.format(netTotal)
  val grossTotalStr = currencyFormat.format(grossTotal)
  val netOrderTotalStr = currencyFormat.format(netOrderTotal)
  val grossOrderTotalStr = currencyFormat.format(grossOrderTotal)

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .background(BorealColors.Neutral)
        .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    androidx.compose.material3.Text(
      text =
        buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.select_items_purchase_label))
          }
          append("$netTotalStr / $grossTotalStr ($totalQuantity)")
        },
      color = Color.White,
      style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth(),
    )
    androidx.compose.material3.Text(
      text =
        buildAnnotatedString {
          withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(stringResource(R.string.select_items_order_label))
          }
          append("$netOrderTotalStr / $grossOrderTotalStr ($totalOrderQuantity)")
        },
      color = Color.White,
      style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
  }
}
