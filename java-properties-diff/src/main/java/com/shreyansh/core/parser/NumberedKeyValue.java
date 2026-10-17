package com.shreyansh.core.parser;

/**
 * A key/value pair together with the line it was read from.
 *
 * @param pair the key/value pair
 * @param line the 1-based line where the entry starts; for a continued entry, its first line
 */
public record NumberedKeyValue(KeyValue pair, int line) {
}
