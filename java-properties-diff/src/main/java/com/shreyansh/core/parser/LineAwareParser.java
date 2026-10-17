package com.shreyansh.core.parser;

import java.nio.file.Path;
import java.util.List;

/**
 * A {@link Parser} that can also report the line each entry comes from.
 */
public interface LineAwareParser extends Parser {

  /**
   * Parses the properties file and keeps each entry's starting line.
   *
   * @param path the file to read
   * @return the entries in file order, duplicates included
   * @throws PropertiesReadException if the file cannot be read or is malformed
   */
  List<NumberedKeyValue> parseFileWithLines(Path path) throws PropertiesReadException;
}
