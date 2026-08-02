package com.zephyr.boreal.store.store

import com.zephyr.boreal.domain.model.StorageListItem
import com.zephyr.boreal.domain.model.StoreDetails
import kotlinx.serialization.Serializable

@Serializable
data class DraftStorageFlowState(
  val primaryStoreDetails: StoreDetails? = null,
  val selectedStoreInitialState: StoreDetails? = null,
  val selectedStoreCurrentState: StoreDetails? = null,
  val storageListItems: List<StorageListItem>? = null,
  val isStorageSavedToApi: Boolean = false,
)
