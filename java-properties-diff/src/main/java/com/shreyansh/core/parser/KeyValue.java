package com.shreyansh.core.parser;

/**
 * A single key/value pair produced by a {@link Parser}.
 *
 * @param key the property key, with escapes already decoded
 * @param value the property value, with escapes already decoded
 */
public record KeyValue(String key, String value) {
}
