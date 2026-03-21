package com.zephyr.boreal.ui.screens

import androidx.compose.ui.graphics.vector.ImageVector
import com.zephyr.boreal.ui.components.TileVariant

data class TileData(
  val title: String,
  val variant: TileVariant,
  val icon: ImageVector,
)
