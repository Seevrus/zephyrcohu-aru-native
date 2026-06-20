package com.zephyr.boreal.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.store.receipts.ReceiptsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class AddPartnerViewModel
  @Inject
  constructor(
    savedStateHandle: SavedStateHandle,
    private val partnersRepository: PartnersRepository,
    private val receiptsStore: ReceiptsStore,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(AddPartnerUiState())
    val uiState: StateFlow<AddPartnerUiState> = _uiState.asStateFlow()

    fun onFieldChanged(
      field: AddPartnerField,
      value: String,
    ) {
    }

    fun onSubmit(onSuccess: () -> Unit) {
    }
  }
