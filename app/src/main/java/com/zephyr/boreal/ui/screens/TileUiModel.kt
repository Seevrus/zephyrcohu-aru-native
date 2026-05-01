package com.zephyr.boreal.ui.screens

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zephyr.boreal.ui.components.TileVariant

data class TileUiModel<T>(
  val id: T,
  @param:StringRes val titleResId: Int,
  val titleArg: Int? = null,
  val variant: TileVariant,
  @param:DrawableRes val iconResId: Int,
  @param:StringRes val disabledMessageResId: Int? = null,
)
