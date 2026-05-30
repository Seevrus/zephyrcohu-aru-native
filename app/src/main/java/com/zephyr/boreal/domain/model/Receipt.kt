package com.zephyr.boreal.domain.model

data class Receipt(
  val id: Int,
  val companyId: Int,
  val companyCode: String,
  val partnerId: Int,
  val partnerCode: String,
  val partnerSiteCode: String,
  val serialNumber: Int,
  val yearCode: Int,
  val cancelSerialNumber: Int?,
  val cancelYearCode: Int?,
  val originalCopiesPrinted: Int,
  val vendor: ReceiptVendor,
  val buyer: ReceiptBuyer,
  val invoiceDate: String, // yyyy-MM-dd
  val fulfillmentDate: String, // yyyy-MM-dd
  val invoiceType: InvoiceType,
  val paidDate: String, // yyyy-MM-dd
  val user: ReceiptUser?,
  val items: List<ReceiptItem> = emptyList(),
  val otherItems: List<ReceiptOtherItem> = emptyList(),
  val quantity: Double,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
  val vatAmounts: List<ReceiptVatAmount> = emptyList(),
  val roundAmount: Double,
  val roundedAmount: Double,
  val lastDownloadedAt: String?, // UTC
  val createdAt: String,
  val updatedAt: String,
) {
  val paymentDays: Int
    get() {
      if (invoiceDate.isEmpty() || fulfillmentDate.isEmpty()) {
        return 0
      }

      val invoice = java.time.LocalDate.parse(invoiceDate)
      val fulfillment = java.time.LocalDate.parse(fulfillmentDate)
      return java.time.temporal.ChronoUnit.DAYS
        .between(invoice, fulfillment)
        .toInt()
    }
}

data class ReceiptUser(
  val id: Int,
  val code: String,
  val name: String,
  val phoneNumber: String,
)

data class ReceiptVendor(
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val felir: String,
  val iban: String,
  val bankAccount: String,
  val vatNumber: String,
)

data class ReceiptBuyer(
  val id: Int,
  val name: String,
  val country: String,
  val postalCode: String,
  val city: String,
  val address: String,
  val deliveryName: String,
  val deliveryCountry: String,
  val deliveryPostalCode: String,
  val deliveryCity: String,
  val deliveryAddress: String,
  val iban: String?,
  val bankAccount: String?,
  val vatNumber: String?,
)

data class ReceiptItem(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val quantity: Double,
  val unitName: String,
  val netPrice: Double,
  val netAmount: Double,
  val vatRate: String,
  val vatAmount: Double,
  val grossAmount: Double,
  val discountName: String?,
  val expirationId: Int,
  val cnCode: String,
  val expiresAt: String,
)

data class ReceiptOtherItem(
  val id: Int,
  val articleNumber: String,
  val name: String,
  val quantity: Double,
  val unitName: String,
  val netPrice: Double,
  val netAmount: Double,
  val vatRate: String,
  val vatAmount: Double,
  val grossAmount: Double,
  val comment: String?,
)

data class ReceiptVatAmount(
  val vatRate: String,
  val netAmount: Double,
  val vatAmount: Double,
  val grossAmount: Double,
)
