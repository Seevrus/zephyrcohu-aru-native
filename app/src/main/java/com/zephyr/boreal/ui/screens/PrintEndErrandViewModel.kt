package com.zephyr.boreal.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyr.boreal.data.repository.UserRepository
import com.zephyr.boreal.di.DefaultDispatcher
import com.zephyr.boreal.domain.utils.DevPrintFileNameBuilder
import com.zephyr.boreal.domain.utils.PrintEndErrandTextBuilder
import com.zephyr.boreal.store.receipts.ReceiptsStore
import com.zephyr.boreal.store.store.StoreSessionStore
import com.zephyr.boreal.store.user.UserSessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PrintEndErrandUiState(
  val isLoading: Boolean = true,
  val isPrintEnabled: Boolean = false,
  val canLeaveWithoutPrint: Boolean = false,
  val isPrinted: Boolean = false,
  val textToPrint: String? = null,
  val suggestedFileName: String = "",
  val showExitConfirm: Boolean = false,
)

@HiltViewModel
class PrintEndErrandViewModel
  @Inject
  constructor(
    private val userRepository: UserRepository,
    private val receiptsStore: ReceiptsStore,
    private val storeSessionStore: StoreSessionStore,
    private val userSessionStore: UserSessionStore,
    @param:DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
  ) : ViewModel() {
    private val _uiState = MutableStateFlow(PrintEndErrandUiState())
    val uiState: StateFlow<PrintEndErrandUiState> = _uiState.asStateFlow()

    private var hasExited = false

    init {
      viewModelScope.launch {
        val user = userRepository.getCurrentUser().first().getOrNull()
        val round = user?.lastRound
        val receipts = receiptsStore.receipts.first()

        val isPrintEnabled = user != null && round != null
        val canLeaveWithoutPrint = receipts.isEmpty()

        val textToPrint =
          if (isPrintEnabled) {
            withContext(defaultDispatcher) {
              PrintEndErrandTextBuilder.build(receipts, requireNotNull(round), requireNotNull(user))
            }
          } else {
            null
          }
        val suggestedFileName = round?.let { DevPrintFileNameBuilder.buildForRound(it) } ?: ""

        _uiState.update {
          it.copy(
            isLoading = false,
            isPrintEnabled = isPrintEnabled,
            canLeaveWithoutPrint = canLeaveWithoutPrint,
            textToPrint = textToPrint,
            suggestedFileName = suggestedFileName,
          )
        }
      }
    }

    fun onPrintSuccess() {
      _uiState.update { it.copy(isPrinted = true) }
      viewModelScope.launch { userSessionStore.setPendingResetAfterPrint(true) }
    }

    fun onReturnClick(onExit: () -> Unit) {
      if (canExitWithoutConfirm()) {
        exit(onExit)
      } else {
        _uiState.update { it.copy(showExitConfirm = true) }
      }
    }

    fun onBackPress(onExit: () -> Unit) {
      if (canExitWithoutConfirm()) {
        exit(onExit)
      } else {
        _uiState.update { it.copy(showExitConfirm = true) }
      }
    }

    fun dismissExitConfirm() {
      _uiState.update { it.copy(showExitConfirm = false) }
    }

    fun confirmExit(onExit: () -> Unit) {
      _uiState.update { it.copy(showExitConfirm = false) }
      exit(onExit)
    }

    private fun canExitWithoutConfirm(): Boolean = _uiState.value.canLeaveWithoutPrint || _uiState.value.isPrinted

    private fun exit(onExit: () -> Unit) {
      if (hasExited) {
        return
      }
      hasExited = true

      viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        receiptsStore.resetReceipts()
        storeSessionStore.resetSelectedStoreCurrentState()
        userSessionStore.setPendingResetAfterPrint(false)
        userRepository.logout()
        onExit()
      }
    }
  }
