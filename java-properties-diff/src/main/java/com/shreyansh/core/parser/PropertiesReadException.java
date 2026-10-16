package com.shreyansh.core.parser;

import java.nio.file.Path;

/**
 * Thrown when a properties file can't be read or loaded.
 * The original I/O error is kept as the cause.
 */
public class PropertiesReadException extends Exception {

  // Exception is Serializable; -Xlint:serial warns without an explicit id
  private static final long serialVersionUID = 1L;

  /**
   * Creates an exception for a file that failed to read or parse.
   *
   * @param path the file that was being read
   * @param cause the underlying error
   */
  public PropertiesReadException(Path path, Throwable cause) {
    super("Couldn't read properties file: " + path, cause);
  }
}
