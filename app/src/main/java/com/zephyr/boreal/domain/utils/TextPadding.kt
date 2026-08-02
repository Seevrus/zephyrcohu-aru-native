package com.zephyr.boreal.domain.utils

/**
 * Shared fixed-width column truncate/pad semantics for monospace printer text, ported from RN's
 * `printUtilities.ts`. Kept as the single implementation so [PrintReceiptTextBuilder] and
 * [PrintStorageTextBuilder] can't drift apart on column alignment, which is UX-critical for a
 * physical receipt printer.
 */
object TextPadding {
  private const val TRUNCATION_SUFFIX_LENGTH = 3

  fun padLeftSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String =
    if (target.length > length) {
      target.take(length - TRUNCATION_SUFFIX_LENGTH) + "..."
    } else {
      target.padStart(length, fillString.first())
    }

  fun padRightSide(
    target: String,
    length: Int,
    fillString: String = " ",
  ): String =
    if (target.length > length) {
      target.take(length - TRUNCATION_SUFFIX_LENGTH) + "..."
    } else {
      target.padEnd(length, fillString.first())
    }
}
