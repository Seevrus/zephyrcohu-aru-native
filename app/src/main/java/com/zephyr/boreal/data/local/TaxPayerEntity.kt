package com.zephyr.boreal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zephyr.boreal.domain.model.PartnerLocation

@Entity(tableName = "tax_payers")
data class TaxPayerEntity(
  @PrimaryKey val id: Int,
  val vatNumber: String,
  val locations: Map<String, PartnerLocation>,
)
