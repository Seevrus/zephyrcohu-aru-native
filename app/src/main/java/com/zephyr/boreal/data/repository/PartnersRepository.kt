package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.PartnerApiService
import com.zephyr.boreal.api.dto.request.SearchTaxNumberDataDto
import com.zephyr.boreal.api.dto.request.SearchTaxNumberRequestDto
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.TaxPayer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartnersRepository
  @Inject
  constructor(
    private val apiService: PartnerApiService,
    private val partnerDao: PartnerDao,
  ) : BaseRepository() {
    fun getPartners(): Flow<ApiResource<List<Partner>>> =
      networkBoundResource(
        query = {
          partnerDao.getAllPartners().map { entities ->
            val collator = Collator.getInstance(Locale("hu", "HU"))
            entities
              .map { it.toDomain() }
              .sortedWith { a, b ->
                val nameA = a.locations.find { it.locationType == LocationType.DELIVERY }?.name ?: ""
                val nameB = b.locations.find { it.locationType == LocationType.DELIVERY }?.name ?: ""
                collator.compare(nameA, nameB)
              }
          }
        },
        fetch = {
          apiService.getPartners().data
        },
        saveFetchResult = { dtos ->
          partnerDao.insertPartners(dtos.map { it.toEntity() })
        },
      )

    suspend fun getPartnerLists(): ApiResource<List<PartnerList>> =
      try {
        val response = apiService.getPartnerLists()
        ApiResource.Success(response.data.map { it.toDomain() })
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to fetch partner lists")
      }

    suspend fun searchTaxNumber(taxNumber: String): ApiResource<List<TaxPayer>> =
      try {
        val response = apiService.searchTaxNumber(SearchTaxNumberRequestDto(SearchTaxNumberDataDto(taxNumber)))
        ApiResource.Success(response.toDomain())
      } catch (e: Exception) {
        ApiResource.Error(e.localizedMessage ?: "Failed to search tax number")
      }
  }
