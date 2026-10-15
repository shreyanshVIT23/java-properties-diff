package com.shreyansh.core.parser;

import java.nio.file.Path;
import java.util.List;

/**
 * Parser
 */
public interface Parser {
  public List<KeyValue> parseFile(Path path) throws PropertiesReadException;
}
