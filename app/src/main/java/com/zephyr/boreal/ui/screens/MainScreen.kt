package com.zephyr.boreal.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zephyr.boreal.R
import com.zephyr.boreal.ui.components.BorealTile
import com.zephyr.boreal.ui.components.LoadingIndicator
import com.zephyr.boreal.ui.components.TileVariant
import com.zephyr.boreal.ui.theme.BorealColors
import com.zephyr.boreal.ui.theme.BorealFontSizes
import kotlinx.coroutines.delay

@Composable
fun getTiles(numberOfReceipts: Int): List<TileData> =
  listOf(
    TileData(
      stringResource(R.string.tile_storage),
      TileVariant.OK,
      ImageVector.vectorResource(R.drawable.truck_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_sell),
      TileVariant.WARNING,
      ImageVector.vectorResource(R.drawable.cart_arrow_down_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_errands),
      TileVariant.NEUTRAL,
      ImageVector.vectorResource(R.drawable.rectangle_list_solid_full),
    ),
    TileData(
      stringResource(R.string.tile_receipts, numberOfReceipts),
      TileVariant.DISABLED,
      ImageVector.vectorResource(R.drawable.receipt_solid_full),
    ),
  )

private const val FONT_WARM_UP_DELAY_MS = 1000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
  val tiles = getTiles(numberOfReceipts = 0)
  var isReady by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    // Brief warm-up to allow downloadable fonts to fetch and avoid visual "jump"
    delay(FONT_WARM_UP_DELAY_MS)
    isReady = true
  }

  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = BorealColors.Background,
            titleContentColor = BorealColors.White,
          ),
        title = {
          Text(
            stringResource(R.string.title_main),
            fontSize = BorealFontSizes.Title,
            fontWeight = FontWeight.Bold,
          )
        },
      )
    },
    containerColor = BorealColors.Background,
  ) { innerPadding ->
    if (!isReady) {
      Box(
        modifier =
          Modifier
            .padding(innerPadding)
            .fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        LoadingIndicator()
      }
    } else {
      LazyColumn(
        contentPadding = PaddingValues(16.dp),
        modifier =
          Modifier
            .padding(innerPadding)
            .fillMaxSize(),
      ) {
        items(tiles) { tile ->
          BorealTile(
            title = tile.title,
            variant = tile.variant,
            icon = tile.icon,
            modifier = Modifier.padding(bottom = 16.dp),
          )
        }
      }
    }
  }
}
