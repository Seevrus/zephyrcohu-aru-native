package com.zephyr.boreal.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TempSelection(
  val netPrice: Double? = null,
  val quantity: Int? = null,
  val comment: String? = null,
)
