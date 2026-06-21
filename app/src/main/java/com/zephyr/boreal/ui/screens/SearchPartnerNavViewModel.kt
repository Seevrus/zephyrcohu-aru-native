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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal const val TAX_NUMBER_DIGIT_COUNT = 8

data class SearchPartnerNavUiState(
  val taxNumber: String = "",
  val isSearching: Boolean = false,
  val results: List<TaxPayer> = emptyList(),
  val error: String? = null,
  val filterQuery: String = "",
  val selectedResultId: Int? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchPartnerNavViewModel
  @Inject
  constructor(
    private val partnersRepository: PartnersRepository,
  ) : ViewModel() {
    private val taxNumberFlow = MutableStateFlow("")
    private val filterQueryFlow = MutableStateFlow("")
    private val selectedResultIdFlow = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<SearchPartnerNavUiState> =
      taxNumberFlow
        .flatMapLatest { digits ->
          if (digits.length == TAX_NUMBER_DIGIT_COUNT) {
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
        }.combine(filterQueryFlow) { state, filter ->
          state.copy(filterQuery = filter)
        }.combine(selectedResultIdFlow) { state, selectedId ->
          state.copy(selectedResultId = selectedId)
        }.stateIn(
          scope = viewModelScope,
          started = SharingStarted.WhileSubscribed(5000),
          initialValue = SearchPartnerNavUiState(),
        )

    fun onTaxNumberChanged(input: String) {
      val digits = input.filter { it.isDigit() }.take(TAX_NUMBER_DIGIT_COUNT)
      filterQueryFlow.value = ""
      selectedResultIdFlow.value = null
      taxNumberFlow.value = digits
    }

    fun onFilterQueryChanged(query: String) {
      filterQueryFlow.value = query
    }

    fun onResultTapped(taxPayerId: Int) {
      val current = selectedResultIdFlow.value
      selectedResultIdFlow.value = if (current == taxPayerId) null else taxPayerId
    }

    fun onConfirmSelection(onNavigate: (taxNumber: String, selectedIndex: Int) -> Unit) {
      val state = uiState.value
      val selectedId = state.selectedResultId ?: return
      val index = state.results.indexOfFirst { it.id == selectedId }
      if (index >= 0) {
        onNavigate(state.taxNumber, index)
      }
    }
  }
