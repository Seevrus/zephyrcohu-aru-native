package com.zephyr.boreal.data.local.converters

import java.util.Collections

/**
 * Bridges Room-instantiated [androidx.room.TypeConverter]s (no DI, no query context — same
 * constraint that led to [PersistenceJson]) with [com.zephyr.boreal.data.repository.BaseRepository]:
 * a converter that degrades a malformed decode to null/empty marks its table's group here so the
 * next [com.zephyr.boreal.data.repository.BaseRepository.networkBoundResource] call for that group
 * treats its cache as stale, even if `fetchedAt` is still fresh.
 *
 * Some groups are coarser than a single dynamic query key (e.g. `get_store_details` is shared by
 * every store id — a converter has no way to know which id it's decoding for). A mark consumed
 * while refetching the "wrong" id is safe: the id whose row actually failed to decode still has a
 * malformed row on disk, so its next real read re-decodes, fails, and re-marks the group again.
 * The coarse grouping costs an extra refetch for unrelated ids, never a permanently-missed repair.
 * Do not "optimize" a converter to skip the re-mark on a repeat failure — that would break this.
 */
object CacheDecodeGuard {
  private val degradedGroups: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

  fun markDegraded(group: String) {
    degradedGroups.add(group)
  }

  fun consumeDegraded(group: String): Boolean = degradedGroups.remove(group)
}
