package com.zephyr.boreal.data.local.converters

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

@Serializable
private data class ShapeA(
  val id: Int,
  val name: String,
)

@Serializable
private data class ShapeARenamedField(
  val id: Int,
  val label: String,
)

@Serializable
private data class ShapeAAddedField(
  val id: Int,
  val name: String,
  val extra: Boolean,
)

@Serializable
private data class ShapeARetypedField(
  val id: Int,
  val name: Int,
)

@Serializable
private data class ShapeWithNested(
  val id: Int,
  val nested: ShapeA,
)

@Serializable
private data class ShapeWithNestedChanged(
  val id: Int,
  val nested: ShapeARenamedField,
)

class CacheStampTest {
  @Test
  fun `the same shape produces the same fingerprint across separate computations`() {
    assertEquals(
      serializer<ShapeA>().descriptor.shapeFingerprint(),
      serializer<ShapeA>().descriptor.shapeFingerprint(),
    )
  }

  @Test
  fun `a renamed field changes the fingerprint`() {
    assertNotEquals(
      serializer<ShapeA>().descriptor.shapeFingerprint(),
      serializer<ShapeARenamedField>().descriptor.shapeFingerprint(),
    )
  }

  @Test
  fun `an added field changes the fingerprint`() {
    assertNotEquals(
      serializer<ShapeA>().descriptor.shapeFingerprint(),
      serializer<ShapeAAddedField>().descriptor.shapeFingerprint(),
    )
  }

  @Test
  fun `a retyped field changes the fingerprint`() {
    assertNotEquals(
      serializer<ShapeA>().descriptor.shapeFingerprint(),
      serializer<ShapeARetypedField>().descriptor.shapeFingerprint(),
    )
  }

  @Test
  fun `a shape change nested inside another type propagates to the outer fingerprint`() {
    assertNotEquals(
      serializer<ShapeWithNested>().descriptor.shapeFingerprint(),
      serializer<ShapeWithNestedChanged>().descriptor.shapeFingerprint(),
    )
  }

  @Test
  fun `CURRENT is a non-trivial fold over CACHE_SHAPE_TYPES, recomputed independently`() {
    val multiplier = HASH_MULTIPLIER_MIRROR
    val recomputed =
      CacheStamp.CACHE_SHAPE_TYPES.fold(0) { acc, descriptor ->
        multiplier * acc +
          descriptor.shapeFingerprint()
      }

    assertEquals(recomputed, CacheStamp.CURRENT)
    assertNotEquals(0, CacheStamp.CURRENT)
  }

  private companion object {
    // Mirrors CacheStamp.kt's private HASH_MULTIPLIER — kept in sync by this test's own assertion.
    const val HASH_MULTIPLIER_MIRROR = 31
  }
}
