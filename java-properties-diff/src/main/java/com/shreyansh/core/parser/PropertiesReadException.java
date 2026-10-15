package com.shreyansh.core.parser;

import java.nio.file.Path;

/**
 * Thrown when a properties file can't be read or loaded.
 * The original I/O error is kept as the cause.
 */
public class PropertiesReadException extends Exception {
  public PropertiesReadException(Path path, Throwable cause) {
    super("Couldn't read properties file: " + path, cause);
  }
}
