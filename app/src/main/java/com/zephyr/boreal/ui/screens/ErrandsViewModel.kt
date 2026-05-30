package com.zephyr.boreal.ui.screens

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.R
import com.zephyr.boreal.data.repository.RoundsRepository
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.domain.model.User
import com.zephyr.boreal.domain.model.UserState
import com.zephyr.boreal.network.ConnectivityObserver
import com.zephyr.boreal.store.print.PrintSettingsStore
import com.zephyr.boreal.store.user.UserSessionStore
import com.zephyr.boreal.ui.components.TileVariant
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ErrandsEvent {
  data object NavigateBack : ErrandsEvent

  data object NavigateToStartErrand : ErrandsEvent

  data object NavigateToEndErrand : ErrandsEvent
}

enum class ErrandTileId {
  START,
  END,
  LIST,
}

data class ErrandsUiState(
  val isReady: Boolean = false,
  val tiles: List<TileUiModel<ErrandTileId>> = emptyList(),
  val alertState: AlertUiState? = null,
)

@HiltViewModel
class ErrandsViewModel
  @Inject
  constructor(
    private val userSessionStore: UserSessionStore,
    private val userRepository: UserRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val printSettingsStore: PrintSettingsStore,
    private val roundsRepository: RoundsRepository,
  ) : ViewModel() {
    private val alertStateFlow = MutableStateFlow<AlertUiState?>(null)
    private val eventChannel = Channel<ErrandsEvent>()
    val events = eventChannel.receiveAsFlow()

    private data class ErrandsContext(
      val user: User?,
      val isOnline: Boolean,
      val hasPrinter: Boolean,
      val isPasswordExpired: Boolean,
      val isLoggedIn: Boolean,
      val roundsCount: Int,
    )

    private val errandsContextFlow =
      combine(
        userRepository.getCurrentUser(),
        connectivityObserver.isInternetReachable,
        printSettingsStore.printSettingsState,
        userSessionStore.userState,
        roundsRepository.getRounds(),
      ) { userRes, isOnline, printSettings, session, roundsRes ->
        ErrandsContext(
          user = userRes.getOrNull(),
          isOnline = isOnline,
          hasPrinter = printSettings.selectedPrinterMacAddress != null,
          isPasswordExpired = session.storedToken?.isPasswordExpired == true,
          isLoggedIn = session.storedToken?.token != null,
          roundsCount = roundsRes.getOrNull()?.size ?: 0,
        )
      }.distinctUntilChanged()

    private val ErrandsContext.isAccessDenied: Boolean
      get() = !isLoggedIn || isPasswordExpired || !isOnline || !hasPrinter

    val uiState: StateFlow<ErrandsUiState> =
      combine(
        errandsContextFlow,
        alertStateFlow,
      ) { ctx, alert ->
        // Business rule: navigate back if invalid state
        if (ctx.isAccessDenied) {
          viewModelScope.launch {
            eventChannel.send(ErrandsEvent.NavigateBack)
          }
        }

        ErrandsUiState(
          isReady = true,
          tiles = buildTiles(ctx),
          alertState = alert,
        )
      }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ErrandsUiState(),
      )

    private fun buildTiles(ctx: ErrandsContext): List<TileUiModel<ErrandTileId>> =
      listOf(
        createStartErrandTile(ctx.user),
        createEndErrandTile(ctx.user, ctx.isOnline),
        createListErrandsTile(ctx.roundsCount),
      )

    private fun createStartErrandTile(user: User?): TileUiModel<ErrandTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      when (user?.state) {
        UserState.IDLE -> state = TileVariant.OK
        UserState.LOADING -> messageResId = R.string.errands_start_disabled_loading
        UserState.ON_ROUND -> messageResId = R.string.errands_start_disabled_on_round
        null -> {}
      }

      return TileUiModel(
        id = ErrandTileId.START,
        titleResId = R.string.tile_errands_start,
        variant = state,
        iconResId = R.drawable.play_circle,
        disabledMessageResId = messageResId,
      )
    }

    private fun createEndErrandTile(
      user: User?,
      isOnline: Boolean,
    ): TileUiModel<ErrandTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      if (user?.state == UserState.ON_ROUND) {
        if (isOnline) {
          state = TileVariant.WARNING
        }
      } else {
        messageResId = R.string.errands_end_disabled_not_on_round
      }

      return TileUiModel(
        id = ErrandTileId.END,
        titleResId = R.string.tile_errands_end,
        variant = state,
        iconResId = R.drawable.stop_circle,
        disabledMessageResId = messageResId,
      )
    }

    private fun createListErrandsTile(roundsCount: Int): TileUiModel<ErrandTileId> {
      var state = TileVariant.DISABLED
      var messageResId: Int? = null

      if (roundsCount > 0) {
        state = TileVariant.NEUTRAL
      } else {
        messageResId = R.string.errands_list_disabled_empty
      }

      return TileUiModel(
        id = ErrandTileId.LIST,
        titleResId = R.string.tile_errands_list,
        variant = state,
        iconResId = R.drawable.assignment_turned_in,
        disabledMessageResId = messageResId,
      )
    }

    fun onTileClick(tile: TileUiModel<ErrandTileId>) {
      if (tile.variant == TileVariant.DISABLED) {
        showAlert(tile.disabledMessageResId)
      } else {
        viewModelScope.launch {
          when (tile.id) {
            ErrandTileId.START -> eventChannel.send(ErrandsEvent.NavigateToStartErrand)
            ErrandTileId.END -> eventChannel.send(ErrandsEvent.NavigateToEndErrand)
            ErrandTileId.LIST -> { /* TODO */ }
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
  }
