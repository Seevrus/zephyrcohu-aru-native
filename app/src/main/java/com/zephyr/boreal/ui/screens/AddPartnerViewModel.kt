package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.domain.model.DraftReceipt
import com.zephyr.boreal.domain.model.InvoiceType
import com.zephyr.boreal.domain.model.ReceiptBuyer
import com.zephyr.boreal.domain.model.TaxPayer
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddPartnerField {
  TAX_NUMBER,
  NAME,
  CENTRAL_POSTAL_CODE,
  CENTRAL_CITY,
  CENTRAL_ADDRESS,
  DELIVERY_POSTAL_CODE,
  DELIVERY_CITY,
  DELIVERY_ADDRESS,
}

data class AddPartnerUiState(
  val taxNumber: String = "",
  val name: String = "",
  val centralPostalCode: String = "",
  val centralCity: String = "",
  val centralAddress: String = "",
  val deliveryPostalCode: String = "",
  val deliveryCity: String = "",
  val deliveryAddress: String = "",
  val taxNumberError: String? = null,
  val nameError: String? = null,
  val centralAddressError: String? = null,
  val deliveryPostalCodeError: String? = null,
  val deliveryCityError: String? = null,
  val deliveryAddressError: String? = null,
  val isSubmitting: Boolean = false,
)

private val TAX_NUMBER_REGEX = Regex("""^\d{8}-\d-\d{2}$""")

@HiltViewModel
class AddPartnerViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val partnersRepository: PartnersRepository,
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val taxNumberArg: String? = savedStateHandle["taxNumber"]
    private val selectedIndexArg: Int = savedStateHandle.get<Int>("selectedIndex") ?: -1

    private val _uiState = MutableStateFlow(AddPartnerUiState())
    val uiState: StateFlow<AddPartnerUiState> = _uiState.asStateFlow()

    init {
      if (taxNumberArg != null && selectedIndexArg >= 0) {
        preFillFromSearch(taxNumberArg, selectedIndexArg)
      }
    }

    private fun preFillFromSearch(
      taxNumber: String,
      index: Int,
    ) {
      viewModelScope.launch {
        val resource =
          partnersRepository.searchTaxNumber(taxNumber).first {
            it !is ApiResource.Loading
          }
        if (resource is ApiResource.Success) {
          val taxPayer = resource.data.getOrNull(index) ?: return@launch
          prefillFromTaxPayer(taxPayer)
        }
      }
    }

    private fun prefillFromTaxPayer(taxPayer: TaxPayer) {
      val central = taxPayer.locations["C"]
      val delivery = taxPayer.locations["D"]
      _uiState.update {
        it.copy(
          taxNumber = taxPayer.vatNumber,
          name = delivery?.name ?: central?.name ?: "",
          centralPostalCode = central?.postalCode ?: "",
          centralCity = central?.city ?: "",
          centralAddress = central?.address ?: "",
          deliveryPostalCode = delivery?.postalCode ?: "",
          deliveryCity = delivery?.city ?: "",
          deliveryAddress = delivery?.address ?: "",
        )
      }
    }

    fun onFieldChanged(
      field: AddPartnerField,
      value: String,
    ) {
      _uiState.update { state ->
        when (field) {
          AddPartnerField.TAX_NUMBER -> state.copy(taxNumber = value, taxNumberError = null)
          AddPartnerField.NAME -> state.copy(name = value, nameError = null)
          AddPartnerField.CENTRAL_POSTAL_CODE ->
            state.copy(centralPostalCode = value, centralAddressError = null)
          AddPartnerField.CENTRAL_CITY ->
            state.copy(centralCity = value, centralAddressError = null)
          AddPartnerField.CENTRAL_ADDRESS ->
            state.copy(centralAddress = value, centralAddressError = null)
          AddPartnerField.DELIVERY_POSTAL_CODE ->
            state.copy(deliveryPostalCode = value, deliveryPostalCodeError = null)
          AddPartnerField.DELIVERY_CITY ->
            state.copy(deliveryCity = value, deliveryCityError = null)
          AddPartnerField.DELIVERY_ADDRESS ->
            state.copy(deliveryAddress = value, deliveryAddressError = null)
        }
      }
    }

    fun onSubmit(onSuccess: () -> Unit) {
      val state = _uiState.value
      val errors = validateForm(state)

      if (errors.hasErrors) {
        _uiState.update {
          it.copy(
            taxNumberError = errors.taxNumberError,
            nameError = errors.nameError,
            centralAddressError = errors.centralAddressError,
            deliveryPostalCodeError = errors.deliveryPostalCodeError,
            deliveryCityError = errors.deliveryCityError,
            deliveryAddressError = errors.deliveryAddressError,
          )
        }
        return
      }

      val buyer =
        ReceiptBuyer(
          id = 0,
          name = state.name,
          country = "HU",
          postalCode = state.centralPostalCode.ifBlank { state.deliveryPostalCode },
          city = state.centralCity.ifBlank { state.deliveryCity },
          address = state.centralAddress.ifBlank { state.deliveryAddress },
          deliveryName = state.name,
          deliveryCountry = "HU",
          deliveryPostalCode = state.deliveryPostalCode,
          deliveryCity = state.deliveryCity,
          deliveryAddress = state.deliveryAddress,
          iban = null,
          bankAccount = null,
          vatNumber = state.taxNumber,
        )

      receiptsStore.setCurrentReceipt(
        DraftReceipt(
          partnerId = null,
          partnerCode = "",
          partnerSiteCode = "",
          buyer = buyer,
          paymentDays = 0,
          invoiceType = InvoiceType.PAPER,
        ),
      )
      onSuccess()
    }
  }

private data class ValidationErrors(
  val taxNumberError: String? = null,
  val nameError: String? = null,
  val centralAddressError: String? = null,
  val deliveryPostalCodeError: String? = null,
  val deliveryCityError: String? = null,
  val deliveryAddressError: String? = null,
) {
  val hasErrors: Boolean
    get() =
      listOfNotNull(
        taxNumberError,
        nameError,
        centralAddressError,
        deliveryPostalCodeError,
        deliveryCityError,
        deliveryAddressError,
      ).isNotEmpty()
}

private fun validateForm(state: AddPartnerUiState): ValidationErrors {
  val centralFields = listOf(state.centralPostalCode, state.centralCity, state.centralAddress)
  return ValidationErrors(
    taxNumberError =
      if (!TAX_NUMBER_REGEX.matches(state.taxNumber)) "Hibás adószám formátum (XXXXXXXX-X-XX)" else null,
    nameError = if (state.name.isBlank()) "A bolt neve kötelező" else null,
    deliveryPostalCodeError = if (state.deliveryPostalCode.isBlank()) "Irányítószám kötelező" else null,
    deliveryCityError = if (state.deliveryCity.isBlank()) "Város kötelező" else null,
    deliveryAddressError = if (state.deliveryAddress.isBlank()) "Cím kötelező" else null,
    centralAddressError =
      if (centralFields.any { it.isNotBlank() } && centralFields.any { it.isBlank() }) {
        "A székhely adatok hiányosak"
      } else {
        null
      },
  )
}
