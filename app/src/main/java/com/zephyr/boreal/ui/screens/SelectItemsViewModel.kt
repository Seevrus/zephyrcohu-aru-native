package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.Discount
import com.zephyr.boreal.domain.model.DraftOrder
import com.zephyr.boreal.domain.model.DraftReceiptItem
import com.zephyr.boreal.domain.model.OrderItem
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.domain.utils.AmountCalculator
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

private val ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

data class SellExpiration(
  val itemId: Int,
  val expirationId: Int,
  val expiresAt: String,
  val quantity: Double,
)

data class SellItem(
  val id: Int,
  val name: String,
  val articleNumber: String,
  val unitName: String,
  val barcodes: List<String>,
  val netPrice: Double,
  val vatRate: String,
  val cnCode: String,
  val expirations: List<SellExpiration>,
  val discounts: List<Discount> = emptyList(),
)

data class SelectItemsUiState(
  val isLoading: Boolean = true,
  val searchQuery: String = "",
  val barcodeQuery: String = "",
  val items: List<SellItem> = emptyList(),
  val allItems: List<SellItem> = emptyList(),
  val selectedItems: Map<Int, Map<Int, Double>> = emptyMap(),
  val selectedOrderItems: Map<Int, Double> = emptyMap(),
  val netTotal: Double = 0.0,
  val grossTotal: Double = 0.0,
  val totalQuantity: Int = 0,
  val netOrderTotal: Double = 0.0,
  val grossOrderTotal: Double = 0.0,
  val totalOrderQuantity: Int = 0,
  val canConfirmItems: Boolean = false,
  val expandedItemIds: Set<Int> = emptySet(),
)

@HiltViewModel
class SelectItemsViewModel
  @Inject
  constructor(
    private val itemsRepository: ItemsRepository,
    private val storeSessionStore: StoreSessionStore,
    private val receiptsStore: ReceiptsStore,
    private val userRepository: UserRepository,
    private val storesRepository: StoresRepository,
  ) : ViewModel() {
    private val searchQueryFlow = MutableStateFlow("")
    private val barcodeQueryFlow = MutableStateFlow("")
    private val expandedItemIdsFlow = MutableStateFlow<Set<Int>>(emptySet())

    init {
      viewModelScope.launch {
        userRepository.getCurrentUser().collect { resource ->
          val user = resource.getOrNull()
          if (user?.state == UserState.ON_ROUND && storeSessionStore.selectedStoreCurrentState.value == null) {
            val storeId = user.lastRound?.store?.id
            if (storeId != null) {
              val detailsRes = storesRepository.getStoreDetails(storeId)
              if (detailsRes is ApiResource.Success && detailsRes.data != null) {
                storeSessionStore.setStore(detailsRes.data)
              }
            }
          }
        }
      }
    }

    private val filtersFlow =
      combine(
        searchQueryFlow,
        barcodeQueryFlow,
        expandedItemIdsFlow,
      ) { search, barcode, expandedIds ->
        Triple(search, barcode, expandedIds)
      }

    private val receiptStateFlow =
      combine(
        receiptsStore.selectedItems,
        receiptsStore.selectedOrderItems,
        receiptsStore.currentReceipt,
      ) { selected, ordered, currentReceipt ->
        Triple(selected, ordered, currentReceipt)
      }

    val uiState: StateFlow<SelectItemsUiState> =
      combine(
        filtersFlow,
        itemsRepository.getItems(),
        storeSessionStore.selectedStoreCurrentState,
        receiptStateFlow,
      ) { filters, itemsRes, storeState, receiptState ->
        val search = filters.first
        val barcode = filters.second
        val expandedIds = filters.third
        val selected = receiptState.first
        val ordered = receiptState.second
        val currentReceipt = receiptState.third

        val rawItems = itemsRes.getOrNull() ?: emptyList()
        val inventory = storeState?.expirations ?: emptyList()

        val allSellItems =
          rawItems.map { item ->
            val sellExpirations =
              item.expirations
                .map { exp ->
                  val expDateDigits = exp.expiresAt.filter { it.isDigit() }
                  val storeExp =
                    inventory.find { it.itemId == item.id && it.expirationId == exp.id }
                      ?: inventory.find {
                        (it.articleNumber == item.articleNumber) &&
                          (
                            it.expirationId == exp.id ||
                              it.expiresAt.filter { c -> c.isDigit() }.endsWith(expDateDigits) ||
                              expDateDigits.endsWith(it.expiresAt.filter { c -> c.isDigit() })
                          )
                      }
                      ?: inventory.find {
                        (
                          it.articleNumber.contains(item.articleNumber) ||
                            item.articleNumber.contains(it.articleNumber)
                        ) &&
                          (
                            it.expiresAt.filter { c -> c.isDigit() }.endsWith(expDateDigits) ||
                              expDateDigits.endsWith(it.expiresAt.filter { c -> c.isDigit() })
                          )
                      }

                  SellExpiration(
                    itemId = item.id,
                    expirationId = exp.id,
                    expiresAt = exp.expiresAt,
                    quantity = storeExp?.quantity ?: 0.0,
                  )
                }

            SellItem(
              id = item.id,
              name = item.name,
              articleNumber = item.articleNumber,
              unitName = item.unitName,
              barcodes =
                if (item.expirations.isEmpty()) {
                  listOfNotNull(item.barcode).filter { it.isNotBlank() }
                } else {
                  item.expirations
                    .mapNotNull { "${item.barcode ?: ""}${it.barcode ?: ""}" }
                    .filter { it.isNotBlank() }
                },
              netPrice = item.netPrice,
              vatRate = item.vatRate,
              cnCode = item.cnCode,
              expirations = sellExpirations,
              discounts = item.discounts,
            )
          }

        val sellItems =
          allSellItems
            .filter {
              val nameMatches = it.name.contains(search, ignoreCase = true)
              val barcodeMatches = barcode.isBlank() || it.barcodes.any { bc -> bc.contains(barcode) }
              nameMatches && barcodeMatches
            }.sortedBy { it.name }

        var netTotal = 0.0
        var grossTotal = 0.0
        var totalQuantity = 0

        selected.forEach { (itemId, expMap) ->
          val item = rawItems.find { it.id == itemId }
          if (item != null) {
            expMap.forEach { (_, qty) ->
              val amounts =
                AmountCalculator.calculateAmounts(
                  item.netPrice,
                  qty,
                  item.vatRate,
                )
              netTotal += amounts.netAmount
              grossTotal += amounts.grossAmount
              totalQuantity += qty.toInt()
            }
          }
        }

        var netOrderTotal = 0.0
        var grossOrderTotal = 0.0
        var totalOrderQuantity = 0

        ordered.forEach { (itemId, qty) ->
          val item = rawItems.find { it.id == itemId }
          if (item != null) {
            val amounts =
              AmountCalculator.calculateAmounts(
                item.netPrice,
                qty,
                item.vatRate,
              )
            netOrderTotal += amounts.netAmount
            grossOrderTotal += amounts.grossAmount
            totalOrderQuantity += qty.toInt()
          }
        }

        val canConfirmItems = currentReceipt?.partnerId != null && selected.isNotEmpty()

        SelectItemsUiState(
          isLoading = itemsRes.isLoading || (storeState == null),
          searchQuery = search,
          barcodeQuery = barcode,
          items = sellItems,
          allItems = allSellItems,
          selectedItems = selected,
          selectedOrderItems = ordered,
          netTotal = netTotal,
          grossTotal = grossTotal,
          totalQuantity = totalQuantity,
          netOrderTotal = netOrderTotal,
          grossOrderTotal = grossOrderTotal,
          totalOrderQuantity = totalOrderQuantity,
          canConfirmItems = canConfirmItems,
          expandedItemIds = expandedIds,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelectItemsUiState(),
      )

    fun onSearchQueryChanged(query: String) {
      searchQueryFlow.value = query
      barcodeQueryFlow.value = "" // clear barcode when typing search
    }

    fun onBarcodeQueryChanged(barcode: String) {
      barcodeQueryFlow.value = barcode
      searchQueryFlow.value = "" // clear search when scanning
    }

    fun onToggleItemExpanded(itemId: Int) {
      expandedItemIdsFlow.update { current ->
        if (current.contains(itemId)) {
          current - itemId
        } else {
          current + itemId
        }
      }
    }

    fun upsertSelectedItem(
      itemId: Int,
      expirationId: Int,
      quantity: Double?,
    ) {
      receiptsStore.upsertSelectedItem(itemId, expirationId, quantity)
    }

    fun upsertOrderItem(
      itemId: Int,
      quantity: Double?,
    ) {
      receiptsStore.upsertOrderItem(itemId, quantity)
    }

    fun confirmItemsHandler(onSuccess: () -> Unit) {
      if (!uiState.value.canConfirmItems) return

      val allItems = uiState.value.allItems

      val ordered = receiptsStore.selectedOrderItems.value
      val orderItems =
        ordered.mapNotNull { (itemId, qty) ->
          val item = allItems.find { it.id == itemId } ?: return@mapNotNull null
          OrderItem(articleNumber = item.articleNumber, name = item.name, quantity = qty)
        }
      receiptsStore.setCurrentOrder(
        DraftOrder(
          partnerId = requireNotNull(receiptsStore.currentReceipt.value?.partnerId),
          orderedAt = LocalDateTime.now().format(ORDER_DATE_FORMAT),
          items = orderItems,
        ),
      )

      receiptsStore.updateCurrentReceipt { draft ->
        val selected = receiptsStore.selectedItems.value
        val itemsList = mutableListOf<DraftReceiptItem>()

        selected.forEach { (itemId, expMap) ->
          val item = allItems.find { it.id == itemId }
          if (item != null) {
            expMap.forEach { (expirationId, qty) ->
              val amounts = AmountCalculator.calculateAmounts(item.netPrice, qty, item.vatRate)
              val expiration = item.expirations.find { it.expirationId == expirationId }
              val expiresAtStr = expiration?.expiresAt?.take(6) ?: ""

              itemsList.add(
                DraftReceiptItem(
                  id = item.id,
                  articleNumber = item.articleNumber,
                  name = item.name,
                  quantity = qty,
                  unitName = item.unitName,
                  netPrice = item.netPrice,
                  netAmount = amounts.netAmount,
                  vatRate = item.vatRate,
                  vatAmount = amounts.vatAmount,
                  grossAmount = amounts.grossAmount,
                  discountName = null,
                  expirationId = expirationId,
                  cnCode = item.cnCode,
                  expiresAt = expiresAtStr,
                  availableDiscounts = item.discounts,
                ),
              )
            }
          }
        }

        draft.copy(items = itemsList)
      }

      onSuccess()
    }

    fun resetReceipts() {
      viewModelScope.launch {
        receiptsStore.resetReceipts()
      }
    }
  }
