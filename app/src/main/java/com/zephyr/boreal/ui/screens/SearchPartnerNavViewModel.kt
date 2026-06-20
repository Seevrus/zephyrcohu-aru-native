package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.domain.model.TaxPayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class SearchPartnerNavUiState(
  val taxNumber: String = "",
  val isSearching: Boolean = false,
  val results: List<TaxPayer> = emptyList(),
  val error: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchPartnerNavViewModel
  @Inject
  constructor(
    private val partnersRepository: PartnersRepository,
  ) : ViewModel() {
    private val taxNumberFlow = MutableStateFlow("")

    val uiState: StateFlow<SearchPartnerNavUiState> =
      taxNumberFlow
        .flatMapLatest { digits ->
          if (digits.length == 8) {
            partnersRepository.searchTaxNumber(digits).map { resource ->
              when (resource) {
                is ApiResource.Loading ->
                  SearchPartnerNavUiState(taxNumber = digits, isSearching = true)
                is ApiResource.Success ->
                  SearchPartnerNavUiState(
                    taxNumber = digits,
                    isSearching = false,
                    results = resource.data,
                  )
                is ApiResource.Error ->
                  SearchPartnerNavUiState(
                    taxNumber = digits,
                    isSearching = false,
                    error = resource.message,
                  )
              }
            }
          } else {
            flowOf(SearchPartnerNavUiState(taxNumber = digits))
          }
        }.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = SearchPartnerNavUiState(),
        )

    fun onTaxNumberChanged(input: String) {
      val digits = input.filter { it.isDigit() }.take(8)
      taxNumberFlow.value = digits
    }

    fun onTaxPayerSelected(
      index: Int,
      onNavigate: (taxNumber: String, selectedIndex: Int) -> Unit,
    ) {
      val taxNumber = uiState.value.taxNumber
      onNavigate(taxNumber, index)
    }
  }
