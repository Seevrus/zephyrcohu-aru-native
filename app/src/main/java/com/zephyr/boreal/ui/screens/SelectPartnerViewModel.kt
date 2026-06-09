package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Partner
import com.zephyr.boreal.domain.model.canAddPartner
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class PartnerTab {
  ROUND_STORES,
  ALL_STORES,
}

data class SelectPartnerUiState(
  val isLoading: Boolean = true,
  val searchQuery: String = "",
  val selectedTab: PartnerTab = PartnerTab.ROUND_STORES,
  val partners: List<Partner> = emptyList(),
  val expandedPartnerIds: Set<Int> = emptySet(),
  val canAddPartner: Boolean = false,
  val selectedPartnerId: Int? = null,
)

@HiltViewModel
class SelectPartnerViewModel
  @Inject
  constructor(
    private val partnersRepository: PartnersRepository,
    private val userRepository: UserRepository,
  ) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val selectedTabFlow = MutableStateFlow(PartnerTab.ROUND_STORES)
    private val expandedPartnerIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    private val filtersFlow =
      combine(
        searchQueryFlow,
        selectedTabFlow,
        expandedPartnerIdsFlow,
      ) { query, tab, expanded ->
        Triple(query, tab, expanded)
      }

    val uiState: StateFlow<SelectPartnerUiState> =
      combine(
        filtersFlow,
        partnersRepository.getPartners(),
        partnersRepository.getPartnerLists(),
        userRepository.getCurrentUser(),
      ) { filters, partnersRes, partnerListsRes, userRes ->
        val (searchQuery, selectedTab, expandedIds) = filters
        val user = userRes.getOrNull()
        val canAddPartner = user?.canAddPartner == true
        val allPartners = partnersRes.getOrNull() ?: emptyList()

        val filteredPartners =
          when (selectedTab) {
            PartnerTab.ALL_STORES -> allPartners
            PartnerTab.ROUND_STORES -> {
              val currentRoundPartnerListId = user?.lastRound?.partnerList?.id
              val currentPartnerList =
                partnerListsRes.getOrNull()?.find { it.id == currentRoundPartnerListId }

              if (currentPartnerList != null) {
                allPartners.filter { currentPartnerList.partners.contains(it.id) }
              } else {
                emptyList()
              }
            }
          }.filter { partner ->
            if (searchQuery.isBlank()) return@filter true

            val needle = searchQuery.lowercase()
            val haystack =
              partner.locations
                .joinToString("") { loc ->
                  "${loc.postalCode}${loc.name}${loc.city}${loc.address}"
                }.lowercase()

            haystack.contains(needle)
          }

        // Treat the expanded partner as the selected partner
        val actualSelectedPartnerId: Int? = expandedIds.firstOrNull()

        val partnersWithSelectedPrepended =
          if (actualSelectedPartnerId != null) {
            val selectedPartner = allPartners.find { it.id == actualSelectedPartnerId }
            if (selectedPartner != null && !filteredPartners.contains(selectedPartner)) {
              listOf(selectedPartner) + filteredPartners
            } else {
              filteredPartners
            }
          } else {
            filteredPartners
          }

        SelectPartnerUiState(
          isLoading = partnersRes.isLoading || partnerListsRes.isLoading,
          searchQuery = searchQuery,
          selectedTab = selectedTab,
          partners = partnersWithSelectedPrepended,
          expandedPartnerIds = expandedIds,
          canAddPartner = canAddPartner,
          selectedPartnerId = actualSelectedPartnerId,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelectPartnerUiState(),
      )

    fun onSearchQueryChanged(query: String) {
      searchQueryFlow.value = query
    }

    fun onTabSelected(tab: PartnerTab) {
      selectedTabFlow.value = tab
    }

    fun onTogglePartnerExpanded(partnerId: Int) {
      expandedPartnerIdsFlow.update { current ->
        if (current.contains(partnerId)) {
          emptySet()
        } else {
          setOf(partnerId)
        }
      }
    }
  }
