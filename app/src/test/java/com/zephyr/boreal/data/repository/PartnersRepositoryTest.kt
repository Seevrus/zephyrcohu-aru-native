package com.zephyr.boreal.data.repository

import com.zephyr.boreal.api.PartnerApiService
import com.zephyr.boreal.data.local.PartnerEntity
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.local.dao.PartnerDao
import com.zephyr.boreal.data.local.dao.PartnerListDao
import com.zephyr.boreal.data.local.dao.TaxPayerDao
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.user.StoredToken
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.store.user.UserState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PartnersRepositoryTest {
  companion object {
    private const val NAME_EVA = "\u00C9va"
    private const val NAME_ESZTER = "Eszter"
    private const val NAME_ZEBRA = "Zebra"
    private const val NAME_ADAM = "\u00C1d\u00E1m"
    private const val NAME_ALADAR = "Alad\u00E1r"
  }

  private val apiService: PartnerApiService = mock()
  private val partnerDao: PartnerDao = mock()
  private val partnerListDao: PartnerListDao = mock()
  private val taxPayerDao: TaxPayerDao = mock()
  private val cacheMetadataDao: CacheMetadataDao = mock()
  private val connectivityObserver: ConnectivityObserver = mock()
  private val userSessionStore: UserSessionStore = mock()

  private lateinit var repository: PartnersRepository

  @BeforeEach
  fun setUp() {
    whenever(connectivityObserver.isInternetReachable).thenReturn(MutableStateFlow(true))
    whenever(userSessionStore.userState).thenReturn(
      MutableStateFlow(UserState(deviceId = "dev", storedToken = StoredToken("token", false, "2099"))),
    )

    repository =
      PartnersRepository(
        apiService,
        partnerDao,
        partnerListDao,
        taxPayerDao,
        cacheMetadataDao,
        connectivityObserver,
        userSessionStore,
      )
  }

  @Test
  fun `getPartners should sort partners by delivery location name using Hungarian Locale`() =
    runTest {
      whenever(cacheMetadataDao.getFetchedAt(any())).thenReturn(System.currentTimeMillis())

      // Input entities in unsorted order, testing Hungarian specific sorting rules.
      // Á comes after A, É comes after E.
      val entities =
        listOf(
          createPartnerEntity(1, NAME_EVA),
          createPartnerEntity(2, NAME_ESZTER),
          createPartnerEntity(3, NAME_ZEBRA),
          createPartnerEntity(4, NAME_ADAM),
          createPartnerEntity(5, NAME_ALADAR),
        )

      whenever(partnerDao.getAllPartners()).thenReturn(flowOf(entities))

      // Just need to collect from flow. Since networkBoundResource checks cache, we can just let it emit cache.
      val resultFlow = repository.getPartners()
      val result = resultFlow.dropWhile { it is ApiResource.Loading }.first()

      assertEquals("Success", result::class.simpleName)
      val partners = (result as ApiResource.Success).data

      // Expected sorting: Ádám, Aladár, Eszter, Éva, Zebra (due to Java Collator primary/secondary differences)
      assertEquals(5, partners.size)
      assertEquals(
        NAME_ADAM,
        partners
          .get(0)
          .locations
          .find { it.locationType == LocationType.DELIVERY }
          ?.name,
      )
      assertEquals(
        NAME_ALADAR,
        partners
          .get(1)
          .locations
          .find { it.locationType == LocationType.DELIVERY }
          ?.name,
      )
      assertEquals(
        NAME_ESZTER,
        partners
          .get(2)
          .locations
          .find { it.locationType == LocationType.DELIVERY }
          ?.name,
      )
      assertEquals(
        NAME_EVA,
        partners
          .get(3)
          .locations
          .find { it.locationType == LocationType.DELIVERY }
          ?.name,
      )
      assertEquals(
        NAME_ZEBRA,
        partners
          .get(4)
          .locations
          .find { it.locationType == LocationType.DELIVERY }
          ?.name,
      )
    }

  private fun createPartnerEntity(
    id: Int,
    deliveryName: String,
  ): PartnerEntity =
    PartnerEntity(
      id = id,
      code = "CODE",
      siteCode = "SITE",
      paymentDays = 0,
      vatNumber = "",
      invoiceType = com.zephyr.boreal.domain.model.InvoiceType.PAPER,
      invoiceCopies = 1,
      iban = null,
      bankAccount = null,
      phoneNumber = null,
      email = null,
      priceList = null,
      createdAt = "",
      updatedAt = "",
      locations =
        listOf(
          com.zephyr.boreal.api.dto.response.PartnerLocationDto(
            name = deliveryName,
            locationType = LocationType.DELIVERY,
            country = "HU",
            postalCode = "1234",
            city = "Budapest",
            address = "Address",
            createdAt = "",
            updatedAt = "",
          ),
        ),
    )
}
