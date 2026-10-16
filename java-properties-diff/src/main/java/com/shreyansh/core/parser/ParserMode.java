package com.shreyansh.core.parser;

/**
 * Selects which {@link Parser} implementation reads the properties files.
 */
public enum ParserMode {

  /** {@link java.util.Properties}-backed parser: sorted by key, duplicates collapsed. */
  DEFAULT,

  /** Hand-written parser: file order, duplicates kept. */
  CUSTOM;

  /**
   * Creates a new parser for this mode.
   *
   * @return a new parser instance
   */
  public Parser create() {
    return switch (this) {
      case DEFAULT -> new DefaultParser();
      case CUSTOM -> new CustomParser();
    };
  }
}
