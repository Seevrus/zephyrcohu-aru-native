package com.zephyr.boreal.update

import com.zephyr.boreal.network.AppUpdateChecker
import com.zephyr.boreal.store.update.AppUpdateStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class AppUpdateGateTest {
  private val appUpdateChecker: AppUpdateChecker = mock()
  private val appUpdateStore: AppUpdateStore = mock()
  private val now = Instant.parse("2026-08-03T12:00:00Z")
  private val lastCheckedAtFlow = MutableStateFlow<Instant?>(null)

  private fun createGate(isDebugBuild: Boolean = false) =
    object : AppUpdateGate(appUpdateChecker, appUpdateStore) {
      override fun isDebugBuild(): Boolean = isDebugBuild

      override fun now(): Instant = this@AppUpdateGateTest.now
    }

  @BeforeEach
  fun setUp() {
    whenever(appUpdateStore.lastCheckedAt).thenReturn(lastCheckedAtFlow)
  }

  @Test
  fun `never checks on a debug build`() =
    runTest {
      val gate = createGate(isDebugBuild = true)

      assertFalse(gate.checkForUpdate(isOnline = true))
      verify(appUpdateChecker, never()).isUpdateAvailable()
    }

  @Test
  fun `never checks while offline`() =
    runTest {
      val gate = createGate()

      assertFalse(gate.checkForUpdate(isOnline = false))
      verify(appUpdateChecker, never()).isUpdateAvailable()
    }

  @Test
  fun `checks and stamps last-checked when online and no prior check exists`() =
    runTest {
      whenever(appUpdateChecker.isUpdateAvailable()).thenReturn(false)
      val gate = createGate()

      assertFalse(gate.checkForUpdate(isOnline = true))
      verify(appUpdateChecker).isUpdateAvailable()
      verify(appUpdateStore).setLastCheckedAt(now)
    }

  @Test
  fun `skips the check when last checked less than 24h ago`() =
    runTest {
      lastCheckedAtFlow.value = now.minusSeconds(60 * 60 * 23)
      val gate = createGate()

      assertFalse(gate.checkForUpdate(isOnline = true))
      verify(appUpdateChecker, never()).isUpdateAvailable()
    }

  @Test
  fun `checks again when last checked at least 24h ago`() =
    runTest {
      lastCheckedAtFlow.value = now.minusSeconds(60 * 60 * 24)
      whenever(appUpdateChecker.isUpdateAvailable()).thenReturn(false)
      val gate = createGate()

      gate.checkForUpdate(isOnline = true)

      verify(appUpdateChecker).isUpdateAvailable()
    }

  @Test
  fun `returns true and stamps last-checked when an update is available`() =
    runTest {
      whenever(appUpdateChecker.isUpdateAvailable()).thenReturn(true)
      val gate = createGate()

      assertTrue(gate.checkForUpdate(isOnline = true))
      verify(appUpdateStore).setLastCheckedAt(now)
    }

  @Test
  fun `treats a checker failure as no update available but still stamps last-checked`() =
    runTest {
      whenever(appUpdateChecker.isUpdateAvailable()).thenThrow(RuntimeException("network error"))
      val gate = createGate()

      assertFalse(gate.checkForUpdate(isOnline = true))
      verify(appUpdateStore).setLastCheckedAt(now)
    }
}
