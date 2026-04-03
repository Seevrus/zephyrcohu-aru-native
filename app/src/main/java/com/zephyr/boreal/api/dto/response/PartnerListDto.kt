package com.zephyr.boreal.api.dto.response

import kotlinx.serialization.Serializable

@Serializable
data class PartnersListResponseDto(
  val data: List<PartnerListResponseDataDto>,
)

@Serializable
data class PartnerListResponseDataDto(
  val id: Int,
  val name: String,
  val partners: List<Int>,
  val createdAt: String,
  val updatedAt: String,
)
