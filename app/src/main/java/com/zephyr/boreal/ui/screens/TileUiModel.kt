package com.zephyr.boreal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zephyr.boreal.ui.components.TileVariant

enum class TileId {
  STORAGE,
  SELL,
  ERRANDS,
  RECEIPTS,
}

data class TileUiModel(
  val id: TileId,
  @StringRes val titleResId: Int,
  val titleArg: Int? = null,
  val variant: TileVariant,
  @DrawableRes val iconResId: Int,
  @StringRes val disabledMessageResId: Int? = null,
)
