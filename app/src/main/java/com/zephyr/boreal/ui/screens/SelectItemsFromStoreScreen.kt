package com.zephyr.boreal.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyr.boreal.R
import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.ui.components.BarcodeScanner
import com.zephyr.boreal.ui.components.BorealAlert
import com.zephyr.boreal.ui.components.BorealButton
import com.zephyr.boreal.ui.components.BorealSearchField
import com.zephyr.boreal.ui.components.BorealTopAppBar
import com.zephyr.boreal.ui.components.ButtonVariant
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.theme.BorealColors
import kotlinx.coroutines.delay

private const val ANIMATION_DURATION_MS = 300

@Suppress("LongMethod")
@Composable
fun SelectItemsFromStoreScreen(
  viewModel: SelectItemsFromStoreViewModel,
  onNavigateHome: () -> Unit,
  onNavigateNext: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  var isScanning by remember { mutableStateOf(false) }
  var scannedBarcode by remember { mutableStateOf<String?>(null) }

  BackHandler {
    if (isScanning) {
      isScanning = false
    } else {
      viewModel.requestAbandon(onAbandoned = onNavigateHome)
    }
  }

  if (uiState.showAbandonConfirm) {
    BorealAlert(
      title = stringResource(R.string.select_items_from_store_abandon_title),
      message = stringResource(R.string.select_items_from_store_abandon_message),
      confirmButtonText = stringResource(R.string.dialog_yes),
      cancelButtonText = stringResource(R.string.dialog_cancel),
      onConfirmClick = { viewModel.confirmAbandon(onAbandoned = onNavigateHome) },
      onCancelClick = viewModel::dismissAbandonConfirm,
      onDismissRequest = viewModel::dismissAbandonConfirm,
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
              .clip(RoundedCornerShape(8.dp)),
        ) {
          BarcodeScanner(
            onBarcodeScanned = { barcode -> scannedBarcode = barcode },
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
            BorealButton(
              text = stringResource(R.string.scanner_accept_and_back),
              variant = ButtonVariant.WARNING,
              onClick = {
                viewModel.onSearchQueryChanged(scannedBarcode!!)
                isScanning = false
              },
            )
          }
        }
      }
    }
    return
  }

  SelectItemsFromStoreScreenContent(
    uiState = uiState,
    onSearchQueryChanged = viewModel::onSearchQueryChanged,
    onBarcodeIconClick = {
      scannedBarcode = null
      isScanning = true
    },
    onToggleExpanded = viewModel::onToggleExpanded,
    onQuantityChanged = viewModel::onQuantityChanged,
    onForward = onNavigateNext,
    onAbandonClick = { viewModel.requestAbandon(onAbandoned = onNavigateHome) },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun SelectItemsFromStoreScreenContent(
  uiState: SelectItemsFromStoreUiState,
  onSearchQueryChanged: (String) -> Unit,
  onBarcodeIconClick: () -> Unit,
  onToggleExpanded: (Int) -> Unit,
  onQuantityChanged: (Int, Int, Int?) -> Unit,
  onForward: () -> Unit,
  onAbandonClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      BorealTopAppBar(
        title = stringResource(R.string.screen_select_items_from_store_title),
        actions = {
          if (uiState.isAnyItemChanged) {
            IconButton(onClick = onForward) {
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
        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .background(BorealColors.Neutral)
              .padding(16.dp),
          horizontalArrangement = Arrangement.Center,
        ) {
          BorealButton(
            text = stringResource(R.string.select_items_from_store_abandon_button),
            variant = ButtonVariant.WARNING,
            onClick = onAbandonClick,
          )
        }
      }
    },
    containerColor = BorealColors.Background,
  ) { paddingValues ->
    if (uiState.isLoading) {
      Box(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator(message = stringResource(R.string.select_items_from_store_loading_message))
      }
    } else {
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
          verticalAlignment = Alignment.CenterVertically,
        ) {
          BorealSearchField(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChanged,
            modifier = Modifier.weight(1f),
            placeholderText = stringResource(R.string.select_items_search_placeholder),
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(onClick = onBarcodeIconClick) {
            Icon(
              painter = painterResource(id = R.drawable.barcode_scanne),
              contentDescription = stringResource(R.string.select_items_barcode),
              tint = Color.White,
              modifier = Modifier.size(32.dp),
            )
          }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
          items(
            items = uiState.displayItems,
            key = { it.expirationId },
          ) { item ->
            StorageListItemRow(
              item = item,
              isExpanded = uiState.expandedExpirationIds.contains(item.expirationId),
              onToggleExpanded = { onToggleExpanded(item.expirationId) },
              onQuantityChanged = { qty -> onQuantityChanged(item.itemId, item.expirationId, qty) },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongMethod", "MagicNumber", "CyclomaticComplexMethod")
@Composable
private fun StorageListItemRow(
  item: StorageListItem,
  isExpanded: Boolean,
  onToggleExpanded: () -> Unit,
  onQuantityChanged: (Int?) -> Unit,
) {
  val bringIntoViewRequester = remember { BringIntoViewRequester() }

  LaunchedEffect(isExpanded) {
    if (isExpanded) {
      delay(ANIMATION_DURATION_MS.toLong() / 2)
      bringIntoViewRequester.bringIntoView()
    }
  }

  var shownText by remember(item.itemId, item.expirationId) {
    mutableStateOf(item.quantityChange?.toString() ?: "")
  }

  fun commit(rawText: String) {
    if (rawText == "-") {
      shownText = "-"
      return
    }
    val resolved = resolveQuantityChange(rawText, item.originalQuantity)
    shownText = resolved?.toString() ?: ""
    onQuantityChanged(resolved)
  }

  val rowColor =
    when {
      item.quantityChange != null -> BorealColors.Blue400
      item.originalQuantity == 0.0 -> BorealColors.Warning
      else -> BorealColors.Neutral
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
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(rowColor)
          .clickable(onClick = onToggleExpanded)
          .padding(16.dp),
    ) {
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = item.name,
          style = MaterialTheme.typography.titleLarge,
          color = BorealColors.White,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = item.expiresAt,
          style = MaterialTheme.typography.titleLarge,
          color = BorealColors.White,
          fontWeight = FontWeight.Bold,
        )
      }
      Row(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = stringResource(R.string.select_items_from_store_own_stock_label),
          color = BorealColors.White,
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = (item.originalQuantity ?: 0.0).toInt().toString(),
          color = BorealColors.White,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
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
            .padding(16.dp),
      ) {
        if (item.itemBarcode.isNotBlank() || item.expirationBarcode.isNotBlank()) {
          Text(
            text = "${stringResource(R.string.select_items_barcode)}:",
            color = BorealColors.White,
            fontWeight = FontWeight.Bold,
          )
          if (item.itemBarcode.isNotBlank()) {
            Text(text = item.itemBarcode, color = BorealColors.White)
          }
          if (item.expirationBarcode.isNotBlank()) {
            Text(text = item.expirationBarcode, color = BorealColors.White)
          }
          Spacer(modifier = Modifier.height(8.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = stringResource(R.string.select_items_from_store_primary_stock_label),
            color = BorealColors.White,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = item.primaryStoreQuantity?.toInt()?.toString() ?: "-",
            color = BorealColors.White,
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = stringResource(R.string.select_items_from_store_load_label),
          color = BorealColors.White,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.fillMaxWidth(),
        ) {
          IconButton(onClick = { commit(((item.quantityChange ?: 0) - 1).toString()) }) {
            Icon(
              painter = painterResource(id = R.drawable.remove_circle),
              contentDescription = stringResource(R.string.select_items_decrease),
              tint = Color.White,
              modifier = Modifier.size(40.dp),
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          TextField(
            value = shownText,
            onValueChange = { commit(it) },
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

          IconButton(onClick = { commit(((item.quantityChange ?: 0) + 1).toString()) }) {
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
  }
}
