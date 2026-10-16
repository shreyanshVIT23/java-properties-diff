package com.shreyansh.core.parser;

import java.nio.file.Path;
import java.util.List;

/**
 * Reads a {@code .properties} file into key/value pairs.
 */
public interface Parser {

  /**
   * Parses the properties file at the given path.
   *
   * @param path the file to read
   * @return the key/value pairs found in the file
   * @throws PropertiesReadException if the file cannot be read or is malformed
   */
  List<KeyValue> parseFile(Path path) throws PropertiesReadException;
}
