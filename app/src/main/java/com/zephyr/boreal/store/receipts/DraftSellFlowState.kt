package com.zephyr.boreal.store.receipts

import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.TempSelection
import kotlinx.serialization.Serializable

@Serializable
data class DraftSellFlowState(
  val currentReceipt: DraftReceipt? = null,
  val currentOrder: DraftOrder? = null,
  val selectedItems: Map<Int, Map<Int, Double>> = emptyMap(),
  val selectedOrderItems: Map<Int, Double> = emptyMap(),
  val otherItemSelections: Map<Int, TempSelection> = emptyMap(),
)
