package com.zephyr.boreal.architecture

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * `receipts` is the one durable record in this app (see
 * `rn-migration-tasks/03f-persistence-durability-classes.md`) — its package must not depend on
 * `api/dto`, unlike the cache entities/converters, which intentionally still do. This targets only
 * that boundary, not a blanket ban.
 */
class ReceiptRecordBoundaryTest {
  @Test
  fun `data local record does not import api dto`() {
    val recordPackageDir = File("src/main/java/com/zephyr/boreal/data/local/record")
    assertTrue(recordPackageDir.isDirectory, "Expected $recordPackageDir to exist")

    val kotlinFiles = recordPackageDir.walkTopDown().filter { it.extension == "kt" }.toList()
    assertTrue(kotlinFiles.isNotEmpty(), "Expected at least one .kt file in $recordPackageDir")

    val offenders =
      kotlinFiles.filter { file ->
        file.readLines().any { line ->
          val trimmed = line.trimStart()
          // Skip comment lines: ReceiptPersistedTypes.kt's KDoc legitimately mentions api/dto by
          // name to explain why its property names mirror it — only code references are offenses.
          !trimmed.startsWith("*") && !trimmed.startsWith("//") && trimmed.contains("com.zephyr.boreal.api.dto")
        }
      }

    assertTrue(
      offenders.isEmpty(),
      "Expected no api/dto references under data/local/record, found in: ${offenders.map { it.name }}",
    )
  }
}
