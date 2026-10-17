package com.shreyansh.cli;

/**
 * ANSI SGR styles for the delta-like output. Backgrounds use the 256-color palette, close to
 * delta's default dark theme.
 */
public enum Style {

  /** Background of a removed (left) row. */
  MINUS("48;5;52"),

  /** Stronger background for the changed part of a removed value. */
  MINUS_EMPH("48;5;88"),

  /** Background of an added (right) row. */
  PLUS("48;5;22"),

  /** Stronger background for the changed part of an added value. */
  PLUS_EMPH("48;5;28"),

  /** File and section headers. */
  HEADER("1;34"),

  /** Line numbers, separators and the summary. */
  DIM("2");

  private static final String ESC = "\u001b[";
  private static final String RESET = ESC + "0m";

  private final String sgr;

  Style(String sgr) {
    this.sgr = sgr;
  }

  /**
   * Wraps text in this style.
   *
   * @param text the text to style
   * @param enabled whether colors are on
   * @return the styled text, or {@code text} unchanged if colors are off or it is empty
   */
  public String paint(String text, boolean enabled) {
    if (!enabled || text.isEmpty()) {
      return text;
    }
    return ESC + sgr + "m" + text + RESET;
  }

  /**
   * Shows tab, newline, carriage return and form feed as escapes so a value fits on one row.
   *
   * @param value the decoded property value
   * @return the value with those control characters escaped
   */
  public static String visible(String value) {
    StringBuilder out = new StringBuilder(value.length());
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\t' -> out.append("\\t");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\f' -> out.append("\\f");
        default -> out.append(c);
      }
    }
    return out.toString();
  }
}
