package com.zephyr.boreal.data.mapper

import com.zephyr.boreal.api.dto.response.AddressDto
import com.zephyr.boreal.api.dto.response.BasePriceListDto
import com.zephyr.boreal.api.dto.response.PartnerLocationDto
import com.zephyr.boreal.api.dto.response.PartnerResponseDataDto
import com.zephyr.boreal.api.dto.response.SearchTaxNumberResponseDto
import com.zephyr.boreal.data.local.PartnerEntity
import com.zephyr.boreal.domain.model.LocationType
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.PartnerLocation
import com.zephyr.boreal.domain.model.PartnerPriceList
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.domain.model.TaxpayerAddressType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun SearchTaxNumberResponseDto.toDomain(): List<TaxPayer> {
  val isoFormat =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
  val now = isoFormat.format(Date())
  val addressList = data.addressList ?: emptyList()

  val centralLocation = mapCentralLocation(data.name ?: "", addressList, now)
  val siteAddresses = addressList.filter { it.taxpayerAddressType == TaxpayerAddressType.SITE }

  return if (siteAddresses.isNotEmpty()) {
    mapSiteAddresses(data.name ?: "", data.taxNumber, siteAddresses, centralLocation, now)
  } else if (addressList.isNotEmpty()) {
    mapSingleAddress(data.name ?: "", data.taxNumber, addressList.first(), centralLocation, now)
  } else {
    emptyList()
  }
}

private fun mapCentralLocation(
  name: String,
  addressList: List<AddressDto>,
  now: String,
): PartnerLocation? {
  val hqAddress = addressList.find { it.taxpayerAddressType == TaxpayerAddressType.HEADQUARTERS }
  return hqAddress?.let {
    PartnerLocation(
      name = name,
      locationType = LocationType.CENTRAL,
      country = it.taxpayerAddress.countryCode,
      postalCode = it.taxpayerAddress.postalCode,
      city = it.taxpayerAddress.city,
      address =
        listOf(
          it.taxpayerAddress.streetName,
          it.taxpayerAddress.publicPlaceCategory,
          it.taxpayerAddress.number,
        ).filter { s -> s.isNotEmpty() }.joinToString(" "),
      createdAt = now,
      updatedAt = now,
    )
  }
}

private fun mapSiteAddresses(
  name: String,
  taxNumber: String,
  siteAddresses: List<AddressDto>,
  centralLocation: PartnerLocation?,
  now: String,
): List<TaxPayer> =
  siteAddresses.mapIndexed { index, site ->
    val deliveryLocation =
      PartnerLocation(
        name = name,
        locationType = LocationType.DELIVERY,
        country = site.taxpayerAddress.countryCode,
        postalCode = site.taxpayerAddress.postalCode,
        city = site.taxpayerAddress.city,
        address =
          listOf(
            site.taxpayerAddress.streetName,
            site.taxpayerAddress.publicPlaceCategory,
            site.taxpayerAddress.number,
          ).filter { s -> s.isNotEmpty() }.joinToString(" "),
        createdAt = now,
        updatedAt = now,
      )

    val locations = mutableMapOf<String, PartnerLocation>()
    centralLocation?.let { locations["C"] = it }
    locations["D"] = deliveryLocation

    TaxPayer(
      id = index + 1,
      vatNumber = taxNumber,
      locations = locations,
    )
  }

private fun mapSingleAddress(
  name: String,
  taxNumber: String,
  address: AddressDto,
  centralLocation: PartnerLocation?,
  now: String,
): List<TaxPayer> {
  val deliveryLocation =
    PartnerLocation(
      name = name,
      locationType = LocationType.DELIVERY,
      country = address.taxpayerAddress.countryCode,
      postalCode = address.taxpayerAddress.postalCode,
      city = address.taxpayerAddress.city,
      address =
        listOf(
          address.taxpayerAddress.streetName,
          address.taxpayerAddress.publicPlaceCategory,
          address.taxpayerAddress.number,
        ).filter { s -> s.isNotEmpty() }.joinToString(" "),
      createdAt = now,
      updatedAt = now,
    )
  val locations = mutableMapOf<String, PartnerLocation>()
  centralLocation?.let { locations["C"] = it }
  locations["D"] = deliveryLocation

  return listOf(
    TaxPayer(
      id = 1,
      vatNumber = taxNumber,
      locations = locations,
    ),
  )
}

fun PartnerResponseDataDto.toEntity(): PartnerEntity =
  PartnerEntity(
    id = id,
    code = code,
    siteCode = siteCode,
    vatNumber = vatNumber,
    invoiceType = invoiceType,
    invoiceCopies = invoiceCopies,
    paymentDays = paymentDays,
    iban = iban,
    bankAccount = bankAccount,
    phoneNumber = phoneNumber,
    email = email,
    locations = locations,
    priceList = priceList,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )

fun PartnerEntity.toDomain(): Partner {
  val domainLocations = locations.map { it.toDomain() }
  val partnerName =
    domainLocations.find { it.locationType == LocationType.DELIVERY }?.name
      ?: domainLocations.find { it.locationType == LocationType.CENTRAL }?.name
      ?: domainLocations.firstOrNull()?.name
      ?: ""

  return Partner(
    id = id,
    code = code,
    siteCode = siteCode,
    name = partnerName,
    vatNumber = vatNumber,
    invoiceType = invoiceType,
    invoiceCopies = invoiceCopies,
    paymentDays = paymentDays,
    iban = iban,
    bankAccount = bankAccount,
    phoneNumber = phoneNumber,
    email = email,
    locations = domainLocations,
    priceList = priceList?.toDomain(),
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}

fun BasePriceListDto.toDomain(): PartnerPriceList =
  PartnerPriceList(
    id = id,
    code = code,
    name = name,
  )

fun PartnerLocationDto.toDomain(): PartnerLocation =
  PartnerLocation(
    name = name,
    locationType = locationType,
    country = country,
    postalCode = postalCode,
    city = city,
    address = address,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
