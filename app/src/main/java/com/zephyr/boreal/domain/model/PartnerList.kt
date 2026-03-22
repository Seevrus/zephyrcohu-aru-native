package com.zephyr.boreal.domain.model

data class PartnerList(
  val id: Int,
  val name: String,
  val partners: List<Int>,
  val createdAt: String,
  val updatedAt: String,
)
