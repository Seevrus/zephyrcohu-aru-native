package com.zephyr.boreal.data.local.converters

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Guards against the exact failure mode the [CacheStamp] hashing redesign exists to remove: a new
 * cache-DTO decode added to a converter without a matching entry in [CacheStamp]'s
 * `CACHE_SHAPE_TYPES`, silently leaving that DTO's shape changes undetected. Derives the "expected"
 * type set straight from the converter source files (same file-scanning approach as
 * `ReceiptRecordBoundaryTest`) instead of a second hand-maintained list, so there is nothing here to
 * forget either.
 */
class CacheStampCoverageTest {
  @Test
  fun `every JSON-decoding cache converter return type is covered by CacheStamp`() {
    val convertersDir = File("src/main/java/com/zephyr/boreal/data/local/converters")
    assertTrue(convertersDir.isDirectory, "Expected $convertersDir to exist")

    val cacheConverterFiles =
      listOf(
        "ItemConverters.kt",
        "PartnerConverters.kt",
        "RoundConverters.kt",
        "StoreConverters.kt",
        "UserConverters.kt",
      ).map { File(convertersDir, it) }
    cacheConverterFiles.forEach { assertTrue(it.isFile, "Expected $it to exist") }

    val decodedTypeNames = cacheConverterFiles.flatMap { it.extractJsonDecodedTypeNames() }.toSet()
    assertTrue(decodedTypeNames.isNotEmpty(), "Expected to find at least one JSON-decoding converter")

    val coveredTypeNames = CacheStamp.CACHE_SHAPE_TYPES.map { it.serialName.substringAfterLast('.') }.toSet()

    val uncovered = decodedTypeNames - coveredTypeNames
    assertTrue(
      uncovered.isEmpty(),
      "Cache converter(s) decode $uncovered but CacheStamp.CACHE_SHAPE_TYPES doesn't list them — " +
        "a shape change there would go undetected. Add them to CacheStamp.",
    )
  }

  /**
   * Finds `fun to*(value: String...): ReturnType =` signatures whose body decodes JSON (looks
   * ahead for `decodeFromString` within the following lines, since the try/catch body isn't on the
   * signature line), and extracts the DTO/domain type name(s) from `ReturnType` — unwrapping
   * `List<X>`/`Map<K, V>` and a trailing `?` — skipping built-in and enum-by-name types.
   */
  private fun File.extractJsonDecodedTypeNames(): List<String> {
    val lines = readLines()
    val signatureRegex = Regex("""fun (to\w+)\(value: String\??\): (.+?) =\s*$""")
    val builtInOrEnumByName = setOf("Int", "Long", "Double", "Boolean", "String", "InvoiceType", "UserState")

    return lines.indices.mapNotNull { i ->
      val match = signatureRegex.find(lines[i]) ?: return@mapNotNull null
      val decodesJson = lines.subList(i, minOf(i + LOOKAHEAD_LINES, lines.size)).any { it.contains("decodeFromString") }
      if (!decodesJson) return@mapNotNull null

      val returnType = match.groupValues[2].trim().removeSuffix("?")
      val innerType =
        when {
          returnType.startsWith("List<") -> returnType.removePrefix("List<").removeSuffix(">")
          returnType.startsWith(
            "Map<",
          ) ->
            returnType
              .removePrefix("Map<")
              .removeSuffix(">")
              .substringAfterLast(',')
              .trim()
          else -> returnType
        }.trim().substringAfterLast('.')

      innerType.takeUnless { it in builtInOrEnumByName }
    }
  }

  companion object {
    private const val LOOKAHEAD_LINES = 15
  }
}
