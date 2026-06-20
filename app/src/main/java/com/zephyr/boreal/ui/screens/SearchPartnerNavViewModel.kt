package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.domain.model.TaxPayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class SearchPartnerNavUiState(
  val taxNumber: String = "",
  val isSearching: Boolean = false,
  val results: List<TaxPayer> = emptyList(),
  val error: String? = null,
)

@HiltViewModel
class SearchPartnerNavViewModel
  @Inject
  constructor(
    private val partnersRepository: PartnersRepository,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchPartnerNavUiState())
    val uiState: StateFlow<SearchPartnerNavUiState> = _uiState.asStateFlow()

    fun onTaxNumberChanged(input: String) {
    }

    fun onTaxPayerSelected(
      index: Int,
      onNavigate: (taxNumber: String, selectedIndex: Int) -> Unit,
    ) {
    }
  }
