package com.shreyansh.core.diff;

/**
 * The comparison result for one key.
 *
 * @param key the property key
 * @param leftValue the value in the left file, or {@code null} if the key is missing there
 * @param rightValue the value in the right file, or {@code null} if the key is missing there
 * @param type how the two sides compare
 */
public record DiffEntry(String key, String leftValue, String rightValue, DiffType type) {
}
