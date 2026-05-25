package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.PartnerListResponseDataDto
import com.zephyr.boreal.data.local.PartnerListEntity
import com.zephyr.boreal.domain.model.PartnerList

fun PartnerListResponseDataDto.toDomain(): PartnerList =
  PartnerList(
    id = id,
    name = name,
    partners = partners,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun PartnerListResponseDataDto.toEntity(): PartnerListEntity =
  PartnerListEntity(
    id = id,
    name = name,
    partners = partners,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun PartnerListEntity.toDomain(): PartnerList =
  PartnerList(
    id = id,
    name = name,
    partners = partners,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
