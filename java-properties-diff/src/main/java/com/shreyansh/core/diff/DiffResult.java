package com.shreyansh.core.diff;

import java.util.List;

/**
 * The full comparison of two properties files, one entry per key, sorted by key.
 *
 * @param entries the per-key results
 */
public record DiffResult(List<DiffEntry> entries) {

  /**
   * Stores an unmodifiable copy of the entries.
   *
   * @param entries the per-key results
   */
  public DiffResult {
    entries = List.copyOf(entries);
  }

  /**
   * Returns the entries of one type, keeping the key order.
   *
   * @param type the type to keep
   * @return the matching entries
   */
  public List<DiffEntry> byType(DiffType type) {
    return entries.stream().filter(entry -> entry.type() == type).toList();
  }

  /**
   * Tells whether any key is changed or missing from one side.
   *
   * @return {@code true} if at least one entry is not {@link DiffType#UNCHANGED}
   */
  public boolean hasDifferences() {
    return entries.stream().anyMatch(entry -> entry.type() != DiffType.UNCHANGED);
  }
}
