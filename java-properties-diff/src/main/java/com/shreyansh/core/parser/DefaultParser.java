package com.shreyansh.core.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/**
 * DefaultParser
 */
public class DefaultParser implements Parser {

  @Override
  public List<KeyValue> parseFile(Path path) throws PropertiesReadException {
    Properties prop = new Properties();
    try (
        BufferedReader fileBufferedReader = Files.newBufferedReader(path)) {
      prop.load(fileBufferedReader);
    } catch (IOException e) {
      throw new PropertiesReadException(path, e);
    }
    return prop.stringPropertyNames().stream()
        .map(propertyName -> new KeyValue(propertyName, prop.getProperty(propertyName)))
        .toList();
  }
}
