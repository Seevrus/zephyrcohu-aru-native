package com.zephyr.boreal.ui.screens

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.R
import com.zephyr.boreal.data.local.dao.CacheMetadataDao
import com.zephyr.boreal.data.repository.ApiResource
import com.zephyr.boreal.data.repository.ItemsRepository
import com.zephyr.boreal.data.repository.PartnersRepository
import com.zephyr.boreal.data.repository.StoresRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.domain.model.canLoadAnyStore
import com.zephyr.boreal.domain.model.canLoadStore
import com.zephyr.boreal.domain.model.canUseApp
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StorageSessionStore
import com.zephyr.boreal.store.store.StoreSessionStore
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.ui.components.TileVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainScreenEvent {
  data object NavigateToErrands : MainScreenEvent

  data object NavigateToSelectPartner : MainScreenEvent

  data object NavigateToReceiptsList : MainScreenEvent

  data object NavigateToSelectStore : MainScreenEvent

  data object NavigateToSelectItemsFromStore : MainScreenEvent

  data object NavigateToStorageSummary : MainScreenEvent
}

data class AlertUiState(
  @param:StringRes val titleResId: Int,
  @param:StringRes val messageResId: Int? = null,
  @param:StringRes val confirmTextResId: Int? = null,
  @param:StringRes val cancelButtonTextResId: Int? = null,
  val onConfirm: () -> Unit = {},
  val onCancel: () -> Unit = {},
  val onDismiss: () -> Unit = {},
)

enum class MainTileId {
  STORAGE,
  SELL,
  ERRANDS,
  RECEIPTS,
}

data class RoundInfoUiModel(
  val partnerListName: String,
  val storeName: String,
  val roundStartedDate: String,
)

data class MainScreenUiState(
  val isReady: Boolean = false,
  val isLoggedIn: Boolean = false,
  val canUseApp: Boolean? = null,
  val userName: String? = null,
  val isInternetReachable: Boolean = true,
  val isPasswordExpired: Boolean = false,
  val tiles: List<TileUiModel<MainTileId>> = emptyList(),
  val alertState: AlertUiState? = null,
  val roundInfo: RoundInfoUiModel? = null,
)

@HiltViewModel
@Suppress("LongParameterList")
class MainViewModel
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    private val cacheMetadataDao: CacheMetadataDao,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val printSettingsStore: PrintSettingsStore,
    private val receiptsStore: ReceiptsStore,
    private val storageSessionStore: StorageSessionStore,
    private val storeSessionStore: StoreSessionStore,
    private val itemsRepository: ItemsRepository,
    private val partnersRepository: PartnersRepository,
    private val storesRepository: StoresRepository,
  ) : ViewModel() {
    companion object {
      const val FONT_WARMUP_DELAY_MS = 1000L
      const val GC_THRESHOLD_MS = 86400000L
    }

    private val isReadyFlow = MutableStateFlow(false)
    private val alertStateFlow = MutableStateFlow<AlertUiState?>(null)
    private val eventChannel = Channel<MainScreenEvent>()
    val events = eventChannel.receiveAsFlow()

    private data class DeviceState(
      val isOnline: Boolean,
      val hasPrinter: Boolean,
    )

    private data class UserContext(
      val isLoggedIn: Boolean,
      val isPasswordExpired: Boolean,
      val user: User?,
    )

    private val deviceStateFlow =
      combine(
        connectivityObserver.isInternetReachable,
        printSettingsStore.printSettingsState,
      ) { isOnline, printSettings ->
        DeviceState(
          isOnline = isOnline,
          hasPrinter = printSettings.selectedPrinterMacAddress != null,
        )
      }.distinctUntilChanged()

    private val userContextFlow =
      combine(
        userSessionStore.userState,
        userRepository.getCurrentUser(),
      ) { session, userResource ->
        UserContext(
          isLoggedIn = session.storedToken?.token != null,
          isPasswordExpired = session.storedToken?.isPasswordExpired == true,
          user = userResource.getOrNull(),
        )
      }.distinctUntilChanged()

    private val tilesFlow =
      combine(
        userContextFlow,
        deviceStateFlow,
        receiptsStore.receipts,
      ) { userCtx, deviceCtx, receipts ->
        val isRoundInProgress = userCtx.user?.state == UserState.ON_ROUND
        buildTiles(
          isLoggedIn = userCtx.isLoggedIn,
          isPasswordExpired = userCtx.isPasswordExpired,
          isOnline = deviceCtx.isOnline,
          hasPrinter = deviceCtx.hasPrinter,
          user = userCtx.user,
          receiptsCount = if (isRoundInProgress) receipts.size else 0,
        )
      }.distinctUntilChanged()

    val uiState: StateFlow<MainScreenUiState> =
      combine(
        isReadyFlow,
        userContextFlow,
        deviceStateFlow,
        tilesFlow,
        alertStateFlow,
      ) { isReady, userCtx, deviceCtx, tiles, alert ->
        val roundInfo =
          userCtx.user?.lastRound?.let { round ->
            if (round.roundFinished == null) {
              val dateStr =
                try {
                  java.time.Instant
                    .parse(round.roundStarted)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .toString()
                } catch (
                  @Suppress("SwallowedException") e: Exception,
                ) {
                  round.roundStarted.substringBefore("T")
                }
              RoundInfoUiModel(
                partnerListName = round.partnerList.name,
                storeName = round.store.name,
                roundStartedDate = dateStr,
              )
            } else {
              null
            }
          }

        MainScreenUiState(
          isReady = isReady,
          isLoggedIn = userCtx.isLoggedIn,
          canUseApp = userCtx.user?.canUseApp,
          userName = userCtx.user?.userName,
          isInternetReachable = deviceCtx.isOnline,
          isPasswordExpired = userCtx.isPasswordExpired,
          alertState = alert,
          tiles = tiles,
          roundInfo = roundInfo,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenUiState(),
      )

    init {
      viewModelScope.launch {
        performGarbageCollection()
        delay(FONT_WARMUP_DELAY_MS)
        isReadyFlow.value = true
      }
      viewModelScope.launch { consumePendingResetAfterPrint() }
      viewModelScope.launch {
        combine(userContextFlow, deviceStateFlow) { userCtx, deviceCtx -> userCtx to deviceCtx }
          .distinctUntilChanged()
          .collect { (userCtx, deviceCtx) ->
            refreshStateDrivenData(userCtx, deviceCtx)
          }
      }
    }

    private suspend fun refreshStateDrivenData(
      userCtx: UserContext,
      deviceCtx: DeviceState,
    ) {
      if (!userCtx.isLoggedIn || userCtx.isPasswordExpired || !deviceCtx.isOnline) {
        return
      }

      when (userCtx.user?.state) {
        UserState.LOADING -> {
          itemsRepository.getItems().first()
          storesRepository.getStores().first()
        }
        UserState.ON_ROUND -> {
          itemsRepository.getItems().first()
          itemsRepository.getOtherItems().first()
          partnersRepository.getPartnerLists().first()
          partnersRepository.getPartners().first()
          itemsRepository.getPriceLists().first()
          ensureStoreSessionPopulated(userCtx.user.storeInUseId)
        }
        else -> {}
      }
    }

    private suspend fun ensureStoreSessionPopulated(storeInUseId: Int?) {
      if (storeInUseId == null || storeSessionStore.selectedStoreCurrentState.value != null) {
        return
      }

      val result = storesRepository.getStoreDetails(storeInUseId)
      if (result is ApiResource.Success) {
        storeSessionStore.setStore(result.data)
      }
    }

    private suspend fun consumePendingResetAfterPrint() {
      if (!userSessionStore.isPendingResetAfterPrint.first()) {
        return
      }

      receiptsStore.resetReceipts()
      storeSessionStore.resetSelectedStoreCurrentState()
      userSessionStore.setPendingResetAfterPrint(false)

      val storedToken = userSessionStore.userState.first().storedToken
      if (storedToken != null && !storedToken.isTokenExpired) {
        userRepository.logout()
      }
    }

    private fun buildTiles(
      isLoggedIn: Boolean,
      isPasswordExpired: Boolean,
      isOnline: Boolean,
      hasPrinter: Boolean,
      user: User?,
      receiptsCount: Int,
    ): List<TileUiModel<MainTileId>> =
      listOf(
        createStorageTile(isLoggedIn, isPasswordExpired, isOnline, hasPrinter, user),
        createSellTile(isLoggedIn, hasPrinter, user),
        createErrandsTile(isLoggedIn, isPasswordExpired, isOnline, hasPrinter),
        createReceiptsTile(isLoggedIn, hasPrinter, receiptsCount),
      )

    private fun createStorageTile(
      isLoggedIn: Boolean,
      isPasswordExpired: Boolean,
      isOnline: Boolean,
      hasPrinter: Boolean,
      user: User?,
    ): TileUiModel<MainTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      when {
        !isLoggedIn -> messageResId = R.string.disabled_tile_logged_out
        isPasswordExpired -> messageResId = R.string.change_password_expired_warning
        user != null && !user.canLoadAnyStore && !user.canLoadStore -> messageResId = R.string.disabled_tile_no_role
        !isOnline -> messageResId = R.string.disabled_tile_offline
        !hasPrinter -> messageResId = R.string.disabled_tile_no_printer
        user != null -> {
          when (user.state) {
            UserState.IDLE -> state = TileVariant.OK
            UserState.LOADING -> state = TileVariant.NEUTRAL
            UserState.ON_ROUND -> messageResId = R.string.disabled_tile_storage_during_round
          }
        }
      }

      return TileUiModel(
        id = MainTileId.STORAGE,
        titleResId = R.string.tile_loading,
        variant = state,
        iconResId = R.drawable.truck_solid_full,
        disabledMessageResId = messageResId,
      )
    }

    private fun createSellTile(
      isLoggedIn: Boolean,
      hasPrinter: Boolean,
      user: User?,
    ): TileUiModel<MainTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      when {
        !isLoggedIn -> messageResId = R.string.disabled_tile_logged_out
        !hasPrinter -> messageResId = R.string.disabled_tile_no_printer
        user != null -> {
          if (user.state == UserState.ON_ROUND) {
            state = TileVariant.NEUTRAL
          } else {
            messageResId = R.string.disabled_tile_sell_before_round
          }
        }
      }

      return TileUiModel(
        id = MainTileId.SELL,
        titleResId = R.string.tile_unloading,
        variant = state,
        iconResId = R.drawable.cart_arrow_down_solid_full,
        disabledMessageResId = messageResId,
      )
    }

    private fun createErrandsTile(
      isLoggedIn: Boolean,
      isPasswordExpired: Boolean,
      isOnline: Boolean,
      hasPrinter: Boolean,
    ): TileUiModel<MainTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      when {
        !isLoggedIn -> messageResId = R.string.disabled_tile_logged_out
        isPasswordExpired -> messageResId = R.string.change_password_expired_warning
        !isOnline -> messageResId = R.string.disabled_tile_offline
        hasPrinter -> state = TileVariant.NEUTRAL
        else -> messageResId = R.string.disabled_tile_no_printer
      }

      return TileUiModel(
        id = MainTileId.ERRANDS,
        titleResId = R.string.tile_rounds,
        variant = state,
        iconResId = R.drawable.rectangle_list_solid_full,
        disabledMessageResId = messageResId,
      )
    }

    private fun createReceiptsTile(
      isLoggedIn: Boolean,
      hasPrinter: Boolean,
      receiptsCount: Int,
    ): TileUiModel<MainTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      when {
        !isLoggedIn -> messageResId = R.string.disabled_tile_logged_out
        !hasPrinter -> messageResId = R.string.disabled_tile_no_printer
        receiptsCount > 0 -> state = TileVariant.NEUTRAL
        else -> messageResId = R.string.disabled_tile_no_receipts
      }

      return TileUiModel(
        id = MainTileId.RECEIPTS,
        titleResId = R.string.tile_receipts,
        titleArg = receiptsCount,
        variant = state,
        iconResId = R.drawable.receipt_solid_full,
        disabledMessageResId = messageResId,
      )
    }

    fun onTileClick(tile: TileUiModel<MainTileId>) {
      if (tile.variant == TileVariant.DISABLED) {
        showAlert(tile.disabledMessageResId)
      } else {
        handleNavigation(tile.id)
      }
    }

    private fun handleNavigation(id: MainTileId) {
      viewModelScope.launch {
        when (id) {
          MainTileId.STORAGE -> {
            val event =
              when {
                storageSessionStore.isStorageSavedToApi.value -> MainScreenEvent.NavigateToStorageSummary
                storageSessionStore.storageListItems.value != null -> MainScreenEvent.NavigateToSelectItemsFromStore
                else -> MainScreenEvent.NavigateToSelectStore
              }
            eventChannel.send(event)
          }
          MainTileId.SELL -> {
            eventChannel.send(MainScreenEvent.NavigateToSelectPartner)
          }
          MainTileId.ERRANDS -> {
            eventChannel.send(MainScreenEvent.NavigateToErrands)
          }
          MainTileId.RECEIPTS -> {
            eventChannel.send(MainScreenEvent.NavigateToReceiptsList)
          }
        }
      }
    }

    private fun showAlert(
      @StringRes messageResId: Int?,
    ) {
      alertStateFlow.value =
        AlertUiState(
          titleResId = R.string.alert_title_feature_not_available,
          messageResId = messageResId,
          cancelButtonTextResId = R.string.alert_button_ok,
          onCancel = { dismissAlert() },
          onDismiss = { dismissAlert() },
        )
    }

    fun dismissAlert() {
      alertStateFlow.value = null
    }

    private suspend fun performGarbageCollection() {
      val threshold = System.currentTimeMillis() - GC_THRESHOLD_MS
      cacheMetadataDao.deleteOldEntries(threshold)
    }
  }
