package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.PartnerApiService
import com.zephyr.boreal.api.dto.request.SearchTaxNumberDataDto
import com.zephyr.boreal.api.dto.request.SearchTaxNumberRequestDto
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.local.dao.PartnerListDao
import com.zephyr.boreal.data.local.dao.TaxPayerDao
import com.zephyr.boreal.data.mapper.toDomain
import com.zephyr.boreal.data.mapper.toEntity
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerList
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.UserSessionStore
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
    private val partnerListDao: PartnerListDao,
    private val taxPayerDao: TaxPayerDao,
    cacheMetadataDao: CacheMetadataDao,
    connectivityObserver: ConnectivityObserver,
    userSessionStore: UserSessionStore,
  ) : BaseRepository(connectivityObserver, userSessionStore, cacheMetadataDao) {
    fun getPartners(): Flow<ApiResource<List<Partner>>> =
      networkBoundResource(
        query = {
          partnerDao.getAllPartners().map { entities ->
            val collator =
              Collator.getInstance(
                Locale
                  .Builder()
                  .setLanguage("hu")
                  .setRegion("HU")
                  .build(),
              )
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
        queryKey = "get_partners",
      )

    fun getPartnerLists(forceRefresh: Boolean = false): Flow<ApiResource<List<PartnerList>>> =
      networkBoundResource(
        query = {
          partnerListDao.getAllPartnerLists().map { entities ->
            entities.map { it.toDomain() }
          }
        },
        fetch = {
          apiService.getPartnerLists().data
        },
        saveFetchResult = { dtos ->
          partnerListDao.insertPartnerLists(dtos.map { it.toEntity() })
        },
        queryKey = "get_partner_lists",
        cacheTimeoutMillis = if (forceRefresh) 0L else DEFAULT_CACHE_TIMEOUT_MS,
      )

    fun searchTaxNumber(
      taxNumber: String,
      forceRefresh: Boolean = false,
    ): Flow<ApiResource<List<TaxPayer>>> =
      networkBoundResource(
        query = {
          taxPayerDao.getTaxPayersByVatNumber(taxNumber).map { entities ->
            entities.map { it.toDomain() }
          }
        },
        fetch = {
          apiService.searchTaxNumber(SearchTaxNumberRequestDto(SearchTaxNumberDataDto(taxNumber)))
        },
        saveFetchResult = { responseDto ->
          val taxpayers = responseDto.toDomain()
          taxPayerDao.insertTaxPayers(taxpayers.map { it.toEntity() })
        },
        queryKey = "search_tax_number_$taxNumber",
        cacheTimeoutMillis = if (forceRefresh) 0L else TAX_NUMBER_CACHE_TIMEOUT_MS,
      )

    companion object {
      private const val TAX_NUMBER_CACHE_TIMEOUT_MS = 86_400_000L // 1 day
    }
  }
