package com.shreyansh.core.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * Parser backed by {@link Properties#load(java.io.Reader)}.
 *
 * <p>{@code Properties} keeps no order and only the last value of a duplicated key, so the
 * result is sorted by key and duplicates are not visible.
 */
public class DefaultParser implements Parser {

  @Override
  public List<KeyValue> parseFile(Path path) throws PropertiesReadException {
    Properties prop = new Properties();
    try (BufferedReader fileBufferedReader = Files.newBufferedReader(path)) {
      prop.load(fileBufferedReader);
    } catch (IOException | IllegalArgumentException e) {
      // Properties.load throws IllegalArgumentException on a malformed unicode escape
      throw new PropertiesReadException(path, e);
    }
    return prop.stringPropertyNames().stream()
        .sorted()
        .map(propertyName -> new KeyValue(propertyName, prop.getProperty(propertyName)))
        .toList();
  }
}
